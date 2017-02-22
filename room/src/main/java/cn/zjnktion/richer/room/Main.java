package cn.zjnktion.richer.room;

import cn.zjnktion.richer.core.IService;

/**
 * @author zjnktion
 */
public class Main {

    public static void main(String[] args) {
        IService roomService = new RoomService();
        roomService.startup();
    }
}
