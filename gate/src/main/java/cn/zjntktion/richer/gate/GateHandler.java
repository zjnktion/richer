package cn.zjntktion.richer.gate;

import cn.zjnktion.richer.core.DaemonThreadFactory;
import cn.zjnktion.richer.core.protobuf.GateRequestMsg;
import cn.zjnktion.richer.core.protobuf.GateResponseMsg;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author zjnktion
 */
class GateHandler extends ChannelHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(GateHandler.class);

    private static final ExecutorService ES = Executors.newCachedThreadPool(new DaemonThreadFactory());

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        GateRequestMsg.GateRequest request = (GateRequestMsg.GateRequest) msg;
        ES.execute(new MsgProcessor(ctx, request));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("Gate handler caught an exception from channel[" + ctx.channel().id() + "]:", cause);
    }

    static class MsgProcessor implements Runnable {

        private static final Logger LOGGER = LoggerFactory.getLogger(MsgProcessor.class);

        private ChannelHandlerContext ctx;
        private GateRequestMsg.GateRequest request;

        MsgProcessor(ChannelHandlerContext ctx, GateRequestMsg.GateRequest request) {
            this.ctx = ctx;
            this.request = request;
        }

        public void run() {
            long s = System.currentTimeMillis();

            if (request == null || request.getVersion() == null || "".equals(request.getVersion())) {
                LOGGER.warn("Read an invalid msg from channel [{}]...", ctx.channel().id());
                ctx.channel().close();
                return;
            }

            // todo 响应逻辑业务需要进一步实现
            GateResponseMsg.GateResponse response = GateResponseMsg.GateResponse.newBuilder()
                    .setLatestVersion(request.getVersion())
                    .setNeedUpdate(false)
                    .setUpdateUrl("https://www.baidu.com")
                    .setServerIp("127.0.0.1")
                    .setServerPort(9090)
                    .build();
            ctx.writeAndFlush(response);

            LOGGER.info("Process msg from channel [{}] and response costs {} millis", ctx.channel().id(), (System.currentTimeMillis() - s));
        }
    }
}
