package cn.zjnktion.richer.connector;

import io.netty.channel.ChannelHandlerContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zjnktion
 */
public class NativeCtxManager {

    private static final NativeCtxManager INSTANCE = new NativeCtxManager();

    private NativeCtxManager() {

    }

    public static NativeCtxManager getInstance() {
        return INSTANCE;
    }

    private ConcurrentHashMap<String, ChannelHandlerContext> ctxMap = new ConcurrentHashMap<String, ChannelHandlerContext>();

    public ChannelHandlerContext getCtx(String token) {
        return ctxMap.get(token);
    }

    public void addCtx(String token, ChannelHandlerContext ctx) {
        ctxMap.put(token, ctx);
    }

    public String removeCtx(ChannelHandlerContext ctx) {
        for (Map.Entry<String, ChannelHandlerContext> contextEntry : ctxMap.entrySet()) {
            if (contextEntry.getValue() == ctx) {
                ctxMap.get(contextEntry.getKey());
                return contextEntry.getKey();
            }
        }
        return null;
    }
}
