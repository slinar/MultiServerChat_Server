package me;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class MainListener implements Runnable {

	@Override
	public void run() {
		while (true) {
			try {
				//如果selector中没有ACCEPT/read/write事件，则会阻塞
				if (Server.selector.select(100) > 0) {
					Set<SelectionKey> keys = Server.selector.selectedKeys();
					Iterator<SelectionKey> iterator = keys.iterator();
					while (iterator.hasNext()) {
						SelectionKey key = iterator.next();
						if (key.isAcceptable()) {
							//ServerCore.acceptConnection (key);
							ServerCore.acceptConn(key);
						} else if (key.isReadable() && Server.clients.contains(key) && (!Server.readQueue.contains(key))) {
							Server.readQueue.offer(key);
						}					
					}
					keys.clear();
				}
				Thread.sleep(1);	
			} catch (IOException | CancelledKeyException | InterruptedException e) {
				//System.out.println("选择器IO异常,已取消key异常!");
			}
			
			finishConn();
		}
	}
	
	void finishConn() {
		if(!Server.finishConn.isEmpty()) {
			Iterator<SocketChannel> iterator = Server.finishConn.iterator();
			while(iterator.hasNext()) {
				SocketChannel sc = iterator.next();
				try {
					sc.configureBlocking(false);
				} catch (IOException e) {
					System.out.println("配置阻塞模式异常");
				}
				SelectionKey clientKey;
				try {
					clientKey = sc.register(Server.selector, SelectionKey.OP_READ);
					clientKey.attach(System.currentTimeMillis());
					Server.clients.add(clientKey);
					System.out.println(ServerCore.getLink(clientKey) + "已连接");
				} catch (ClosedChannelException e) {
					System.out.println("已关闭的通道异常");
				}
				Server.finishConn.remove(sc);
			}
		}
	}
}
