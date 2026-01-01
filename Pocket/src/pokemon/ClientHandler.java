package pokemon;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    
    private Player player;       // 当前连接的玩家对象
    private Room currentRoom;    // 玩家当前的位置

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            // 保持 GBK 编码，防止乱码
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "GBK"));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "GBK"), true);

            // --- 1. 登录与初始化 ---
            out.println("欢迎来到宝可梦 MUD 世界！");
            out.println("请输入你的名字：");
            
            String name = in.readLine();
            if (name == null || name.trim().isEmpty()) name = "无名训练师";
            
            player = new Player(name); 
            // 送一只初始宝可梦，防止一出门就被打死
            player.setStarterPokemon(new PocketMon("皮卡丘", PocketMon.Type.ELECTRIC, 5));

            // 从公共地图获取出生点
            currentRoom = WorldManager.getStartRoom(); 
            
            // 进房登记
            if (currentRoom != null) {
                currentRoom.addPlayer(this.player);
            }

            // 告诉玩家现在在哪
            printRoomInfo();

            // --- 2. 游戏主循环 (监听指令) ---
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                inputLine = inputLine.trim();
                System.out.println("玩家 [" + name + "] 执行: " + inputLine);
                
                // 处理所有指令
                processCommand(inputLine);
            }

        } catch (IOException e) {
            System.out.println("玩家 [" + (player != null ? player.getName() : "未知") + "] 断开连接");
        } finally {
            // --- 3. 下线清理 ---
            try {
                if (currentRoom != null && player != null) {
                    currentRoom.removePlayer(player);
                }
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    // --- 指令处理中心 (这里加入了新指令！) ---
    private void processCommand(String cmd) {
        String[] parts = cmd.split(" ");
        String action = parts[0].toLowerCase();

        switch (action) {
            case "go":
                if (parts.length > 1) {
                    handleMove(parts[1]); 
                } else {
                    out.println("你要去哪里？(例如: go north)");
                }
                break;
                
            case "look":
                printRoomInfo();
                break;
                
            // === ? 新增：查看状态 ===
            case "status":
                out.println(player.getStatus());
                break;
                
            // === ? 新增：查看背包 ===
            case "bag":
                out.println(player.getBagContent());
                break;

            // === ? 新增：治疗 (必须在宝可梦中心) ===
            case "heal":
                if (currentRoom.getId().equals("pokemon_center")) {
                     out.println(player.healTeam());
                } else {
                     out.println("只有在【宝可梦中心】才能治疗哦！");
                }
                break;

            // === ? 新增：打工 (必须在打工场所) ===
            case "work":
                 if (currentRoom.getId().equals("work_place")) {
                     out.println(player.work());
                 } else {
                     out.println("这里不能打工！请去【常青市】北边的打工场所。");
                 }
                 break;
            
            // === ? 新增：帮助菜单 ===
            case "help":
                out.println("=== 可用指令 ===");
                out.println("移动: go [north/south/east/west]");
                out.println("状态: status");
                out.println("背包: bag");
                out.println("观察: look");
                out.println("特殊: heal (治疗), work (打工)");
                out.println("离开: exit");
                break;
                
            case "exit":
                out.println("再见！");
                break;
            default:
                out.println("我不懂这个指令: " + cmd);
        }
    }

    // --- 核心逻辑：移动 (加入了随机遇敌！) ---
    private void handleMove(String direction) {
        String nextRoomId = currentRoom.getExit(direction);

        if (nextRoomId == null) {
            out.println("那个方向没有路！");
            return;
        }

        Room nextRoom = WorldManager.getRoom(nextRoomId);

        if (nextRoom != null) {
            // A. 离开旧房间
            currentRoom.removePlayer(this.player);
            
            // B. 改变位置
            currentRoom = nextRoom;
            
            // C. 进入新房间
            currentRoom.addPlayer(this.player);

            // D. 打印新环境
            printRoomInfo();
            
            // === ?? E. 随机遇敌逻辑 (新增!) ===
            checkRandomEncounter();
        }
    }
    
    // --- 战斗触发检查 ---
    private void checkRandomEncounter() {
        // 从房间获取野生宝可梦
        PocketMon wildPokemon = currentRoom.getRandomWildPokemon();
        
        // 如果这里有怪，并且随机数小于 0.4 (40%几率)
        if (wildPokemon != null && Math.random() < 0.4) {
            out.println("\n?? 草丛里有什么东西在动...");
            try { Thread.sleep(1000); } catch (InterruptedException e) {}

            // 创建一个新的宝可梦对象（否则打死一次就没了）
            // 这里简单处理，直接用 room 里的对象（正式版最好 clone 一个）
            
            // 启动战斗！把 socket 的输入输出流传给 BattleSystem
            BattleSystem battle = new BattleSystem(this.player, wildPokemon, this.out, this.in);
            battle.startBattle();
            
            // 战斗结束后，重新打印房间信息，以免玩家迷路
            if (this.player.getFirstPokemon() != null && !this.player.getFirstPokemon().isFainted()) {
                 printRoomInfo();
            }
        }
    }

    private void printRoomInfo() {
        if (currentRoom == null) return;
        
        out.println("================================");
        out.println(currentRoom.getFullDescription()); 
        out.println("可用出口: " + currentRoom.getAvailableExits());
        out.println(currentRoom.getPlayerNames(this.player)); 
        out.println("================================");
    }
    
    public void sendMessage(String msg) {
        out.println(msg);
    }
}