package me;

import java.nio.channels.SelectionKey;

public class ReadHandle implements Runnable {
	
	private String msg = null;
	private SelectionKey clientKey;
	
	@Override
	public void run() {
		while (true) {
			if (!Server.readQueue.isEmpty()) {
				clientKey = Server.readQueue.poll();
				msg = ServerCore.read(clientKey);
				if (msg != null) {
					filter();
					if (msg.length() > 0) forward();
				}
			}
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {}
		}
	}
	
	private void filter () {
		if (msg.contains(Server.heat)) {
			clientKey.attach(System.currentTimeMillis());
			msg = msg.replace(Server.heat, "");
		}
	}
	
	private void forward () {
		//String temp = msg;
		System.out.println(msg);
		Server.writeQueue.offer(msg);
	}
}
