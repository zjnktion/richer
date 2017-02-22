package cn.zjnktion.richer.connector;

import cn.zjnktion.richer.core.protobuf.BusinessMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zjnktion
 */
public class RoomRRCenter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoomRRCenter.class);

    private static final RoomRRCenter INSTANCE = new RoomRRCenter();

    private RoomRRCenter() {

    }

    public static RoomRRCenter getInstance() {
        return INSTANCE;
    }

    private ConcurrentHashMap<Integer, RequestWrapper> requestMap = new ConcurrentHashMap<Integer, RequestWrapper>();

    public void addRequest(BusinessMsg.Request request, String token) {
        if (request == null || token == null) {
            return;
        }

        Integer seq = request.getSeq();
        RequestWrapper wrapper = new RequestWrapper(request, token);
        requestMap.put(seq, wrapper);
    }

    public RequestWrapper response(BusinessMsg.Response response) {
        if (response == null) {
            return null;
        }

        RequestWrapper wrapper = requestMap.get(response.getSeq());
        return wrapper;
    }

    static class RequestWrapper {
        private BusinessMsg.Request request;
        private String token;

        RequestWrapper(BusinessMsg.Request request, String token) {
            this.request = request;
            this.token = token;
        }

        public BusinessMsg.Request getRequest() {
            return request;
        }

        public String getToken() {
            return token;
        }
    }

}
