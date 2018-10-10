package me;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
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
        // long startTime = System.currentTimeMillis();
        Socket socket = sc.socket();
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
                Server.finishConn.add(this.sc);
            } else {
                System.out.println(sc.getRemoteAddress() + " hashcode不一致关闭客户端连接,认证失败");
                close();
            }
        } catch (IOException e) {
            System.out.println("认证时IO异常关闭客户端连接");
            close();
        }
        // System.out.println("认证耗时：" + (System.currentTimeMillis() - startTime) +
        // "ms");
    }

    void close() {
        try {
            sc.close();
        } catch (IOException e) {

        }
    }
}
