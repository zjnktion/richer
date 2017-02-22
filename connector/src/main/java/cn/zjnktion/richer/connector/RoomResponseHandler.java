package cn.zjnktion.richer.connector;

import cn.zjnktion.richer.core.DaemonThreadFactory;
import cn.zjnktion.richer.core.SeqGenerator;
import cn.zjnktion.richer.core.protobuf.BusinessMsg;
import cn.zjnktion.richer.core.protobuf.RequestBodyMsg;
import cn.zjnktion.richer.core.protobuf.ResponseBodyMsg;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author zjnktion
 */
public class RoomResponseHandler extends ChannelHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoomResponseHandler.class);

    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1, new DaemonThreadFactory());

    private ExecutorService executorService = Executors.newCachedThreadPool(new DaemonThreadFactory());

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        scheduledExecutorService.scheduleAtFixedRate(new HeartBeatRequestProcessor(ctx), 30, 30, TimeUnit.SECONDS);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        BusinessMsg.Response response = (BusinessMsg.Response) msg;
        RoomRRCenter.RequestWrapper wrapper = RoomRRCenter.getInstance().response(response);
        if (wrapper == null) {
            return;
        }

        if (response.getResponseType() == BusinessMsg.ResponseType.RESPONSE_HEARTBEAT) {
            BusinessMsg.Request request = wrapper.getRequest();
            if (request == null) {
                LOGGER.warn("Response take a request but null, danger.");
                return;
            }
            RequestBodyMsg.HeartBeatRequest requestBody = request.getExtension(RequestBodyMsg.HeartBeatRequest.heartBeatRequest);
            ResponseBodyMsg.HeartBeatResponse responseBody = response.getExtension(ResponseBodyMsg.HeartBeatResponse.heartBeatResponse);
            if (requestBody.getSeq() != responseBody.getSeq()) {
                LOGGER.warn("RoomClient Receive a heartbeat response, but request body and response body had difference seq [request:{},response:{}], danger.", requestBody.getSeq(), responseBody.getSeq());
            }
        }

        // 转发回给客户端
        ChannelHandlerContext accessCtx = NativeCtxManager.getInstance().getCtx(wrapper.getToken());
        if (wrapper.getToken() == null || accessCtx == null) {
            LOGGER.info("Access Token:{} to witch ctx has been disconnect.", wrapper.getToken());
            return;
        }
        executorService.execute(new ResponseDispatchProcess(response, accessCtx));
    }

    private static class HeartBeatRequestProcessor implements Runnable {
        private ChannelHandlerContext ctx;
        private Random ran = new Random();

        HeartBeatRequestProcessor(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        public void run() {
            RequestBodyMsg.HeartBeatRequest body = RequestBodyMsg.HeartBeatRequest.newBuilder()
                    .setSeq(ran.nextInt())
                    .build();
            BusinessMsg.Request request = BusinessMsg.Request.newBuilder()
                    .setSeq(SeqGenerator.generate())
                    .setRequestType(BusinessMsg.RequestType.REQUEST_HEARTBEAT)
                    .setExtension(RequestBodyMsg.HeartBeatRequest.heartBeatRequest, body)
                    .build();

            RoomRRCenter.getInstance().addRequest(request, request.getAccessToken());
        }
    }

    private static class ResponseDispatchProcess implements Runnable {

        private BusinessMsg.Response response;
        private ChannelHandlerContext ctx;

        ResponseDispatchProcess(BusinessMsg.Response response, ChannelHandlerContext ctx) {
            this.response = response;
            this.ctx = ctx;
        }

        public void run() {
            if (response == null || ctx == null) {
                return;
            }

            ctx.writeAndFlush(response);
        }
    }
}
