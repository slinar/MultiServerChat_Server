package me;

import java.nio.channels.SelectionKey;

public class ReadHandle implements Runnable {

    private String msg = null;
    private SelectionKey clientKey;
    private final String[] COLOR = { "§4", "§c", "§6", "§e", "§2", "§a", "§b", "§3", "§1", "§9", "§d", "§5", "§f", "§7",
            "§8", "§0", "§l", "§n", "§o", "§k", "§m", "§r" };

    @Override
    public void run() {
        while (true) {
            if (!Server.readQueue.isEmpty()) {
                clientKey = Server.readQueue.poll();
                msg = ServerCore.read(clientKey);
                if (msg != null) {
                    filter();
                    if (msg.length() > 0)
                        forward();
                }
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
            }
        }
    }

    private void filter() {
        if (msg.contains(Server.heat)) {
            clientKey.attach(System.currentTimeMillis());
            msg = msg.replace(Server.heat, "");
        }
    }

    private void forward() {
        Server.writeQueue.offer(msg);
        System.out.println(filterColor(msg));
    }
    
    private String filterColor(String msg) {
        for (int i = 0; i < COLOR.length; i++) {
            msg = msg.replace(COLOR[i], "");
        }
        return msg;
    }
}
