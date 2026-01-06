package pokemon;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.*;

public class ClientHandler implements Runnable {
    public static final Map<String, ClientHandler> onlinePlayers = new ConcurrentHashMap<>();

    private final Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private Player player;
    private Room currentRoom;
    private boolean gameRunning = true;

    public ClientHandler duelTarget;
    public PvPBattle activeBattle;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public Player getPlayer() { return player; }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            out.println("欢迎来到《宝可梦多人联机 MUD》！");
            out.println("请输入你的名字：");

            String name = in.readLine();
            if (name == null) return;
            name = name.trim();

            if (isSaveExists(name)) {
                player = Player.loadPlayer(name);
                out.println("读取存档成功，欢迎回来 " + player.getName() + "！");
            } else {
                out.println("新玩家创建成功，欢迎你 " + name + "！");
                initializePlayer(name);
            }

            player.setOut(out);

            if (player.getCurrentMap() != null) {
                currentRoom = WorldManager.getRoom(player.getCurrentMap());
            }
            if (currentRoom == null) {
                currentRoom = WorldManager.getStartRoom();
                player.setCurrentMap(currentRoom.getId());
            }

            onlinePlayers.put(player.getName(), this);
            broadcast("玩家 " + player.getName() + " 上线了！当前在线：" + onlinePlayers.size() + " 人");

            if (currentRoom != null) currentRoom.addPlayer(this.player);

            showHelp();
            printRoomInfo();
            out.println(WorldManager.getAsciiMap(currentRoom.getId()));

