package me;

import java.nio.channels.SelectionKey;
import java.util.Iterator;

public class StatusHandle implements Runnable {

    @Override
    public void run() {
        while (true) {
            // System.out.println("当前在线:"+Server.clients.size());
            if (!Server.clients.isEmpty()) {
                Iterator<SelectionKey> iterator = Server.clients.iterator();
                while (iterator.hasNext()) {
                    SelectionKey clientKey = iterator.next();
                    long millis = (long) clientKey.attachment();
                    if ((System.currentTimeMillis() - millis) > 3000) {
                        System.out.println("客户端超时断开连接");
                        ServerCore.removeConnection(clientKey);
                    }
                }
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
    }
}
