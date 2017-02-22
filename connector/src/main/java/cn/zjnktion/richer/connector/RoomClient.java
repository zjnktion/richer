package cn.zjnktion.richer.connector;

import cn.zjnktion.richer.core.protobuf.BusinessMsg;
import cn.zjnktion.richer.core.protobuf.ResponseBodyMsg;
import com.google.protobuf.ExtensionRegistry;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zjnktion
 */
public class RoomClient implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoomClient.class);

    private static final int EVENT_LOOPS = 2 * Runtime.getRuntime().availableProcessors();

    private String hostName = "localhost";
    private int port = 4396;

    public RoomClient() {

    }

    public RoomClient(int port) {
        this.port = port;
    }

    public RoomClient(String hostName, int port) {
        this.hostName = hostName;
        this.port = port;
    }

    public void run() {
        EventLoopGroup loopGroup = new NioEventLoopGroup(EVENT_LOOPS);

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(loopGroup)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {

                        protected void initChannel(SocketChannel ch) throws Exception {
                            // 注册所需的extensions
                            ExtensionRegistry registry = ExtensionRegistry.newInstance();
                            BusinessMsg.registerAllExtensions(registry);
                            ResponseBodyMsg.registerAllExtensions(registry);

                            // decoder
                            ProtobufDecoder protobufDecoder = new ProtobufDecoder(BusinessMsg.Response.getDefaultInstance(), registry);
                            ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(1024, 0, 4, 0, 4));
                            ch.pipeline().addLast(protobufDecoder);

                            // encoder
                            ch.pipeline().addLast(new LengthFieldPrepender(4));
                            ch.pipeline().addLast(new ProtobufEncoder());

                            // business handler
                            ch.pipeline().addLast(new RoomResponseHandler());
                        }
                    });

            bootstrap.connect(hostName, port).sync().channel().closeFuture().sync();
        }
        catch (InterruptedException e) {
            LOGGER.error("Connect to room occur exception:", e);
        }
        finally {
            loopGroup.shutdownGracefully();
        }
    }
}
