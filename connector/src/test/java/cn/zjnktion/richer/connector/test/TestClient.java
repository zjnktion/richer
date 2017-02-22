package cn.zjnktion.richer.connector.test;

import cn.zjnktion.richer.core.protobuf.BusinessMsg;
import cn.zjnktion.richer.core.protobuf.GateResponseMsg;
import cn.zjnktion.richer.core.protobuf.RequestBodyMsg;
import cn.zjnktion.richer.core.protobuf.ResponseBodyMsg;
import com.google.protobuf.ExtensionRegistry;
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
                    // 注册响应所需的extensions
                    ExtensionRegistry registry = ExtensionRegistry.newInstance();
                    BusinessMsg.registerAllExtensions(registry);
                    ResponseBodyMsg.registerAllExtensions(registry);

                    //decoded
                    ProtobufDecoder protobufDecoder = new ProtobufDecoder(BusinessMsg.Response.getDefaultInstance(), registry);
                    ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(1024, 0, 4, 0, 4));
                    ch.pipeline().addLast(protobufDecoder);
                    //encoded
                    ch.pipeline().addLast(new LengthFieldPrepender(4));
                    ch.pipeline().addLast(new ProtobufEncoder());
                    // 注册handler
                    ch.pipeline().addLast(new ChannelHandlerAdapter() {
                        @Override
                        public void channelActive(ChannelHandlerContext ctx) throws Exception {
                            RequestBodyMsg.LoginRequest requestBody = RequestBodyMsg.LoginRequest.newBuilder()
                                    .setAccount("test1")
                                    .setPwd("123456")
                                    .build();

                            BusinessMsg.Request request = BusinessMsg.Request.newBuilder()
                                    .setRequestType(BusinessMsg.RequestType.REQUEST_LOGIN)
                                    .setAccessToken("acc")
                                    .setExtension(RequestBodyMsg.LoginRequest.loginRequest, requestBody)
                                    .build();

                            ctx.writeAndFlush(request);
                        }

                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                            BusinessMsg.Response response = (BusinessMsg.Response) msg;

                            if (response.getResponseType() == BusinessMsg.ResponseType.RESPONSE_LOGIN) {
                                System.out.println("response code=" + response.getCode() + ";request access_token=" + response.getAccessToken());

                                ResponseBodyMsg.LoginResponse responseBody = response.getExtension(ResponseBodyMsg.LoginResponse.loginResponse);
                                System.out.println("body[login_success=" + responseBody.getLoginSuccess() + ";account=" + responseBody.getAccount() + ";response access_token=" + responseBody.getAccessToken() + ";msg=" + responseBody.getMsg() + "]");
                            }
                            if (response.getResponseType() == BusinessMsg.ResponseType.RESPONSE_DUPLICATE_LOGIN) {
                                System.out.println("response code=" + response.getCode() + ";request access_token=" + response.getAccessToken());

                                ResponseBodyMsg.DuplicateLoginResponse responseBody = response.getExtension(ResponseBodyMsg.DuplicateLoginResponse.duplicateLoginResponse);
                                System.out.println("body[account=" + responseBody.getAccount() + ";msg=" + responseBody.getMsg() + "]");
                            }
                        }
                    });
                }
            });
            ChannelFuture f=b.connect("127.0.0.1", 8081).sync();
            f.channel().closeFuture().sync();
        }finally{
            workerGroup.shutdownGracefully();
        }
    }
}
