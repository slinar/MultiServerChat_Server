package me;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.SimpleDateFormat;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;

public class ServerCore {

    // 定义一个日期格式
    static final SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss",
            java.util.Locale.SIMPLIFIED_CHINESE);
    static final CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
    static final CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();

    public static String read(SelectionKey key) {
        SocketChannel sc = (SocketChannel) key.channel();
        ByteBuffer readBuffer = ByteBuffer.allocate(1024);
        readBuffer.clear();
        int count = 0;
        try {
            while (((count = sc.read(readBuffer)) > 0) || ((count = sc.read(readBuffer)) == -1)) {
                if (count == -1) {
                    System.out.println("ServerCore.read = -1 关闭连接");
                    removeConnection(key);
                    break;
                }
            }
            readBuffer.flip();
            if (readBuffer.limit() == 0)
                return null;
            String msg = decoder.decode(readBuffer).toString().trim();
            if (msg.length() == 0)
                return null;
            return msg;
        } catch (IOException e) {
            // System.out.println("ServerCore.read IO异常,返回空");
            // e.printStackTrace();
            removeConnection(key);
            return null;
        }

    }

    public static boolean write(SelectionKey clientKey, String msg) {
        CharBuffer charBuffer = CharBuffer.allocate(1024);
        ByteBuffer writeBuffer = ByteBuffer.allocate(1024);
        charBuffer.clear();
        charBuffer.put(msg);
        charBuffer.flip();
        try {
            writeBuffer = encoder.encode(charBuffer);
        } catch (CharacterCodingException e1) {
            System.out.println("ServerCore.write 字符串编码异常");
            return false;
        }
        try {
            while (writeBuffer.position() < writeBuffer.limit()) {
                ((SocketChannel) clientKey.channel()).write(writeBuffer);
            }
        } catch (IOException e) {
            System.out.println("ServerCore.write IO异常");
            removeConnection(clientKey);
            return false;
        }
        return true;
    }

    /**
     * 接受连接
     * 
     * @param key 表示服务端ServerSocketChannel在选择器中的选择键,因为只有ServerSocketChannel能ACCEPT
     */
    static void acceptConnection(SelectionKey serverKey) {
        try {
            // 接受连接,返回一个SocketChannel 取名叫 sc
            SocketChannel sc = ((ServerSocketChannel) serverKey.channel()).accept(); // 返回表示客户端的套接字
            System.out.println(sc.toString().replace("java.nio.channels.SocketChannel", "") + "准备接入!");

            // 设置SocketChannel(表示客户端)的模式为非阻塞模式
            sc.configureBlocking(false);

            // 把SocketChannel(表示客户端)注册到选择器中,关心的操作为读,并返回代表此客户端的SelectionKey(可以叫它选择键)
            SelectionKey clientKey = sc.register(Server.selector, SelectionKey.OP_READ);

            // 给客户端的SelectionKey附加一个当前时间毫秒,用来检测超时状态
            clientKey.attach(System.currentTimeMillis());

            // 把客户端的SelectionKey添加到检测超时的集合
            Server.clients.add(clientKey);
            System.out.println(ServerCore.getLink(clientKey) + "已连接");
        } catch (IOException | CancelledKeyException | NullPointerException e) {
            System.out.println("接受连接异常!");
        }
    }

    static void acceptConn(SelectionKey serverKey) {
        SocketChannel sc;
        try {
            sc = ((ServerSocketChannel) serverKey.channel()).accept();
            Server.es2.execute(new AuthHandle(sc));
            // Server.console.flush();
            System.out.println("\r\n" + sc.toString().replace("java.nio.channels.SocketChannel", "") + "准备接入!");
        } catch (IOException e) {
            System.out.println("接受连接异常!");
        }

    }

    /**
     * 从服务端移除客户端连接
     * 
     * @param clientKey 表示客户端SocketChannel在选择器中的选择键
     */
    static void removeConnection(SelectionKey clientKey) {

        // 如果超时列表(可以理解为当前已连接的客户端)中不包含这个客户端的SelectionKey 则直接返回
        if (!Server.clients.contains(clientKey))
            return;
        Server.clients.remove(clientKey);
        System.out.println(ServerCore.getLink(clientKey) + "超时,已断开连接!");
        try {
            clientKey.channel().close();
        } catch (IOException e) {
        }
        clientKey.attach(null);
        clientKey.cancel();
    }

    public static String getLink(SelectionKey key) {
        return ((SocketChannel) key.channel()).toString().replace("java.nio.channels.SocketChannel", "");
    }

    public static void config() {
        String address = null;
        int port = 60000;
        for (String s : Server._args) {
            if (s.contains("-address:")) {
                String addressTemp = s.split(":", 2)[1];
                if (addressTemp.length() > 0)
                    address = addressTemp;
            } else if (s.contains("-port:")) {
                String portTemp = s.split(":", 2)[1];
                if (portTemp.length() > 0)
                    port = Integer.valueOf(portTemp);
            } else if (s.contains("-password:")) {
                String pwd = s.split(":", 2)[1];
                if (pwd.length() > 0)
                    Server.pwd = pwd;
            }
        }
        if (address == null) {
            Server.addr = new InetSocketAddress(port);
        } else {
            Server.addr = new InetSocketAddress(address, port);
        }
    }

    public static String rand() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * @param string    需要hash的字符串
     * @param algorithm hash算法(MD5,SHA-1,SHA-256)
     * @return String 字符串hash后的值,如果传入的hash算法不被支持则返回null
     */
    public static String hash(String string, String algorithm) {
        final char[] HEX = "0123456789abcdef".toCharArray();
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("不能识别的Hash算法");
            return null;
        }
        byte[] temp = md.digest(string.getBytes());
        char[] chars = new char[temp.length * 2];
        for (int i = 0, offset = 0; i < temp.length; i++) {
            chars[offset++] = HEX[temp[i] >> 4 & 0xf];
            chars[offset++] = HEX[temp[i] & 0xf];
        }
        return new String(chars);
    }

    public static ByteBuffer Hash(String string, String algorithm) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("不能识别的Hash算法");
            return null;
        }
        byte[] array = md.digest(string.getBytes(Charset.forName("UTF-8")));
        ByteBuffer bb = ByteBuffer.wrap(array);
        return bb;
    }

    public static SecretKey aesKg() {
        KeyGenerator kg = null;
        try {
            kg = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            System.out.println("aesKey不能识别的加密算法");
        }

        return kg.generateKey();
    }

    /**
     * @param mode 密码器工作模式(DECRYPT_MODE,ENCRYPT_MODE)
     * @param skey 密钥
     * @param src  源字节缓冲区
     * @param dst  目标字节缓冲区
     */
    public static void encrypt(Key key, ByteBuffer src, ByteBuffer dst) {
        Cipher cipher = null;
        String a = null;
        if (key instanceof SecretKey) {
            a = "AES/CBC/NoPadding";
        } else if ((key instanceof RSAPublicKey) || (key instanceof RSAPrivateKey)) {
            a = "RSA/ECB/PKCS1Padding";
        }
        try {
            cipher = Cipher.getInstance(a);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
        try {
            cipher.init(Cipher.ENCRYPT_MODE, key);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        try {
            cipher.doFinal(src, dst);
        } catch (ShortBufferException e) {
            System.out.println("加密时输出缓冲区没有足够的空间");
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
    }

    public static void decrypt(Key key, ByteBuffer src, ByteBuffer dst) {
        Cipher cipher = null;
        String a = null;
        if (key instanceof SecretKey) {
            a = "AES/CBC/NoPadding";
        } else if (key instanceof RSAPublicKey) {
            a = "RSA/ECB/PKCS1Padding";
        } else if (key instanceof RSAPrivateKey) {
            a = "RSA/ECB/PKCS1Padding";
        }
        try {
            cipher = Cipher.getInstance(a);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
        try {
            cipher.init(Cipher.DECRYPT_MODE, key);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        try {
            cipher.doFinal(src, dst);
        } catch (ShortBufferException e) {
            System.out.println("解密时输出缓冲区没有足够的空间");
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
    }

    public static KeyPair rsaKpg() {
        KeyPairGenerator kpg = null;
        try {
            kpg = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        kpg.initialize(1024);
        return kpg.genKeyPair();
    }
}
