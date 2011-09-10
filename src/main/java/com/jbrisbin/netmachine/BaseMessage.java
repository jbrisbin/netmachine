package com.jbrisbin.netmachine;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousByteChannel;
import java.nio.channels.CompletionHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.Future;

import com.jbrisbin.netmachine.io.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public abstract class BaseMessage<M extends Message> implements Message<M> {

  private final Object outgoingMutex = new Object();

  protected final Logger log = LoggerFactory.getLogger(getClass());

  protected Map<String, String> headers = new HashMap<>();
  protected List<Handler<Void>> completionHandlers = new ArrayList<>();
  protected DelegatingReadHandler readHandler = new DelegatingReadHandler();
  protected List<BufferByteChannel> availableDataChannels = new ArrayList<>();
  protected WriteHandler<Object> writeHandler = null;
  protected Stack<ObjectToWrite> outgoing = new Stack<>();
  protected boolean completed = false;
  protected Message reply;
  protected Handler<Message> replyHandler = null;

  @SuppressWarnings({"unchecked"})
  @Override public M header(String name, String value, boolean replace) {
    if (replace) {
      headers.put(name, value);
    } else {
      String s = headers.get(name);
      if (null != s) {
        s += ", " + value;
      }
      headers.put(name, value);
    }
    return (M) this;
  }

  @Override public M header(String name, String value) {
    return header(name, value, true);
  }

  @Override public String header(String name) {
    return headers.get(name);
  }

  @Override public List<String> headers(String name) {
    String s = headers.get(name);
    if (null != s) {
      return Arrays.asList(s.split(","));
    }
    return Collections.emptyList();
  }

  @SuppressWarnings({"unchecked"})
  @Override public M headers(Map<String, String> headers) {
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      header(entry.getKey(), entry.getValue(), true);
    }
    return (M) this;
  }

  @Override public Map<String, String> headers() {
    return headers;
  }

  @Override public AsynchronousByteChannel io() {
    //This doesn't work right. Just an experiment...
    //BufferByteChannel ch = new BufferByteChannel();
    //availableDataChannels.add(ch);
    return null;
  }

  @SuppressWarnings({"unchecked"})
  @Override public M completionHandlers(Handler<Void>... completionHandlers) {
    for (Handler<Void> handler : completionHandlers) {
      if (this.completed) {
        handler.handle(null);
      } else {
        this.completionHandlers.add(handler);
      }
    }
    return (M) this;
  }

  @SuppressWarnings({"unchecked"})
  @Override public M completionHandler(Handler<Void> completionHandler) {
    if (this.completed) {
      completionHandler.handle(null);
    } else {
      this.completionHandlers.add(completionHandler);
    }
    return (M) this;
  }

  @Override public List<Handler<Void>> completionHandlers() {
    return completionHandlers;
  }

  @SuppressWarnings({"unchecked"})
  @Override public M readHandler(Handler<Buffer> handler) {
    readHandler.handler = handler;
    return (M) this;
  }

  @Override public Handler<Buffer> readHandler() {
    return readHandler.handler;
  }

  @SuppressWarnings({"unchecked"})
  @Override public M writeHandler(WriteHandler<Object> handler) {
    writeHandler = handler;
    synchronized (outgoingMutex) {
      for (ObjectToWrite o : outgoing) {
        writeHandler.write(o.obj, o.completionHandler);
      }
      outgoing.clear();
    }
    return (M) this;
  }

  @Override public WriteHandler<Object> writeHandler() {
    return writeHandler;
  }

  @SuppressWarnings({"unchecked"})
  @Override public <V> M write(V obj) {
    return write(obj, null);
  }

  @SuppressWarnings({"unchecked"})
  @Override public <V> M write(V obj, Handler<Void> completionHandler) {
    if (this.completed) {
      throw new IllegalStateException("Cannot write to this response as it's already been completed");
    }
    if (null != writeHandler) {
      writeHandler.write(obj, completionHandler);
    } else {
      synchronized (outgoingMutex) {
        outgoing.push(new ObjectToWrite(obj, completionHandler));
      }
    }
    return (M) this;
  }

  @SuppressWarnings({"unchecked"})
  @Override public M complete() {
    this.completed = true;
    for (Handler<Void> handler : completionHandlers) {
      handler.handle(null);
    }
    return (M) this;
  }

  @SuppressWarnings({"unchecked"})
  @Override public M replyHandler(Handler<Message> replyHandler) {
    synchronized (outgoingMutex) {
      this.replyHandler = replyHandler;
      if (null != replyHandler) {
        this.replyHandler.handle(reply);
      }
    }
    return (M) this;
  }

  @SuppressWarnings({"unchecked"})
  @Override public M reply(Message reply) {
    synchronized (outgoingMutex) {
      this.reply = reply;
      if (null != this.replyHandler) {
        this.replyHandler.handle(reply);
      }
    }
    return (M) this;
  }

  private class ObjectToWrite {
    Object obj;
    Handler<Void> completionHandler;

    private ObjectToWrite(Object obj, Handler<Void> completionHandler) {
      this.obj = obj;
      this.completionHandler = completionHandler;
    }
  }

  @Override public String toString() {
    return ", headers=" + headers +
        ", completionHandlers=" + completionHandlers +
        ", readHandler=" + readHandler +
        ", writeHandler=" + writeHandler +
        ", completed=" + completed +
        ", response=" + reply +
        ", replyHandler=" + replyHandler;
  }

  private class DelegatingReadHandler implements Handler<Buffer> {

    private Handler<Buffer> handler = null;

    @Override public void handle(Buffer buffer) {
      for (BufferByteChannel channel : availableDataChannels) {
        channel.available(buffer);
      }
      if (null != handler) {
        handler.handle(buffer);
      }
    }

  }

  private class BufferByteChannel implements AsynchronousByteChannel {

    private Buffer available;
    private ByteBuffer bufferToFill;
    private Object attachment;
    private CompletionHandler<Integer, Object> completionHandler;

    private void available(Buffer buffer) {
      available = buffer;
      if (null != bufferToFill) {
        available.flip().byteBuffer().put(bufferToFill);
      }
    }

    private void fill(ByteBuffer b) {
      if (null != available) {
        int pos = available.position();
        available.flip().byteBuffer().put(b);
        int end = available.position();
        completionHandler.completed(end - pos, attachment);
      }
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public <A> void read(ByteBuffer buffer, A attachment, CompletionHandler<Integer, ? super A> completionHandler) {
      this.bufferToFill = buffer;
      this.attachment = attachment;
      this.completionHandler = (CompletionHandler<Integer, Object>) completionHandler;
      if (null != available) {
        fill(buffer);
        this.completionHandler.completed(available.position(), attachment);
      }
    }

    @Override public Future<Integer> read(ByteBuffer buffer) {
      throw new RuntimeException("Blocking reads not implemented");
    }

    @Override
    public <A> void write(ByteBuffer buffer, final A attachment, final CompletionHandler<Integer, ? super A> completionHandler) {
      final int len = buffer.remaining();
      BaseMessage.this.write(new Buffer(buffer), (null == completionHandler ? null : new Handler<Void>() {
        @Override public void handle(Void v) {
          completionHandler.completed(len, attachment);
        }
      }));
    }

    @Override public Future<Integer> write(ByteBuffer buffer) {
      throw new RuntimeException("Blocking writes not implemented");
    }

    @Override public void close() throws IOException {
      available.clear();
    }

    @Override public boolean isOpen() {
      return true;
    }
  }


}
