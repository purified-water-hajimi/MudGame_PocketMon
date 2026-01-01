package pokemon;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class GameClient {
    private static final String SERVER_IP = "192.168.3.35";
    private static final int SERVER_PORT = 6666;         // 端口要和服务器一致

    public static void main(String[] args) {
        System.out.println("正在连接游戏服务器...");

        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT)) {
            // =================================================================
            // 关键：这里要设置成 "GBK"，必须和服务器端的 ClientHandler 保持一致！
            // 只要两边都是 GBK，你的 IDEA 控制台绝对不会乱码！
            // =================================================================
            BufferedReader serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "GBK"));
            PrintWriter serverWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "GBK"), true);

            // 1. 开启一个线程，专门负责“听”服务器说话
            // 这样无论你有没有在打字，服务器的消息都能随时弹出来
            new Thread(() -> {
                try {
                    String msg;
                    while ((msg = serverReader.readLine()) != null) {
                        System.out.println(msg);
                    }
                } catch (IOException e) {
                    System.out.println("服务器连接已断开！");
                    System.exit(0);
                }
            }).start();

            // 2. 主线程专门负责“说”，读取你的键盘输入发给服务器
            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNextLine()) {
                String input = scanner.nextLine();
                serverWriter.println(input); // 发送给服务器

                if (input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("exit")) {
                    break;
                }
            }

        } catch (IOException e) {
            System.out.println("无法连接到服务器！请确认服务器已启动 (运行 run.bat)。");
        }
    }
}