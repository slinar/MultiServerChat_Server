package me;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuthHandle implements Runnable {

    SocketChannel sc;
    ExecutorService es = Executors.newSingleThreadExecutor();
    InputStream in;
    OutputStream out;

    public AuthHandle(SocketChannel sc) {
        this.sc = sc;
    }

    @Override
    public void run() {
        Socket socket = this.sc.socket();
        try {
            socket.setSoTimeout(1500);
        } catch (SocketException e1) {
            e1.printStackTrace();
        }
        String uuid = ServerCore.rand();
        String hashcode = ServerCore.hash(Server.pwd + uuid, "SHA-1");
        try {
            in = socket.getInputStream();
            out = socket.getOutputStream();
            out.write(uuid.getBytes(Charset.forName("UTF-8")));
            byte[] b = new byte[40];
            in.read(b);
            String s = new String(b, Charset.forName("UTF-8"));
            if (s.equals(hashcode)) {
                out.write("OK".getBytes("UTF-8"));
                finishConn();
            } else {
                System.out.println(this.sc.getRemoteAddress() + " hashcode不一致关闭客户端连接,认证失败");
                close();
            }
        } catch (IOException e) {
            System.out.println("认证时IO异常关闭客户端连接");
            close();
        }
    }

    private void close() {
        try {
            this.sc.close();
        } catch (IOException e) {}
    }
    
    private void finishConn() {
        try {
            this.sc.configureBlocking(false);
        } catch (IOException e) {
            System.out.println("配置阻塞模式异常");
        }
        SelectionKey clientKey;
        try {
            clientKey = this.sc.register(Server.selector.wakeup(), SelectionKey.OP_READ);
            clientKey.attach(System.currentTimeMillis());
            Server.clients.add(clientKey);
            System.out.println(ServerCore.getLink(clientKey) + "已连接");
        } catch (ClosedChannelException e) {
            System.out.println("已关闭的通道异常");
        }
    }
}
