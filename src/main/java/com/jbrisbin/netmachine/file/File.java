package com.jbrisbin.netmachine.file;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.jbrisbin.netmachine.Handler;
import com.jbrisbin.netmachine.ThreadPool;
import com.jbrisbin.netmachine.WriteHandler;
import com.jbrisbin.netmachine.io.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class File {

  private static final int BUFFER_SIZE = 16 * 1024;

  protected static final Logger log = LoggerFactory.getLogger(File.class);

  protected Path path;
  protected AsynchronousFileChannel fileChannel;
  protected Handler<Buffer> readHandler;
  protected long position = 0;
  protected boolean completed = false;
  protected AtomicInteger pendingWrites = new AtomicInteger(0);
  protected WriteHandler<Buffer> writeHandler = new WriteHandler<Buffer>() {
    @Override public void write(Buffer b, final Handler<Void> completionHandler) {
      pendingWrites.incrementAndGet();
      long len = b.remaining();
      fileChannel.write(b.byteBuffer(), position, null, new CompletionHandler<Integer, Void>() {
        @Override public void completed(Integer written, Void v) {
          if (pendingWrites.decrementAndGet() == 0 && completed) {
            try {
              fileChannel.close();
            } catch (IOException e) {
              log.error(e.getMessage(), e);
            }
            if (null != completionHandler) {
              completionHandler.handle(null);
            }
            if (null != File.this.completionHandler) {
              File.this.completionHandler.handle(null);
            }
          }
        }

        @Override public void failed(Throwable throwable, Void v) {
          pendingWrites.decrementAndGet();
        }
      });
      position += len;
    }
  };
  protected Handler<Throwable> failureHandler;
  protected Handler<Void> completionHandler;

  protected File(Path path, Set<OpenOption> options) throws IOException {
    this.path = path;
    fileChannel = AsynchronousFileChannel.open(path, options, ThreadPool.WORKER_POOL);
  }

  public static File read(Path path, Handler<Buffer> readHandler) throws IOException {
    return open(path, StandardOpenOption.READ)
        .readHandler(readHandler)
        .readFully();
  }

  public static File overwrite(Path path) throws IOException {
    return open(path,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
  }

  public static File open(Path path, OpenOption... options) throws IOException {
    return new File(path, openOptions(options));
  }

  public static void transferTo(Path path, WritableByteChannel channel) throws IOException {
    FileChannel f = new RandomAccessFile(path.toFile(), "r").getChannel();
    long size = f.size();
    f.transferTo(0, size, channel);
  }

  public WriteHandler<Buffer> writeHandler() {
    return writeHandler;
  }

  public Handler<Buffer> readHandler() {
    return readHandler;
  }

  public File readHandler(Handler<Buffer> readHandler) {
    this.readHandler = readHandler;
    return this;
  }

  public Handler<Throwable> failureHandler() {
    return failureHandler;
  }

  public File failureHandler(Handler<Throwable> failureHandler) {
    this.failureHandler = failureHandler;
    return this;
  }

  public Handler<Void> completionHandler() {
    return completionHandler;
  }

  public File completionHandler(Handler<Void> completionHandler) {
    if (completed) {
      completionHandler.handle(null);
    } else {
      this.completionHandler = completionHandler;
    }
    return this;
  }

  public long position() {
    return position;
  }

  public File position(long position) {
    this.position = position;
    return this;
  }

  public File reset() {
    position = 0;
    return this;
  }

  public Handler<Void> closeHandler() {
    return new Handler<Void>() {
      @Override public void handle(Void v) {
        try {
          close();
        } catch (IOException e) {
          log.error(e.getMessage(), e);
        }
      }
    };
  }

  public void close() throws IOException {
    close(false);
  }

  public void close(boolean force) throws IOException {
    if (force) {
      fileChannel.close();
    } else {
      this.completed = true;
    }
  }

  public long size() throws IOException {
    return fileChannel.size();
  }

  public File readFully() throws IOException {
    final long size = fileChannel.size();
    if (null != readHandler) {
      final ByteBuffer buff = ByteBuffer.allocateDirect(BUFFER_SIZE);
      CompletionHandler<Integer, Void> handler = new CompletionHandler<Integer, Void>() {
        long position = 0;

        @Override public void completed(Integer read, Void v) {
          position += read;
          if (position == size) {
            File.this.completed = true;
            if (null != completionHandler) {
              completionHandler.handle(null);
            }
          } else {
            buff.clear();
            readChunk(position, buff, this);
          }
        }

        @Override public void failed(Throwable throwable, Void v) {
          if (null != failureHandler) {
            failureHandler.handle(throwable);
          }
        }
      };
      readChunk(0, buff, handler);
    }
    return this;
  }

  protected File readChunk(long position, ByteBuffer buff, CompletionHandler<Integer, Void> handler) {
    fileChannel.read(buff, position, null, handler);
    return this;
  }

  private static Set<OpenOption> openOptions(OpenOption... options) {
    Set<OpenOption> opts = new HashSet<>();
    for (OpenOption o : options) {
      opts.add(o);
    }
    return opts;
  }

}
