package cn.zjntktion.richer.gate;

import cn.zjnktion.richer.core.protobuf.GateRequestMsg;
import cn.zjnktion.richer.core.IService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author zjnktion
 */
public class GateService implements IService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GateService.class);

    private static final int BOSS_EVENT_LOOPS = 1;
    private static final int WORKER_EVENT_LOOPS = 2 * Runtime.getRuntime().availableProcessors();

    private int bindPort = 9090;

    private ExecutorService es = Executors.newCachedThreadPool();

    public GateService() {

    }

    public GateService(int bindPort) {
        this.bindPort = bindPort;
    }

    public void startup() {
        // 启动所有后台线程
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                LOGGER.error("Thread[" + t.getName() + "] occur exception :", e);
            }
        });

        // 启动监听服务
        EventLoopGroup bossGroup = new NioEventLoopGroup(BOSS_EVENT_LOOPS);
        EventLoopGroup workerGroup = new NioEventLoopGroup(WORKER_EVENT_LOOPS);
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.option(ChannelOption.SO_BACKLOG, 1024)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            // decoder
                            socketChannel.pipeline().addLast(new LengthFieldBasedFrameDecoder(1024, 0, 4, 0, 4));
                            socketChannel.pipeline().addLast(new ProtobufDecoder(GateRequestMsg.GateRequest.getDefaultInstance()));

                            // encoder
                            socketChannel.pipeline().addLast(new LengthFieldPrepender(4));
                            socketChannel.pipeline().addLast(new ProtobufEncoder());

                            // business handler
                            socketChannel.pipeline().addLast(new GateHandler());
                        }
                    });

            // 绑定监听端口
            ChannelFuture bindFuture = bootstrap.bind(bindPort).sync();
            LOGGER.info("Gate start up and listen at port#{}...", bindPort);
            bindFuture.channel().closeFuture().sync();
        }
        catch (InterruptedException e) {
            LOGGER.error("Gate occur exception while starting up:", e);
            throw new IllegalStateException("Gate bind failed.");
        }
        finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    private class StatisticsThread implements Runnable {

        public void run() {

        }
    }
}
