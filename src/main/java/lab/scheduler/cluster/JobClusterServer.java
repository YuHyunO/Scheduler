package lab.scheduler.cluster;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JobClusterServer {
    private int port;
    private static JobClusterServer instance;
    private boolean running;

    private JobClusterServer() {}

    public static JobClusterServer getServerInstance(int port) {
        if (instance == null) {
            instance = new JobClusterServer();
        }
        instance.port = port;
        return instance;
    }

    public static JobClusterServer getServerInstance() {
        if (instance == null) {
            instance = getServerInstance(25000);
        }
        return instance;
    }

    public static boolean isRunning() {
        if (instance == null) {
            return false;
        }
        return instance.running;
    }

    public void run() throws Exception {
        if (running) {
            log.warn("TCPJobClusterServer is already running");
            return;
        }

        EventLoopGroup boss = new NioEventLoopGroup();
        EventLoopGroup worker = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast();
                        }
                    });

            ChannelFuture future = bootstrap.bind(port).sync();
            future.channel().closeFuture().sync();
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }

        running = true;
    }

}
