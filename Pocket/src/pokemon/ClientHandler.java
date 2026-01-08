package pokemon;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler implements Runnable {
    public static final Map<String, ClientHandler> onlinePlayers = new ConcurrentHashMap<>();

    private final Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private Player player;
    private Room currentRoom;
    private boolean gameRunning = true;
    private PvPBattle activeBattle;

    // 引入两个经理
    private MarketManager marketManager;
    private DuelManager duelManager;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.marketManager = new MarketManager(this);
        this.duelManager = new DuelManager(this);
    }

    public Player getPlayer() { return player; }
    public Room getCurrentRoom() { return currentRoom; }
    public void sendMessage(String msg) { out.println(msg); }
    public PvPBattle getActiveBattle() { return activeBattle; }
    public void setActiveBattle(PvPBattle b) { this.activeBattle = b; }
    public DuelManager getDuelManager() { return duelManager; }
    public void printPrompt() { out.print("> "); out.flush(); }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            out.println("欢迎来到《宝可梦多人联机 MUD》！输入你的名字：");
            String name = in.readLine().trim();

            if (Player.loadPlayer(name) != null) {
                player = Player.loadPlayer(name);
                out.println("欢迎回来 " + player.getName());
            } else {
                initializePlayer(name);
            }
            player.setOut(out);

            currentRoom = WorldManager.getRoom(player.getCurrentMap());
            if (currentRoom == null) currentRoom = WorldManager.getStartRoom();
            onlinePlayers.put(player.getName(), this);
            currentRoom.addPlayer(player);

            showHelp();
            out.println(WorldManager.getAsciiMap(currentRoom.getId()));

            while (gameRunning) {
                if (activeBattle == null) printPrompt();

                String input = in.readLine();
                if (input == null) break;
                input = input.trim();
                if (input.isEmpty()) continue;

                // 自动取消挑战逻辑（委托给 DuelManager）
                if (activeBattle == null && duelManager.isInitiator()) {
                    if (!input.equals("cancel")) {
                        duelManager.cancelDuel(false);
                        out.println("(进行了其他操作，挑战自动取消)");
                    }
                }

                if (activeBattle != null) {
                    activeBattle.handleInput(this, input);
                } else {
                    processCommand(input);
                }
            }

        } catch (Exception e) {
            System.out.println("玩家断开连接");
        } finally {
            cleanup();
        }
    }

    private void processCommand(String input) {
        String[] parts = input.split(" ");
        String cmd = parts[0];

        switch (cmd) {
            case "pk": case "duel":
                if (parts.length > 1) duelManager.handleRequest(parts[1]);
                else out.println("用法: pk [名字]");
                break;
            case "yes": duelManager.handleResponse(true); break;
            case "no": duelManager.handleResponse(false); break;
            case "cancel": duelManager.cancelDuel(true); break;

            case "market":
                if (parts.length > 1) marketManager.handleCommand(parts[1], parts);
                else out.println("用法: market list/sell/buy/stall");
                break;
            case "shop": marketManager.showShop(); break;
            case "buy":
                if (parts.length > 1) marketManager.buyShopItem(parts[1], parts.length > 2 ? Integer.parseInt(parts[2]) : 1);
                else out.println("用法: buy [物品] [数量]");
                break;

            case "n": handleMove("north"); break;
            case "s": handleMove("south"); break;
            case "e": handleMove("east"); break;
            case "w": handleMove("west"); break;
            case "go": if(parts.length > 1) handleMove(parts[1]); break;

            case "look": printRoomInfo(); break;
            case "bag": out.println(player.getBagContent()); break;
            case "status": out.println(player.getStatus()); break;
            case "map": out.println(WorldManager.getAsciiMap(currentRoom.getId())); break;
            case "who":
                out.println("=== 在线玩家 ===");
                for(String n : onlinePlayers.keySet()) out.println("- " + n);
                break;

            case "save": Player.savePlayer(player); out.println("存档成功"); break;
            case "quit": gameRunning = false; break;
            case "help": showHelp(); break;
            default: out.println("未知指令"); break;
        }
    }

    private void handleMove(String dir) {
        String nextId = currentRoom.getExit(dir);
        if (nextId == null) { out.println("没路了"); return; }

        currentRoom.removePlayer(player);
        currentRoom = WorldManager.getRoom(nextId);
        currentRoom.addPlayer(player);
        player.setCurrentMap(nextId);

        out.println("进入了 " + currentRoom.getName());
        printRoomInfo();
    }

    private void printRoomInfo() {
        out.println(currentRoom.getFullDescription());
        out.println("玩家: " + currentRoom.getPlayerNames(player));
    }

    public void endPvP() {
        this.activeBattle = null;

        if (this.duelManager != null) {
            this.duelManager.clearDuelTarget();
        }

        sendMessage("PvP 结束，回归自由行动模式。");
        printRoomInfo();
        Player.savePlayer(player);
    }

    private void cleanup() {
        if (player != null) {
            if (activeBattle != null) activeBattle.handleDisconnect(this);
            onlinePlayers.remove(player.getName());
            if (currentRoom != null) currentRoom.removePlayer(player);
            Player.savePlayer(player);
        }
    }

    // 初始化玩家、Help等辅助方法保持原样，省略以节省篇幅，请直接把原来的复制过来即可
    private void initializePlayer(String name) throws IOException { /* 同之前 */ }
    private void chooseStarterPokemon() throws IOException { /* 同之前 */ }
    private void showHelp() { /* 同之前 */ }
}