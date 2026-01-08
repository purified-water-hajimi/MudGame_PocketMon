package pokemon;

import java.io.*;
import java.net.*;
import java.util.*;

public class GameServer {
    private static final int PORT = 6666;

    private static List<ClientHandler> onlinePlayers = new Vector<>();

    public static void main(String[] args) {
        System.out.println("游戏服务器正在启动...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("服务器已启动！监听端口: " + PORT);

            MarketManager.loadMarket();
            System.out.println("市场数据加载完成！");

            System.out.println("等待玩家连接...");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("有个新玩家连进来了！IP: " + socket.getInetAddress());

                ClientHandler handler = new ClientHandler(socket);
                onlinePlayers.add(handler);

                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void broadcast(String message, ClientHandler sender) {
        for (ClientHandler player : onlinePlayers) {
            player.sendMessage(message);
        }
    }
}