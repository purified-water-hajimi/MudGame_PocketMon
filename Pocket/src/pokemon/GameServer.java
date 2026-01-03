package pokemon;

import java.io.*;
import java.net.*;
import java.util.*;

public class GameServer {
    private static final int PORT = 6666;

    // 存放所有在线玩家的列表
    private static List<ClientHandler> onlinePlayers = new Vector<>();

    public static void main(String[] args) {
        System.out.println("✅ 游戏服务器正在启动...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("✅ 服务器已启动！监听端口: " + PORT);
            System.out.println("等待玩家连接...");

            while (true) {
                // 1. 等待客户端连接 (会阻塞直到有连接)
                Socket socket = serverSocket.accept();
                System.out.println("有个新玩家连进来了！IP: " + socket.getInetAddress());

                // 2. 为每个连接创建一个处理线程
                ClientHandler handler = new ClientHandler(socket);
                onlinePlayers.add(handler);

                // 3. 启动线程，开始为这个玩家服务
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 广播消息给所有玩家
    public static void broadcast(String message, ClientHandler sender) {
        for (ClientHandler player : onlinePlayers) {
            player.sendMessage(message);
        }
    }
}