package neil.netty.helloworld;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class Server {
    public static void main(String[] args) throws Exception {
        // 第一个线程组，用于接受Client端连接
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        // 第二个线程组 用于实际业务的处理操作
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        
        try {
            //辅助类，用于配置Server
            ServerBootstrap b = new ServerBootstrap();
            //将工作线程组加入进来
            b.group(bossGroup, workerGroup)
            //指定NioServerSocketChannel类型的通道
            .channel(NioServerSocketChannel.class)
            //配置日志输出
            .handler(new LoggingHandler(LogLevel.INFO))
            //绑定具体的事件处理器
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new ServerHandler());
                }
            }).option(ChannelOption.SO_BACKLOG, 128)
            .childOption(ChannelOption.SO_KEEPALIVE, true);
            //绑定端口，等待绑定成功
            ChannelFuture f = b.bind(12345).sync();
            //等待服务器退出
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
