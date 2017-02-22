package cn.zjnktion.richer.gate.test;

import cn.zjnktion.richer.core.protobuf.GateRequestMsg;
import cn.zjnktion.richer.core.protobuf.GateResponseMsg;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;

/**
 * @author zjnktion
 */
public class TestClient {

    public static void main(String[] args) throws InterruptedException {
        EventLoopGroup workerGroup=new NioEventLoopGroup();
        try{
            Bootstrap b=new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.TCP_NODELAY, true);
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    //decoded
                    ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(1024, 0, 4, 0, 4));
                    ch.pipeline().addLast(new ProtobufDecoder(GateResponseMsg.GateResponse.getDefaultInstance()));
                    //encoded
                    ch.pipeline().addLast(new LengthFieldPrepender(4));
                    ch.pipeline().addLast(new ProtobufEncoder());
                    // 注册handler
                    ch.pipeline().addLast(new ChannelHandlerAdapter() {
                        @Override
                        public void channelActive(ChannelHandlerContext ctx) throws Exception {
                            GateRequestMsg.GateRequest request = GateRequestMsg.GateRequest.newBuilder()
                                    .setVersion("123444")
                                    .build();

                            ctx.writeAndFlush(request);
                        }

                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                            GateResponseMsg.GateResponse response = (GateResponseMsg.GateResponse) msg;

                            System.out.println("version:" + response.getLatestVersion() + ";update:" + response.getNeedUpdate() +";url:" + response.getUpdateUrl() + ";ip:" + response.getServerIp() + ";port:" + response.getServerPort());
                        }
                    });
                }
            });
            ChannelFuture f=b.connect("127.0.0.1", 9090).sync();
            f.channel().closeFuture().sync();
        }finally{
            workerGroup.shutdownGracefully();
        }
    }
}
