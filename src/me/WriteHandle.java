package me;

import java.nio.channels.SelectionKey;
import java.util.Iterator;

public class WriteHandle implements Runnable {

    @Override
    public void run() {
        while (true) {
            if (!Server.writeQueue.isEmpty()) {
                String msg = Server.writeQueue.poll();
                Iterator<SelectionKey> clientKeys = Server.clients.iterator();
                while (clientKeys.hasNext()) {
                    ServerCore.write(clientKeys.next(), msg);
                }
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
            }
        }
    }

}
