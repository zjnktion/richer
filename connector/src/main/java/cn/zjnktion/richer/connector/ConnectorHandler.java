package cn.zjnktion.richer.connector;

import cn.zjnktion.richer.connector.db.dao.UserDao;
import cn.zjnktion.richer.connector.db.dto.User;
import cn.zjnktion.richer.core.SeqGenerator;
import cn.zjnktion.richer.core.protobuf.BusinessMsg;
import cn.zjnktion.richer.core.protobuf.RequestBodyMsg;
import cn.zjnktion.richer.core.protobuf.ResponseBodyMsg;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author zjnktion
 */
public class ConnectorHandler extends ChannelHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorHandler.class);

    private static final ExecutorService ES = Executors.newCachedThreadPool();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        BusinessMsg.Request request = (BusinessMsg.Request) msg;

        ES.execute(new BusinessProcessor(ctx, request));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String token = NativeCtxManager.getInstance().removeCtx(ctx);
        if (token != null) {
            Redis.getStringTemplate().delete(token);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("Connector caught an exception :", cause);
        ctx.close();
    }

    static class BusinessProcessor implements Runnable {

        private static final Logger LOGGER = LoggerFactory.getLogger(BusinessProcessor.class);

        private ChannelHandlerContext ctx;
        private BusinessMsg.Request request;

        BusinessProcessor(ChannelHandlerContext ctx, BusinessMsg.Request request) {
            this.ctx = ctx;
            this.request = request;
        }

        public void run() {
            long s = System.currentTimeMillis();

            String account = null;
            if (!(request.getRequestType() == BusinessMsg.RequestType.REQUEST_LOGIN || request.getRequestType() == BusinessMsg.RequestType.REQUEST_REGISTER)) {
                // 验证accessToken，判断是否登录
                String accessToken = request.getAccessToken();
                account = (String) Redis.getStringTemplate().opsForValue().get(accessToken);
                if (account == null || "".equals(account)) {
                    LOGGER.info("invalid access token:{}, close channel without confirm.", accessToken);
                    ctx.close();
                    return;
                }
            }

            switch (request.getRequestType()) {
                case REQUEST_LOGIN: {
                    RequestBodyMsg.LoginRequest loginRequest = request.getExtension(RequestBodyMsg.LoginRequest.loginRequest);

                    String loginAccount = null;
                    if (loginRequest.getUseTokenLogin()) {
                        String loginToken = loginRequest.getLoginToken();
                        if (loginToken == null || "".equals(loginToken)) {
                            ResponseBodyMsg.LoginResponse responseBody = ResponseBodyMsg.LoginResponse.newBuilder()
                                    .setLoginSuccess(false)
                                    .setAccount("")
                                    .setMsg("请填写用户名和密码")
                                    .build();
                            BusinessMsg.Response response = BusinessMsg.Response.newBuilder()
                                    .setResponseType(BusinessMsg.ResponseType.RESPONSE_LOGIN)
                                    .setCode(200)
                                    .setAccessToken(request.getAccessToken())
                                    .setExtension(ResponseBodyMsg.LoginResponse.loginResponse, responseBody)
                                    .build();
                            ctx.writeAndFlush(response);
                            ctx.close();
                            break;
                        }
                        User user = UserDao.getInstance().findUserByLoginToken(loginToken);
                        if (user == null) {
                            ResponseBodyMsg.LoginResponse responseBody = ResponseBodyMsg.LoginResponse.newBuilder()
                                    .setLoginSuccess(false)
                                    .setAccount("")
                                    .setMsg("请填写用户名和密码")
                                    .build();
                            BusinessMsg.Response response = BusinessMsg.Response.newBuilder()
                                    .setResponseType(BusinessMsg.ResponseType.RESPONSE_LOGIN)
                                    .setCode(200)
                                    .setAccessToken(request.getAccessToken())
                                    .setExtension(ResponseBodyMsg.LoginResponse.loginResponse, responseBody)
                                    .build();
                            ctx.writeAndFlush(response);
                            ctx.close();
                            break;
                        }
                        loginAccount = user.getAccount();
                    }
                    else {
                        loginAccount = loginRequest.getAccount();
                        if (loginAccount == null || "".equals(loginAccount) || loginRequest.getPwd() == null || "".equals(loginRequest.getPwd())) {
                            ResponseBodyMsg.LoginResponse responseBody = ResponseBodyMsg.LoginResponse.newBuilder()
                                    .setLoginSuccess(false)
                                    .setAccount(loginAccount)
                                    .setMsg("请填写用户名和密码")
                                    .build();
                            BusinessMsg.Response response = BusinessMsg.Response.newBuilder()
                                    .setResponseType(BusinessMsg.ResponseType.RESPONSE_LOGIN)
                                    .setCode(200)
                                    .setAccessToken(request.getAccessToken())
                                    .setExtension(ResponseBodyMsg.LoginResponse.loginResponse, responseBody)
                                    .build();
                            ctx.writeAndFlush(response);
                            ctx.close();
                            break;
                        }
                    }

                    User user = UserDao.getInstance().findUserByAccount(loginAccount);
                    if (user == null || !user.getPwd().equals(loginRequest.getPwd())) {
                        ResponseBodyMsg.LoginResponse responseBody = ResponseBodyMsg.LoginResponse.newBuilder()
                                .setLoginSuccess(false)
                                .setAccount(loginAccount)
                                .setMsg("用户名或密码不正确")
                                .build();
                        BusinessMsg.Response response = BusinessMsg.Response.newBuilder()
                                .setResponseType(BusinessMsg.ResponseType.RESPONSE_LOGIN)
                                .setCode(200)
                                .setAccessToken(request.getAccessToken())
                                .setExtension(ResponseBodyMsg.LoginResponse.loginResponse, responseBody)
                                .build();
                        ctx.writeAndFlush(response);
                        break;
                    }

                    // 判断是否别的设备有登录
                    String otherToken = (String) Redis.getStringTemplate().opsForValue().get(loginAccount);
                    if (otherToken != null) {
                        // todo 通知别的设备退出登录
                        ChannelHandlerContext otherCtx = NativeCtxManager.getInstance().getCtx(otherToken);
                        if (otherCtx != null) {
                            ResponseBodyMsg.DuplicateLoginResponse responseBody = ResponseBodyMsg.DuplicateLoginResponse.newBuilder()
                                    .setAccount(loginAccount)
                                    .setMsg("该账号已经在别的设备上登录了，请检查是否本人操作")
                                    .build();
                            BusinessMsg.Response response = BusinessMsg.Response.newBuilder()
                                    .setResponseType(BusinessMsg.ResponseType.RESPONSE_DUPLICATE_LOGIN)
                                    .setCode(200)
                                    .setAccessToken(otherToken)
                                    .setExtension(ResponseBodyMsg.DuplicateLoginResponse.duplicateLoginResponse, responseBody)
                                    .build();
                            otherCtx.writeAndFlush(response);
                            otherCtx.close();
                        }
                    }
                    Redis.getStringTemplate().delete(loginAccount);

                    // 生成新access_token todo accessToken对应的值是否要记录房间信息等
                    String newToken = UUID.randomUUID().toString();
                    Redis.getStringTemplate().opsForValue().set(loginAccount, newToken);
                    Redis.getStringTemplate().opsForValue().set(newToken, loginAccount, 30, TimeUnit.MINUTES);
                    NativeCtxManager.getInstance().addCtx(newToken, ctx);
                    ResponseBodyMsg.LoginResponse responseBody = ResponseBodyMsg.LoginResponse.newBuilder()
                            .setLoginSuccess(true)
                            .setAccount(loginAccount)
                            .setAccessToken(newToken)
                            .setMsg("登陆成功")
                            .build();
                    BusinessMsg.Response response = BusinessMsg.Response.newBuilder()
                            .setResponseType(BusinessMsg.ResponseType.RESPONSE_LOGIN)
                            .setCode(200)
                            .setAccessToken(request.getAccessToken())
                            .setExtension(ResponseBodyMsg.LoginResponse.loginResponse, responseBody)
                            .build();
                    ctx.writeAndFlush(response);
                    break;
                }
                case REQUEST_REGISTER: {
                    RequestBodyMsg.RegisterRequest registerRequest = request.getExtension(RequestBodyMsg.RegisterRequest.registerRequest);
                    System.out.println("register:" + registerRequest.getAccount() + "==" + registerRequest.getPwd());
                    break;
                }
                case REQUEST_HEARTBEAT: {
                    RequestBodyMsg.HeartBeatRequest heartBeatRequest = request.getExtension(RequestBodyMsg.HeartBeatRequest.heartBeatRequest);
                    // 通过心跳延长登录时间 todo accessToken对应的值是否要记录房间信息等
                    String accessToken = request.getAccessToken();
                    Redis.getStringTemplate().opsForValue().set(accessToken, account, 30, TimeUnit.MINUTES);
                    break;
                }
                case REQUEST_LOGOUT: {
                    RequestBodyMsg.LogoutRequest logoutRequest = request.getExtension(RequestBodyMsg.LogoutRequest.logoutRequest);
                    String logoutAccount = logoutRequest.getAccount();
                    if (logoutAccount.equals(account)) {
                        ResponseBodyMsg.LogoutResponse responseBody = ResponseBodyMsg.LogoutResponse.newBuilder()
                                .setLogoutSuccess(true)
                                .setMsg("账号退出成功")
                                .build();
                        BusinessMsg.Response response = BusinessMsg.Response.newBuilder()
                                .setResponseType(BusinessMsg.ResponseType.RESPONSE_LOGOUT)
                                .setAccessToken(request.getAccessToken())
                                .setExtension(ResponseBodyMsg.LogoutResponse.logoutResponse, responseBody)
                                .build();
                        ctx.writeAndFlush(response);
                        ctx.close();
                    }
                    else  {
                        ResponseBodyMsg.LogoutResponse responseBody = ResponseBodyMsg.LogoutResponse.newBuilder()
                                .setLogoutSuccess(false)
                                .setMsg("账号不正确")
                                .build();
                        BusinessMsg.Response response = BusinessMsg.Response.newBuilder()
                                .setResponseType(BusinessMsg.ResponseType.RESPONSE_LOGOUT)
                                .setAccessToken(request.getAccessToken())
                                .setExtension(ResponseBodyMsg.LogoutResponse.logoutResponse, responseBody)
                                .build();
                        ctx.writeAndFlush(response);
                    }
                }
                default: {
                    // 其他情况，做消息转发
                    int seq = SeqGenerator.generate();
                    BusinessMsg.Request newRequest = request.toBuilder().setSeq(seq).build();
                    RoomRRCenter.getInstance().addRequest(newRequest, newRequest.getAccessToken());
                }
            }

            LOGGER.info("Process msg from channel [{}] and response costs {} millis", ctx.channel().id(), (System.currentTimeMillis() - s));
        }
    }
}
