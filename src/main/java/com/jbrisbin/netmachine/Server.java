package com.jbrisbin.netmachine;

import java.net.InetSocketAddress;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public abstract class Server<T extends Server> {

  protected String host = "127.0.0.1";
  protected int port = 8080;
  protected ServerBootstrap server = new ServerBootstrap(
      new NioServerSocketChannelFactory(ThreadPool.ACCEPTOR_POOL,
                                        ThreadPool.WORKER_POOL,
                                        ThreadPool.PROCESSORS)
  );
  protected Channel channel;

  @SuppressWarnings({"unchecked"})
  public T listen(String host, int port) {
    this.host = host;
    this.port = port;
    return (T) this;
  }

  @SuppressWarnings({"unchecked"})
  public T start() {
    server.setOption("child.tcpNoDelay", true);
    server.setOption("child.keepAlive", true);
    server.setOption("reuseAddress", true);

    server.setPipelineFactory(new ChannelPipelineFactory() {
      @Override public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = Channels.pipeline();
        configurePipeline(pipeline);
        return pipeline;
      }
    });
    channel = server.bind(new InetSocketAddress(host, port));
    return (T) this;
  }

  protected abstract void configurePipeline(ChannelPipeline pipeline);

}
