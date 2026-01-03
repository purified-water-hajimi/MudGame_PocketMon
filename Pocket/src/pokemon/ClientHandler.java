package pokemon;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler implements Runnable {
    // 全局在线玩家列表 (名字 -> 处理器)
    public static final Map<String, ClientHandler> onlinePlayers = new ConcurrentHashMap<>();

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private Player player;
    private Room currentRoom;
    private boolean gameRunning = true;

    // PvP 相关状态
    private ClientHandler duelTarget; // 我正在向谁发起挑战 / 谁向我发起了挑战
    private PvPBattle activeBattle;   // 当前正在进行的战斗对象

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    // 供 PvPBattle 调用，获取对应的 Player 对象
    public Player getPlayer() { return player; }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "GBK"));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "GBK"), true);

            // ==========================================
            // 1. 开场剧情与初始化
            showGameIntroduction();
            initializePlayer();

            // ==========================================
            // 2. 注册到在线列表
            if (player != null) {
                onlinePlayers.put(player.getName(), this);
            }

            // ==========================================
            // 3. 进入游戏世界
            if (currentRoom == null) currentRoom = WorldManager.getStartRoom();
            if (currentRoom != null) currentRoom.addPlayer(this.player);

            printRoomInfo();

            // ==========================================
            // 4. 主循环 (指令监听)
            String inputLine;
            while (gameRunning && (inputLine = in.readLine()) != null) {
                inputLine = inputLine.trim();
                if (inputLine.isEmpty()) continue;

                System.out.println("玩家 [" + player.getName() + "] 输入: " + inputLine);

                // 如果是 PvP 状态，所有指令交给裁判处理
                if (activeBattle != null) {
                    activeBattle.handleInput(this, inputLine.toLowerCase());
                } else {
                    // 否则处理普通指令
                    processCommand(inputLine.toLowerCase());
                }
            }

        } catch (IOException e) {
            System.out.println("玩家 [" + (player != null ? player.getName() : "Unknown") + "] 断开连接");
        } finally {
            // 玩家下线清理逻辑
            if (player != null) {
                onlinePlayers.remove(player.getName()); // 从在线列表移除
                if (currentRoom != null) currentRoom.removePlayer(player);
            }
            try { socket.close(); } catch (IOException e) {}
        }
    }

    // ============================================================
    // 剧情与初始化

    private void showGameIntroduction() {
        out.println("=== 宝可梦 MUD 游戏 (联机 PvP 版) ===");
        sleep(1000);
        out.println("\n欢迎！这是一个充满冒险和挑战的世界。");
        sleep(1200);
        showHelp();
        sleep(1000);
        out.println("\n按回车键开始冒险...");
        try { in.readLine(); } catch (IOException e) {}
    }

    private void initializePlayer() throws IOException {
        out.println("\n【真新镇 - 大木研究所】");
        sleep(800);
        out.println("阳光透过窗户洒在地板上。墙上贴着各种宝可梦的海报，桌上整齐地摆放着研究资料。");
        sleep(1200);
        out.println("突然，楼下传来声音：\"快来，大木博士在等你！\"");
        sleep(1000);

        out.println("\n请输入你的名字: ");
        String playerName = in.readLine();
        if (playerName == null || playerName.trim().isEmpty()) playerName = "小智";

        this.player = new Player(playerName);
        // 如果你的 Player 类支持 setClientHandler，请取消下面这行的注释
        // this.player.setClientHandler(this);

        sleep(800);
        out.println("\n博士：\"欢迎，" + playerName + "！你是刚满10岁的新人训练家吧。\"");
        sleep(1500);
        out.println("博士指着桌上的三个精灵球...");
        sleep(1200);
        out.println("博士：\"你也看到了，这里有三只宝可梦...\"");
        sleep(1000);

        chooseStarterPokemon();
    }

    private void chooseStarterPokemon() throws IOException {
        out.println("\n大木博士：\"这三个精灵球里，装着你的初始伙伴。慎重选择吧！\"");
        sleep(1500);

        out.println("\n桌上放着三个精灵球：");
        sleep(800);
        out.println("🌱 妙蛙种子 - 草系宝可梦，性格温和，背上的种子会开花。");
        sleep(1000);
        out.println("🔥 小火龙 - 火系宝可梦，尾巴上的火焰代表它的心情。");
        sleep(1000);
        out.println("💧 杰尼龟 - 水系宝可梦，擅长游泳，遇到危险会缩进壳里。");
        sleep(1000);
        out.println("\n你的宿敌（看起来很拽的样子）正盯着你，好像在等你先选...");
        sleep(1500);

        boolean validChoice = false;
        while (!validChoice) {
            out.println("\n请选择你的伙伴 (输入 妙蛙种子/小火龙/杰尼龟): ");
            String choice = in.readLine();
            if (choice == null) break;
            choice = choice.trim();

            if (choice.contains("妙蛙") || choice.contains("种子")) {
                out.println("\n你拿起了标有草系图案的精灵球。\"就是你了，妙蛙种子！\"");
                sleep(1000);
                out.println("妙蛙种子跳了出来，开心地蹭了蹭你的腿。");
                out.println("大木博士：\"不错的选择！草系宝可梦很容易饲养。\"");
                player.setStarterPokemon(new PocketMon("妙蛙种子", PocketMon.Type.GRASS, 5));
                validChoice = true;
            } else if (choice.contains("小火龙") || choice.contains("火")) {
                out.println("\n你拿起了标有火系图案的精灵球。\"就是你了，小火龙！\"");
                sleep(1000);
                out.println("小火龙跳了出来，尾巴上的火焰燃烧得更旺了。");
                out.println("大木博士：\"很有精神的选择！火系宝可梦非常有潜力。\"");
                player.setStarterPokemon(new PocketMon("小火龙", PocketMon.Type.FIRE, 5));
                validChoice = true;
            } else if (choice.contains("杰尼龟") || choice.contains("水")) {
                out.println("\n你拿起了标有水系图案的精灵球。\"就是你了，杰尼龟！\"");
                sleep(1000);
                out.println("杰尼龟跳了出来，自信地拍了拍胸脯。");
                out.println("大木博士：\"明智的选择！水系宝可梦在很多道馆都占优势。\"");
                player.setStarterPokemon(new PocketMon("杰尼龟", PocketMon.Type.WATER, 5));
                validChoice = true;
            } else {
                out.println("并没有这只宝可梦哦，请重新选择。");
            }
        }

        out.println("\n大木博士递给你一个背包：");
        sleep(800);
        out.println("- 📘 宝可梦图鉴 (未激活)");
        out.println("- 🔴 精灵球 x5");
        out.println("- 💊 伤药 x3");
        out.println("- 💰 1000元 零花钱");
        sleep(1000);
        out.println("\n博士：\"好了，去冒险吧！目标是成为宝可梦大师！\"");
        sleep(2000);
        out.println("\n(按回车键走出研究所...)");
        try { in.readLine(); } catch (IOException e) {}
    }

    // ============================================================
    // 核心指令处理 (包含 PvP 指令 + PvE 战斗 + 商店)

    private void processCommand(String input) {
        String[] parts = input.split(" ");
        String command = parts[0];

        switch (command) {
            // --- PvP 玩家对战指令 ---
            case "pk":
            case "duel":
                if (parts.length < 2) {
                    out.println("指令格式: pk [玩家名字] (例如: pk 小茂)");
                } else {
                    handleDuelRequest(parts[1]);
                }
                break;

            case "accept":
            case "yes":
            case "y":
                handleDuelResponse(true);
                break;

            case "reject":
            case "no":
                handleDuelResponse(false);
                break;

            // --- 移动指令 ---
            case "n": case "north": handleMove("north"); break;
            case "s": case "south": handleMove("south"); break;
            case "e": case "east": handleMove("east"); break;
            case "w": case "west": handleMove("west"); break;

            // --- 观察与状态 ---
            case "look": printRoomInfo(); break;
            case "status": out.println(player.getStatus()); break;
            case "bag": out.println(player.getBagContent()); break;
            case "map": out.println("你拿出地图看了一眼... (地图功能开发中)"); break;

            // --- 治疗 ---
            case "heal":
                if (currentRoom != null && currentRoom.getId().equals("pokemon_center")) {
                    out.println("乔伊小姐：欢迎来到宝可梦中心！");
                    sleep(500);
                    out.println("乔伊小姐：你的宝可梦恢复精神了！");
                    out.println(player.healTeam());
                } else {
                    out.println("这里不是【宝可梦中心】，无法治疗！");
                }
                break;

            // --- PvE 野外战斗 (主动触发) ---
            case "battle":
                startActiveBattle();
                break;

            // --- 商店 ---
            case "shop": showShop(); break;

            // --- 打工 ---
            case "work":
                if (currentRoom != null && currentRoom.getId().equals("work_place")) {
                    out.println(player.work());
                } else {
                    out.println("这里不能打工！请去【常青市】北边的打工场所。");
                }
                break;

            // --- 帮助与退出 ---
            case "help": showHelp(); break;
            case "quit": case "exit":
                out.println("再见！");
                gameRunning = false;
                break;

            // --- 复合指令 (use/buy) ---
            default:
                if (input.startsWith("use ")) {
                    if (parts.length > 1) player.useItem(parts[1]);
                    else out.println("指令格式错误，请输入: use 物品名");
                } else if (input.startsWith("buy ")) {
                    if (parts.length > 1) buyItem(parts[1]);
                    else out.println("指令格式错误，请输入: buy 物品名");
                } else {
                    out.println("未知指令。输入 'help' 查看帮助。");
                }
                break;
        }
    }

    // ============================================================
    // PvP 专用逻辑 (发起、接受、结束)

    private void handleDuelRequest(String targetName) {
        if (targetName.equals(player.getName())) {
            out.println("你不能和自己打架！");
            return;
        }

        ClientHandler targetHandler = onlinePlayers.get(targetName);
        if (targetHandler == null) {
            out.println("找不到玩家: " + targetName + " (他必须在线且名字输入完全正确)");
            return;
        }

        if (targetHandler.activeBattle != null || targetHandler.duelTarget != null) {
            out.println("对方正忙，稍后再试。");
            return;
        }

        // 检查两人是否在同一个房间
        if (targetHandler.currentRoom != this.currentRoom) {
            out.println("你必须和他在同一个房间才能发起挑战！他在: " + targetHandler.currentRoom.getName());
            return;
        }

        // 发送请求
        this.duelTarget = targetHandler;
        targetHandler.receiveDuelRequest(this);
        out.println("已向 " + targetName + " 发起挑战！等待对方接受...");
    }

    // 被挑战方收到消息
    public void receiveDuelRequest(ClientHandler challenger) {
        this.duelTarget = challenger;
        out.println("\n🔥 收到挑战！");
        out.println("玩家 [" + challenger.getPlayer().getName() + "] 想和你 PK！");
        out.println("输入 'yes' (接受) 或 'no' (拒绝)");
    }

    // 处理接受或拒绝
    private void handleDuelResponse(boolean accept) {
        if (duelTarget == null) {
            out.println("目前没有人向你发起挑战。");
            return;
        }

        if (accept) {
            out.println("你接受了挑战！");
            duelTarget.sendMessage(player.getName() + " 接受了你的挑战！");

            PvPBattle battle = new PvPBattle(duelTarget, this);

            // 设置双方状态为“战斗中”
            this.activeBattle = battle;
            duelTarget.activeBattle = battle;

            // 启动战斗
            battle.start();

            // 清空待处理目标
            this.duelTarget = null;
        } else {
            out.println("你拒绝了挑战。");
            duelTarget.sendMessage(player.getName() + " 拒绝了你的挑战。");
            duelTarget.duelTarget = null;
            this.duelTarget = null;
        }
    }

    // 战斗结束回调
    public void endPvP() {
        this.activeBattle = null;
        this.duelTarget = null;
        out.println("PvP 结束，回归自由行动模式。");
        printRoomInfo();
    }

    // ============================================================
    // 商店与道具逻辑

    private void showShop() {
        out.println("\n=== 友好商店 ===");
        sleep(500);
        out.println("欢迎光临！请问需要点什么？");
        out.println("1. 伤药       - 恢复20HP   | 价格: 200元");
        out.println("2. 好伤药     - 恢复50HP   | 价格: 500元");
        out.println("3. 精灵球     - 捕捉宝可梦 | 价格: 200元");
        out.println("4. 经验糖果   - 增加100经验| 价格: 300元");
        out.println("5. 攻击强化剂 - 提升攻击力 | 价格: 400元");
        out.println("6. 防御强化剂 - 提升防御力 | 价格: 400元");
        out.println("\n使用 'buy [物品名]' 来购买。余额: " + player.getMoney());
    }

    private void buyItem(String itemName) {
        switch (itemName) {
            case "伤药": player.buyItem("伤药", 200); break;
            case "好伤药": player.buyItem("好伤药", 500); break;
            case "精灵球": player.buyItem("精灵球", 200); break;
            case "经验糖果": player.buyItem("经验糖果", 300); break;
            case "攻击强化剂": player.buyItem("攻击强化剂", 400); break;
            case "防御强化剂": player.buyItem("防御强化剂", 400); break;
            default: out.println("店员：没有这种商品哦。"); return;
        }
        out.println("(系统) 正在尝试购买 " + itemName + "...");
    }

    // ============================================================
    // 移动与 PvE 战斗逻辑

    private void handleMove(String direction) {
        String nextRoomId = currentRoom.getExit(direction);
        if (nextRoomId == null) {
            out.println("那个方向没有路！");
            return;
        }
        Room nextRoom = WorldManager.getRoom(nextRoomId);
        if (nextRoom != null) {
            currentRoom.removePlayer(this.player);
            currentRoom = nextRoom;
            currentRoom.addPlayer(this.player);
            printRoomInfo();

            // 移动后触发随机遇敌 (30%)
            checkRandomEncounter();
        }
    }

    // 随机遇敌 (PvE)
    private void checkRandomEncounter() {
        PocketMon wildPokemon = currentRoom.getRandomWildPokemon();
        if (wildPokemon != null && Math.random() < 0.3) {
            out.println("\n⚠️ 草丛里有什么东西在动...");
            sleep(1000);
            triggerBattle(wildPokemon);
        }
    }

    // 主动找怪 (PvE)
    private void startActiveBattle() {
        out.println("正在寻找野生宝可梦...");
        sleep(1000);
        PocketMon wildPokemon = currentRoom.getRandomWildPokemon();
        if (wildPokemon != null) {
            triggerBattle(wildPokemon);
        } else {
            out.println("这里静悄悄的，什么也没有。(请去有宝可梦的区域)");
        }
    }

    // 触发 PvE 战斗
    private void triggerBattle(PocketMon wildPokemon) {
        out.println("野生的 " + wildPokemon.getName() + " 跳出来了！");

        BattleSystem battle = new BattleSystem(player, wildPokemon, out, in);
        battle.startBattle();

        if (!player.getFirstPokemon().isFainted()) {
            out.println("战斗结束，你赢了！");
        } else {
            out.println("你输了，眼前一黑...");
        }
    }

    // ============================================================
    // 辅助方法 (Help, PrintInfo, SendMessage)

    private void showHelp() {
        out.println("\n=== 游戏操作指南 ===");
        out.println("⚔️ PvP对战: pk [名字] (发起), yes/no (接受/拒绝)");
        out.println("🏃 移动指令: n/s/e/w (或 go north)");
        out.println("🔍 状态查看: status, bag, look");
        out.println("👹 野外战斗: battle (主动搜寻), heal (治疗)");
        out.println("🛒 商店交易: shop, buy [物品名]");
        out.println("💼 其他指令: work, use [物品名], help, exit");
    }

    private void printRoomInfo() {
        if (currentRoom == null) return;
        out.println("\n================================");
        out.println(currentRoom.getFullDescription());
        out.println("可用出口: " + currentRoom.getAvailableExits());
        // 显示当前房间的其他玩家
        out.println(currentRoom.getPlayerNames(this.player));
        out.println("================================");
    }

    public void sendMessage(String msg) {
        out.println(msg);
    }

    private void sleep(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }
}