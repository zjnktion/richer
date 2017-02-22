package cn.zjnktion.richer.core;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zjnktion
 */
public class SeqGenerator {

    private static final AtomicInteger GEN = new AtomicInteger(0);

    public static int generate() {
        return GEN.getAndIncrement();
    }
}
