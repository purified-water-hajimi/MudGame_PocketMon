package pokemon;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class GameClient {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 6666;         // 端口

    public static void main(String[] args) {
        System.out.println("正在连接游戏服务器...");

        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT)) {
            BufferedReader serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "GBK"));
            PrintWriter serverWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "GBK"), true);

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

            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNextLine()) {
                String input = scanner.nextLine();
                serverWriter.println(input);

                if (input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("exit")) {
                    break;
                }
            }

        } catch (IOException e) {
            System.out.println("无法连接到服务器！请确认服务器已启动 (运行 run.bat)。");
        }
    }
}