package me;

import java.io.Console;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.SecretKey;

public class Server {

    // 创建一个线程池，异步任务要用到
    private static ExecutorService es = Executors.newFixedThreadPool(5);

    static ExecutorService es2 = Executors.newCachedThreadPool();

    // 创建一个选择器,用来从ServerSocketChannel通道中挑选出代接受的连接
    static Selector selector;

    // 定义一个超时集合,存放当前已连接SocketChannel对应的clientKey
    static Set<SelectionKey> clients = new CopyOnWriteArraySet<SelectionKey>();

    static Set<SocketChannel> finishConn = new CopyOnWriteArraySet<SocketChannel>();

    // 定义一个队列,存放已经准备好读的clientKey
    static Queue<SelectionKey> readQueue = new ConcurrentLinkedQueue<SelectionKey>();

    // 定义个队列,存放准备转发的消息
    static Queue<String> writeQueue = new ConcurrentLinkedQueue<String>();

    // 自定义心跳信息
    static final String heat = "_ACTIVE_";

    static String pwd;

    static String[] _args;

    static InetSocketAddress addr;

    static SecretKey aesKey;

    static RSAPublicKey publicKey;

    static RSAPrivateKey privateKey;

    static Console console = System.console();

    public static void main(String[] args) throws IOException, InterruptedException {
        _args = args;
        init();
    }

    public static void init() throws IOException, InterruptedException {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        ServerCore.config();
        if (pwd == null) {
            System.out.println("没有设置密码,开启服务端失败");
            return;
        }
        try {
            ssc.bind(addr);
        } catch (Exception e) {
            System.out.println("绑定失败,端口已被占用或者IP地址端口无效,请修改启动配置");
            return;
        }

        System.out.println("服务端已启动,监听端口:" + addr.toString());

        // 打开选择器
        selector = Selector.open();

        // ssc注册到选择器,关心的事件为接受连接
        ssc.register(selector, SelectionKey.OP_ACCEPT);

        aesKey = ServerCore.aesKg();
        KeyPair kp = ServerCore.rsaKpg();
        publicKey = (RSAPublicKey) kp.getPublic();
        privateKey = (RSAPrivateKey) kp.getPrivate();

        es.execute(new ReadHandle());
        es.execute(new WriteHandle());
        es.execute(new MainListener());
        es.execute(new StatusHandle());
        es.execute(new Cmd());
    }

}
