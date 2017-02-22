package cn.zjnktion.richer.core;

import java.util.concurrent.ThreadFactory;

/**
 * @author zjnktion
 */
public class DaemonThreadFactory implements ThreadFactory {
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    }
}
