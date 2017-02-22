package cn.zjnktion.richer.connector;

import cn.zjnktion.richer.core.IService;

/**
 * @author zjnktion
 */
public class Main {

    public static void main(String[] args) {
        // 初始化服务器
        IService server = new ConnectorService();
        server.startup();
    }
}
