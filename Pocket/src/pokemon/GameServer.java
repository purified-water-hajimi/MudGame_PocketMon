package pokemon;

import java.io.*;
import java.net.*;
import java.util.*;

public class GameServer {
    private static final int PORT = 6666;
    
    // 用来存放所有在线玩家的列表，方便广播消息
    private static List<ClientHandler> onlinePlayers = new Vector<>();

    public static void main(String[] args) {
        System.out.println("? 游戏服务器正在启动...");
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("? 服务器已启动！监听端口: " + PORT);
            System.out.println("等待玩家连接...");

            while (true) {
                // 1. 阻塞等待：这里会卡住，直到有人连进来
                Socket socket = serverSocket.accept(); 
                System.out.println("? 有个新玩家连进来了！IP: " + socket.getInetAddress());

                // 2. 为这个玩家单独雇佣一个“服务员” (线程)
                ClientHandler handler = new ClientHandler(socket);
                onlinePlayers.add(handler);
                
                // 3. 启动这个服务员线程，让他去专门伺候这个玩家
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // 广播方法：让所有人都能收到消息（比如“某某某进入了房间”）
    public static void broadcast(String message, ClientHandler sender) {
        for (ClientHandler player : onlinePlayers) {
            // 可以选择发给所有人，或者排除发送者自己
            player.sendMessage(message);
        }
    }
}