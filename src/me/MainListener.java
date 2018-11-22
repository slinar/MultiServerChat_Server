package me;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.util.Iterator;
import java.util.Set;

public class MainListener implements Runnable {

    @Override
    public void run() {
        while (true) {
            try {
                // 如果selector中没有ACCEPT/read/write事件，则会阻塞
                if (Server.selector.select() > 0) {
                    Set<SelectionKey> keys = Server.selector.selectedKeys();
                    Iterator<SelectionKey> iterator = keys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        if (key.isAcceptable()) {
                            // ServerCore.acceptConnection (key);
                            ServerCore.acceptConn(key);
                        } else if (key.isReadable() && Server.clients.contains(key)
                                && (!Server.readQueue.contains(key))) {
                            Server.readQueue.offer(key);
                        }
                    }
                    keys.clear();
                }
                Thread.sleep(10);
            } catch (IOException | CancelledKeyException | InterruptedException e) {
                // System.out.println("选择器IO异常,已取消key异常!");
            }
        }
    }
}
