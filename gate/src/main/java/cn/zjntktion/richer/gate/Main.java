package cn.zjntktion.richer.gate;

import cn.zjnktion.richer.core.IService;

/**
 * @author zjnktion
 */
public class Main {

    public static void main(String[] args) {
        IService gate = new GateService();
        gate.startup();
    }
}