            while (gameRunning) {

                if (activeBattle == null) {
                    out.print("> ");
                    out.flush();
                }

                String input = in.readLine();
                if (input == null) break;

                input = input.trim();
                if (input.isEmpty()) continue;

                if (activeBattle != null) {
                    activeBattle.handleInput(this, input);
                } else {
                    processCommand(input);
                }
            }

        } catch (IOException e) {
            System.out.println("玩家 [" + (player != null ? player.getName() : "Unknown") + "] 断开连接");
        } finally {
            if (player != null) {
                onlinePlayers.remove(player.getName());
                if (currentRoom != null) currentRoom.removePlayer(player);
                Player.savePlayer(player);
            }
            try { socket.close(); } catch (IOException e) {}
        }
    }

    private boolean isSaveExists(String name) {
        File f = new File("saves/" + name + ".ser");
        return f.exists();
    }

    private void initializePlayer(String name) throws IOException {
        out.println("\n【真新镇 - 大木研究所】");
        sleep(800);
        out.println("阳光透过窗户洒在地板上。墙上贴着各种宝可梦的海报，桌上整齐地摆放着研究资料。");
        sleep(1200);
        out.println("突然，楼下传来声音：\"快来，大木博士在等你！\"");
        sleep(1000);

        this.player = new Player(name);
        this.player.setOut(out);

        sleep(800);
        out.println("\n博士：\"欢迎，" + name + "！你是刚满10岁的新人训练家吧。\"");
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
        out.println("妙蛙种子 - 草系宝可梦，性格温和，背上的种子会开花。");
        sleep(1000);
        out.println("小火龙 - 火系宝可梦，尾巴上的火焰代表它的心情。");
        sleep(1000);
        out.println("杰尼龟 - 水系宝可梦，擅长游泳，遇到危险会缩进壳里。");
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
        out.println("- 宝可梦图鉴 (未激活)");
        out.println("- 精灵球 x5");
        out.println("- 伤药 x3");
        out.println("- 1000元 零花钱");
        sleep(1000);
        out.println("\n博士：\"好了，去冒险吧！目标是成为宝可梦大师！\"");
        sleep(2000);
        out.println("\n(按回车键走出研究所...)");
        try { in.readLine(); } catch (IOException e) {}
    }

    private void processCommand(String input) {
        String[] parts = input.split(" ");
        String command = parts[0];

        switch (command) {
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

            case "n": case "north": handleMove("north"); break;
            case "s": case "south": handleMove("south"); break;
            case "e": case "east": handleMove("east"); break;
            case "w": case "west": handleMove("west"); break;

            case "look": printRoomInfo(); break;
            case "status": out.println(player.getStatus()); break;
            case "bag": out.println(player.getBagContent()); break;
            case "map":
                out.println(WorldManager.getAsciiMap(currentRoom != null ? currentRoom.getId() : ""));
                break;
            case "go":
                if (parts.length >= 2) handleMove(parts[1]);
                else out.println("指令格式：go 方向");
                break;

            case "heal":
                if (currentRoom != null && currentRoom.getId().equals("pokemon_center")) {
                    out.println("乔伊小姐：欢迎来到宝可梦中心！");
                    sleep(500);
                    out.println("乔伊小姐：你的宝可梦恢复精神了！");
                    player.healTeam();
                    Player.savePlayer(player);
                } else {
                    out.println("这里不是【宝可梦中心】，无法治疗！");
                }
                break;

            case "battle":
                startActiveBattle();
                break;

            case "train":
                handleTrain();
                break;

            case "shop": showShop(); break;

            case "work":
                if (currentRoom != null && currentRoom.getId().equals("work_place")) {
                    player.work();
                    Player.savePlayer(player);
                } else {
                    out.println("这里不能打工！请去【常青市】北边的打工场所。");
                }
                break;
            case "who":
                showOnlinePlayers();
                break;

            case "help": showHelp(); break;
            case "quit": case "exit":
                out.println("再见！");
                gameRunning = false;
                break;

            case "save":
                if(Player.savePlayer(player)) {
                    out.println("存档成功！");
                } else {
                    out.println("存档失败！");
                }
                break;

            case "load":
                Player loaded = Player.loadPlayer(player.getName());
                if (loaded != null) {
                    player = loaded;
                    player.setOut(out);

                    if (player.getCurrentMap() != null) {
                        Room savedRoom = WorldManager.getRoom(player.getCurrentMap());
                        if (savedRoom != null) {
                            if (currentRoom != null) currentRoom.removePlayer(this.player);
                            currentRoom = savedRoom;
                            currentRoom.addPlayer(this.player);
                        }
                    }
                    out.println("读档完成！当前进度已覆盖为存档内容。");
                }
                break;
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

    private void handleTrain(){
        if (currentRoom == null || !"training_town".equals(currentRoom.getId())) {
            out.println("你不在训练镇，无法训练。请去 training_town。");
            return;
        }

        PocketMon my = player.getFirstPokemon();
        if (my == null) {
            out.println("你还没有宝可梦，无法训练。");
            return;
        }

        int lvl = Math.max(1, my.getLevel());
        PocketMon spar = new PocketMon("练习木桩", PocketMon.Type.NORMAL, lvl);

        triggerBattle(spar);

        if (!my.isFainted()) {
            int bonusMoney = 10 + lvl * 2;
            int bonusExp   = 5 + lvl * 2;

            player.addMoney(bonusMoney);
            my.gainExp(bonusExp);

            out.println("训练奖励：+" + bonusMoney + " 金币，+" + bonusExp + " 经验。");
            out.println("(提示) 可继续 train 训练，或 go west 回家。");
            Player.savePlayer(player);
        }
    }

    private void showOnlinePlayers() {
        out.println("=== 在线玩家 ===");
        for (String name : onlinePlayers.keySet()) {
            out.println("- " + name);
        }
        out.println("(提示) duel [玩家名] 发起挑战。");
    }

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

        if (targetHandler.currentRoom != this.currentRoom) {
            out.println("你必须和他在同一个房间才能发起挑战！他在: " + targetHandler.currentRoom.getName());
            return;
        }

        this.duelTarget = targetHandler;
        targetHandler.receiveDuelRequest(this);
        out.println("已向 " + targetName + " 发起挑战！等待对方接受...");
    }

    public void receiveDuelRequest(ClientHandler challenger) {
        this.duelTarget = challenger;
        out.println("\n收到挑战！");
        out.println("玩家 [" + challenger.getPlayer().getName() + "] 想和你 PK！");
        out.println("输入 'yes' (接受) 或 'no' (拒绝)");
    }

    private void handleDuelResponse(boolean accept) {
        if (duelTarget == null) {
            out.println("目前没有人向你发起挑战。");
            return;
        }

        if (accept) {
            out.println("你接受了挑战！");
            duelTarget.sendMessage(player.getName() + " 接受了你的挑战！");

            PvPBattle battle = new PvPBattle(duelTarget, this);

            this.activeBattle = battle;
            duelTarget.activeBattle = battle;

            battle.start();

            this.duelTarget = null;
        } else {
            out.println("你拒绝了挑战。");
            duelTarget.sendMessage(player.getName() + " 拒绝了你的挑战。");
            duelTarget.duelTarget = null;
            this.duelTarget = null;
        }
    }

    public void endPvP() {
        this.activeBattle = null;
        this.duelTarget = null;
        out.println("PvP 结束，回归自由行动模式。");
        printRoomInfo();
        Player.savePlayer(player);
    }

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
        Player.savePlayer(player);
    }

    private void broadcast(String msg) {
        for (ClientHandler ch : onlinePlayers.values()) {
            ch.sendMessage(msg);
        }
    }

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
            player.setCurrentMap(currentRoom.getId());
            printRoomInfo();

            checkRandomEncounter();
        }
    }

    private void checkRandomEncounter() {
        PocketMon wildPokemon = currentRoom.getRandomWildPokemon();
        if (wildPokemon != null && Math.random() < 0.3) {
            out.println("\n草丛里有什么东西在动...");
            sleep(1000);
            triggerBattle(wildPokemon);
        }
    }

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

    private void triggerBattle(PocketMon wildPokemon) {
        out.println("野生的 " + wildPokemon.getName() + " 跳出来了！");

        BattleSystem battle = new BattleSystem(player, wildPokemon, out, in);
        battle.startBattle();

        if (!player.getFirstPokemon().isFainted()) {
            out.println("战斗结束，你赢了！");
        } else {
            out.println("你输了，眼前一黑...");
        }
        Player.savePlayer(player);
    }

    private void showHelp() {
        out.println("\n=== 指令帮助 ===");
        out.println("go [方向]      - 移动 (north/south/east/west)");
        out.println("look           - 查看当前房间信息");
        out.println("map            - 查看地图");
        out.println("status         - 查看状态");
        out.println("bag            - 查看背包");
        out.println("use [道具名]    - 使用道具（使用后有提示）");
        out.println("battle         - 主动触发野外战斗");
        out.println("train          - 训练镇练习战斗（训练后额外奖励）");
        out.println("heal           - 在宝可梦中心治疗");
        out.println("shop           - 查看商店商品 (在商店中)");
        out.println("buy [物品名]    - 购买商品 (在商店中，购买后有提示)");
        out.println("work           - 打工赚钱 (在打工场所)");
        out.println("who            - 查看在线玩家");
        out.println("duel [玩家名]   - 向玩家发起 PvP 挑战");
        out.println("accept/decline - 接受/拒绝挑战");
        out.println("save           - 保存存档");
        out.println("load           - 读取存档（覆盖当前进度）");
        out.println("exit/quit      - 退出游戏");
        out.println("help           - 查看帮助");
        out.println("================\n");
    }

    private void printRoomInfo() {
        if (currentRoom == null) return;
        out.println("\n================================");
        out.println(currentRoom.getFullDescription());
        out.println("可用出口: " + currentRoom.getAvailableExits());
        out.println(currentRoom.getPlayerNames(this.player));
        out.println("================================");
    }

    public void setOut(PrintWriter out) {
        this.out = out;
    }

    public void sendMessage(String msg) {
        out.println(msg);
    }

    private void sleep(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }
}