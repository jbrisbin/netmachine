package com.jbrisbin.netmachine.http;

import static com.jbrisbin.netmachine.http.HttpHeader.*;
import static org.jboss.netty.handler.codec.http.HttpHeaders.*;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import com.jbrisbin.netmachine.Handler;
import com.jbrisbin.netmachine.Message;
import com.jbrisbin.netmachine.Server;
import com.jbrisbin.netmachine.WriteHandler;
import com.jbrisbin.netmachine.http.routing.Route;
import com.jbrisbin.netmachine.http.routing.UriMatcher;
import com.jbrisbin.netmachine.http.util.HttpHeaderUtils;
import com.jbrisbin.netmachine.io.Buffer;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.DefaultFileRegion;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpChunk;
import org.jboss.netty.handler.codec.http.DefaultHttpChunkTrailer;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.GenericConversionService;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class HttpServer extends Server<HttpServer> {

  private static final String SERVER_VERSION = "netmachine/0.1";

  private final Logger log = LoggerFactory.getLogger(getClass());

  private List<Route<Handler<HttpRequest>>> routes = new ArrayList<>();
  private ConfigurableConversionService conversionService = new GenericConversionService();

  public HttpServer() {
    addConverters();
  }

  public HttpServer(ConfigurableConversionService conversionService) {
    this.conversionService = conversionService;
    addConverters();
  }

  public ConfigurableConversionService conversionService() {
    return conversionService;
  }

  public HttpServer conversionService(ConfigurableConversionService conversionService) {
    this.conversionService = conversionService;
    addConverters();
    return this;
  }

  public List<Route<Handler<HttpRequest>>> routes() {
    return routes;
  }

  public HttpServer route(String uriPattern, Handler<HttpRequest> handler) {
    routes.add(new Route<Handler<HttpRequest>>(new UriMatcher(uriPattern), handler));
    return this;
  }

  private void addConverters() {
    if (!conversionService.canConvert(String.class, HttpChunk.class)) {
      conversionService.addConverter(StringToHttpChunkConverter.INSTANCE);
    }
  }

  @Override protected void configurePipeline(ChannelPipeline pipeline) {
    pipeline.addLast("decoder", new HttpRequestDecoder());
    pipeline.addLast("encoder", new HttpResponseEncoder());
    pipeline.addLast("chunker", new ChunkedWriteHandler());
    pipeline.addLast("handler", new HttpServerHandler());
  }

  private URI createURI(org.jboss.netty.handler.codec.http.HttpRequest request) {
    String uri = request.getProtocolVersion()
                        .getProtocolName()
                        .toLowerCase()
        + "://"
        + request.getHeader("Host")
        + request.getUri();
    try {
      return new URI(uri);
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }

  private ChannelFuture writeResponse(HttpResponse response, Channel channel) {
    response.header(SERVER, SERVER_VERSION);

    // Make sure Date and Host headers exist in the reply
    if (null == response.header(DATE)) {
      response.header(DATE, HttpHeaderUtils.formatDate(Calendar.getInstance().getTime()));
    }
    if (null == response.header(HOST)) {
      InetSocketAddress addr = ((InetSocketAddress) channel.getLocalAddress());
      String host = addr.getHostName() + (80 != addr.getPort() ? ":" + addr.getPort() : "");
      response.header(HOST, host);
    }

    org.jboss.netty.handler.codec.http.HttpResponse nettyResponse =
        new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(response.statusCode()));
    for (Map.Entry<String, String> header : response.headers().entrySet()) {
      nettyResponse.setHeader(header.getKey(), header.getValue());
    }

    return channel.write(nettyResponse);
  }

  private class HttpServerHandler extends SimpleChannelUpstreamHandler {
    @Override public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
      //log.debug("msg: " + e);
      final Channel channel = e.getChannel();
      final Object msg = e.getMessage();

      HttpRequest request = (HttpRequest) ctx.getAttachment();
      if (null == request || msg instanceof org.jboss.netty.handler.codec.http.HttpRequest) {
        request = new HttpRequest();
        org.jboss.netty.handler.codec.http.HttpRequest nettyRequest =
            (org.jboss.netty.handler.codec.http.HttpRequest) e.getMessage();

        // Handle Expect: 100-continue
        if (is100ContinueExpected(nettyRequest)) {
          channel.write(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
        }

        Method method = Method.valueOf(nettyRequest.getMethod().toString());
        request.method(method);

        URI uri = createURI(nettyRequest);
        request.uri(uri);

        for (String s : nettyRequest.getHeaderNames()) {
          request.header(s, nettyRequest.getHeader(s));
        }
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(nettyRequest.getUri());
        request.params(queryStringDecoder.getParameters());

        final boolean keepAlive;
        if (null != request.header(CONNECTION)) {
          keepAlive = HttpHeader.matches(request.header(CONNECTION), "Keep-Alive");
        } else {
          keepAlive = false;
        }

        request.replyHandler(new Handler<Message>() {
          @Override public void handle(Message msg) {
            if (msg instanceof HttpResponse) {
              final HttpResponse response = (HttpResponse) msg;

              if (keepAlive) {
                response.header(CONNECTION, "Keep-Alive");
              }

              final boolean chunked;
              if (response.contentLength() < 0) {
                response.header(TRANSFER_ENCODING, "chunked");
                chunked = true;
              } else {
                chunked = false;
              }

              writeResponse(response, channel);

              if (chunked || response.contentLength() > 0) {
                response.writeHandler(new WriteHandler<Object>() {
                  @Override public void write(Object obj, final Handler<Void> completionHandler) {
                    if (obj instanceof Path) {
                      Path path = (Path) obj;
                      try {
                        RandomAccessFile f = new RandomAccessFile(path.toFile(), "r");
                        long len = f.length();
                        channel.write(new DefaultFileRegion(f.getChannel(), 0, len));
                      } catch (IOException ioe) {
                        log.error(ioe.getMessage(), ioe);
                      }
                    } else {
                      HttpChunk chunk;
                      if (obj instanceof HttpChunk) {
                        chunk = (HttpChunk) obj;
                      } else if (obj instanceof Buffer) {
                        chunk = new DefaultHttpChunk(ChannelBuffers.wrappedBuffer(((Buffer) obj).byteBuffer()));
                      } else {
                        chunk = conversionService.convert(obj, HttpChunk.class);
                      }

                      ChannelFuture f = channel.write(chunk);
                      if (null != completionHandler) {
                        f.addListener(new ChannelFutureListener() {
                          @Override public void operationComplete(ChannelFuture future) throws Exception {
                            completionHandler.handle(null);
                          }
                        });
                      }
                    }
                  }
                });
              }

              response.completionHandler(new Handler<Void>() {
                @Override public void handle(Void v) {
                  if (!keepAlive) {
                    channel.write(new DefaultHttpChunkTrailer()).addListener(ChannelFutureListener.CLOSE);
                  }
                }
              });

            }
          }
        });

        ctx.setAttachment(request);
        for (Route<Handler<HttpRequest>> r : routes) {
          if (r.matches(uri.getPath())) {
            r.resource().handle(request);
            return;
          }
        }

        // No routes matched
        HttpResponse notFound = new HttpResponse()
            .status(404, "Resource Not Found")
            .header(CONTENT_LENGTH, "0");
        writeResponse(notFound, channel);

      } else if (msg instanceof HttpChunk) {
        // Already handled the first bit, we must be processing chunks only now
        HttpChunk chunk = (HttpChunk) e.getMessage();
        ChannelBuffer contentBuffer = chunk.getContent();
        int len = contentBuffer.readableBytes();
        ByteBuffer b = ByteBuffer.allocateDirect(len);
        contentBuffer.readBytes(b);
        b.flip();
        Handler<Buffer> handler = request.readHandler();
        if (null != handler) {
          handler.handle(new Buffer(b));
        }

        if (chunk.isLast()) {
          request.complete();
        }
      } else {
        log.warn("Unknown message: " + msg);
      }
    }

    @Override public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
      switch ("" + e.getCause().getMessage()) {
        case "Connection reset by peer":
        case "Broken pipe":
          // Client shut down early
          break;
        default:
          log.debug(e.getCause().getMessage(), e.getCause());
      }
    }
  }

  private enum StringToHttpChunkConverter implements Converter<String, HttpChunk> {
    INSTANCE;

    @Override public HttpChunk convert(String source) {
      if (null != source) {
        return new DefaultHttpChunk(ChannelBuffers.wrappedBuffer(source.getBytes()));
      } else {
        return new DefaultHttpChunk(ChannelBuffers.EMPTY_BUFFER);
      }
    }
  }

}
