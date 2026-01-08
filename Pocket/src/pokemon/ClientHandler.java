package pokemon;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.*;
import java.time.*;

public class ClientHandler implements Runnable {
    public static final Map<String, ClientHandler> onlinePlayers = new ConcurrentHashMap<>();

    public static class MarketItem implements Serializable {
        private static final long serialVersionUID = 1L;
        String sellerName;
        String itemName;
        int quantity;
        int price;
        long expireTime;

        public MarketItem(String sellerName, String itemName, int quantity, int price) {
            this.sellerName = sellerName;
            this.itemName = itemName;
            this.quantity = quantity;
            this.price = price;
            this.expireTime = System.currentTimeMillis() + 24L * 60 * 60 * 1000;
        }
    }

    public static final List<MarketItem> marketItems = new ArrayList<>();
    public static final Map<String, Long> playerStalls = new HashMap<>();

    public static final Set<String> outOfStockItems = new HashSet<>();
    public static LocalDate lastOutOfStockRefreshDate = LocalDate.now();

    public static final List<String> limitedItems =
            Arrays.asList("好伤药", "攻击强化剂", "防御强化剂", "经验糖果");
    public static final int MAX_DAILY_PURCHASE = 5;

    public static final Map<String, Map<String, Integer>> dailyPurchaseCounts =
            new ConcurrentHashMap<>();
    public static LocalDate lastPurchaseResetDate = LocalDate.now();

    private Timer duelTimer;
    private boolean isDuelInitiator = false;
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

                if (activeBattle == null && duelTarget != null && isDuelInitiator) {
                    if (!input.equals("cancel")) {
                        handleCancelDuel(false);
                        out.println("(由于进行了其他操作，挑战请求已自动取消)");
                    }
                }

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
                if (activeBattle != null) {
                    activeBattle.handleDisconnect(this);
                }
                onlinePlayers.remove(player.getName());
                if (currentRoom != null) currentRoom.removePlayer(player);
                Player.savePlayer(player);
                handleCancelDuel(false);
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
            case "cancel":
                handleCancelDuel(true);
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
            case "shout":
                handleShout(input);
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

            case "market":
                if (parts.length < 2) {
                    out.println("市场指令格式：");
                    out.println("- market stall 创建摊位");
                    out.println("- market sell [物品名] [数量] [价格] 上架物品");
                    out.println("- market buy [物品名] [数量] [价格] 购买物品");
                    out.println("- market list 查看所有在售物品");
                } else {
                    String subCommand = parts[1];
                    handleMarketCommand(subCommand, parts);
                }
                break;

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
            case "where":
                showPlayersHere();
                break;
            case "say":
                handleSay(input, parts);
                break;

            case "tell":
            case "pm":
                handleTell(parts);
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
                }
                else if (input.startsWith("buy ")) {
                    if (parts.length > 1) {
                        String itemName = parts[1];
                        int count = 1;

                        if (parts.length > 2) {
                            try {
                                count = Integer.parseInt(parts[2]);
                            } catch (NumberFormatException e) {
                                out.println("数量输入错误，将默认购买 1 个。");
                            }
                        }

                        buyItem(itemName, count);
                    } else {
                        out.println("指令格式错误，请输入: buy [物品名] [数量]");
                        out.println("例如: buy 伤药 5");
                    }
                }
                else {
                    out.println("未知指令。输入 'help' 查看帮助。");
                }
                break;
        }
    }

    private void handleSay(String rawInput, String[] parts) {
        if (currentRoom == null) {
            out.println("你还没有进入任何房间，无法聊天。");
            return;
        }

        String msg = rawInput.length() >= 4 ? rawInput.substring(4).trim() : "";
        if (msg.isEmpty()) {
            out.println("用法: say [内容]");
            return;
        }

        String formatted = "[" + player.getName() + "]: " + msg;

        for (Player p : currentRoom.getPlayersSnapshot()) {
            p.sendMessage(formatted);
        }
    }

    private void handleTell(String[] parts) {
        if (parts.length < 3) {
            out.println("用法: tell [玩家名] [内容]");
            return;
        }

        String targetName = parts[1].trim();
        if (targetName.isEmpty()) {
            out.println("用法: tell [玩家名] [内容]");
            return;
        }

        String msg = joinFrom(parts, 2).trim();
        if (msg.isEmpty()) {
            out.println("用法: tell [玩家名] [内容]");
            return;
        }

        ClientHandler target = findOnlinePlayerByName(targetName);
        if (target == null || target.getPlayer() == null) {
            out.println("玩家不在线或不存在: " + targetName);
            return;
        }

        if (target.getPlayer().getName().equals(player.getName())) {
            out.println("你不能私聊自己。");
            return;
        }

        target.sendMessage("[私聊] " + player.getName() + ": " + msg);
        player.sendMessage("[私聊→" + target.getPlayer().getName() + "] " + msg);
    }

    private ClientHandler findOnlinePlayerByName(String name) {
        ClientHandler exact = onlinePlayers.get(name);
        if (exact != null) return exact;

        for (Map.Entry<String, ClientHandler> e : onlinePlayers.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(name)) {
                return e.getValue();
            }
        }
        return null;
    }

    private String joinFrom(String[] parts, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < parts.length; i++) {
            if (i > start) sb.append(" ");
            sb.append(parts[i]);
        }
        return sb.toString();
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

        out.println("开始与 " + spar.getName() + " 进行模拟对战！");

        BattleSystem battle = new BattleSystem(player, spar, out, in);
        battle.setTrainingMode(true);

        battle.startBattle();

        if (!my.isFainted() && spar.isFainted()) {
            int bonusMoney = 10 + lvl * 2;
            int bonusExp   = 5 + lvl * 2;

            player.addMoney(bonusMoney);
            my.gainExp(bonusExp);

            out.println("训练奖励：+" + bonusMoney + " 金币，+" + bonusExp + " 经验。");
            out.println("(提示) 可继续 train 训练，或 go west 回家。");
            Player.savePlayer(player);
        }else if (!my.isFainted() && !spar.isFainted()) {
            out.println("训练未完成，无法获得奖励。");
        }
    }

    private void showOnlinePlayers() {
        out.println("=== 在线玩家 ===");
        for (String name : onlinePlayers.keySet()) {
            out.println("- " + name);
        }
        out.println("(提示) duel [玩家名] 发起挑战。");
    }

    private void showPlayersHere() {
        if (currentRoom == null) {
            out.println("你当前不在任何房间。");
            return;
        }
        out.println("你当前在【" + currentRoom.getName() + "】");
        out.println(currentRoom.getPlayerNames(player));
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

        if (targetHandler.currentRoom != this.currentRoom) {
            out.println("你必须和他在同一个房间才能发起挑战！他在: " + targetHandler.currentRoom.getName());
            return;
        }

        synchronized (targetHandler) {
            if (targetHandler.activeBattle != null || targetHandler.duelTarget != null) {
                out.println("对方正忙，稍后再试。");
                return;
            }
            targetHandler.receiveDuelRequest(this);
        }

        this.duelTarget = targetHandler;
        this.isDuelInitiator = true;

        out.println("已向 " + targetName + " 发起挑战！等待对方接受...");
        out.println("(30秒内未响应将自动取消，输入任意指令可撤回)");

        stopDuelTimer();
        duelTimer = new Timer();
        duelTimer.schedule(new TimerTask(){
            @Override
            public void run() {
                if (activeBattle == null && duelTarget != null) {
                    try {
                        duelTarget.sendMessage("\n>> 响应超时，挑战请求已失效。");

                        if (duelTarget.duelTarget == ClientHandler.this) {
                            duelTarget.duelTarget = null;
                        }
                    } catch (Exception e) {
                    }

                    out.println("\n>> 对方响应超时，挑战已自动取消。");
                    out.print("> ");
                    out.flush();

                    stopDuelTimer();
                    isDuelInitiator = false;
                    duelTarget = null;
                }
            }
        }, 30000);
    }

    public void receiveDuelRequest(ClientHandler challenger) {
        this.duelTarget = challenger;
        out.println("\n收到挑战！");
        out.println("玩家 [" + challenger.getPlayer().getName() + "] 想和你 PK！");
        out.println("输入 'yes' (接受) 或 'no' (拒绝)");
    }

    public void stopDuelTimer() {
        if (duelTimer != null) {
            duelTimer.cancel();
            duelTimer = null;
        }
    }

    private void handleDuelResponse(boolean accept) {
        if (duelTarget == null) {
            out.println("目前没有人向你发起挑战。");
            return;
        }

        if (accept) {
            out.println("你接受了挑战！");
            duelTarget.sendMessage(player.getName() + " 接受了你的挑战！");

            duelTarget.stopDuelTimer();
            duelTarget.isDuelInitiator = false;
            this.stopDuelTimer();

            PvPBattle battle = new PvPBattle(duelTarget, this);

            this.activeBattle = battle;
            duelTarget.activeBattle = battle;

            battle.start();

            this.duelTarget = null;
        } else {
            out.println("你拒绝了挑战。");
            duelTarget.sendMessage(player.getName() + " 拒绝了你的挑战。");

            duelTarget.stopDuelTimer();
            duelTarget.isDuelInitiator = false;
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
        ensureDailyUpdates();

        out.println("\n=== 友好商店 ===");
        sleep(500);
        out.println("欢迎光临！请问需要点什么？");

        Map<String, Integer> playerPurchases =
                dailyPurchaseCounts.computeIfAbsent(player.getName(), k -> new HashMap<>());

        out.println(formatShopItem("伤药", "恢复20HP", 200, playerPurchases));
        out.println(formatShopItem("好伤药", "恢复50HP", 500, playerPurchases));
        out.println(formatShopItem("精灵球", "捕捉宝可梦", 200, playerPurchases));
        out.println(formatShopItem("经验糖果", "增加100经验", 300, playerPurchases));
        out.println(formatShopItem("攻击强化剂", "提升攻击力", 400, playerPurchases));
        out.println(formatShopItem("防御强化剂", "提升防御力", 400, playerPurchases));

        out.println("\n使用 'buy [物品名] [数量]' 来购买。余额: " + player.getMoney());
    }

    private String formatShopItem(String name, String description, int price, Map<String, Integer> purchases) {
        String status = "";

        if (outOfStockItems.contains(name)) {
            status = " [缺货]";
        }
        else if (limitedItems.contains(name)) {
            int purchased = purchases.getOrDefault(name, 0);
            int remaining = MAX_DAILY_PURCHASE - purchased;
            status = String.format(" [今日剩余: %d]", remaining);
        }

        return String.format("%-4s%-15s | 价格: %4d元%s", "", name + " - " + description, price, status);
    }

    private static void refreshOutOfStockItems() {
        LocalDate today = LocalDate.now();
        if (!today.equals(lastOutOfStockRefreshDate)) {
            outOfStockItems.clear();

            List<String> allItems = Arrays.asList("伤药", "好伤药", "精灵球", "经验糖果", "攻击强化剂", "防御强化剂");
            for (String item : allItems) {
                if (Math.random() < 0.05) {
                    outOfStockItems.add(item);
                }
            }

            lastOutOfStockRefreshDate = today;
        }
    }

    private static void resetDailyPurchaseCounts() {
        LocalDate today = LocalDate.now();
        if (!today.equals(lastPurchaseResetDate)) {
            dailyPurchaseCounts.clear();
            lastPurchaseResetDate = today;
        }
    }

    private static void ensureDailyUpdates() {
        refreshOutOfStockItems();
        resetDailyPurchaseCounts();
    }

    private void buyItem(String itemName, int quantity) {
        if (quantity <= 0) {
            out.println("购买数量必须大于0。");
            return;
        }

        ensureDailyUpdates();

        int price = 0;

        switch (itemName) {
            case "伤药": price = 200; break;
            case "好伤药": price = 500; break;
            case "精灵球": price = 200; break;
            case "经验糖果": price = 300; break;
            case "攻击强化剂": price = 400; break;
            case "防御强化剂": price = 400; break;
            default:
                out.println("店员：不好意思，我们要么没货，要么没这个东西。");
                return;
        }

        if (outOfStockItems.contains(itemName)) {
            out.println("店员：不好意思，" + itemName + " 目前缺货。");
            return;
        }

        if (limitedItems.contains(itemName)) {
            Map<String, Integer> playerPurchases =
                    dailyPurchaseCounts.computeIfAbsent(player.getName(), k -> new HashMap<>());
            int purchased = playerPurchases.getOrDefault(itemName, 0);
            if (purchased + quantity > MAX_DAILY_PURCHASE) {
                int remaining = MAX_DAILY_PURCHASE - purchased;
                out.println("店员：该物品每日限购" + MAX_DAILY_PURCHASE + "个，今日剩余：" + remaining);
                return;
            }
        }

        int totalPrice = price * quantity;
        if (player.getMoney() < totalPrice) {
            out.println("金钱不足！需要" + totalPrice + "元，但你只有" + player.getMoney() + "元。");
            return;
        }

        player.declineMoney(totalPrice);
        player.addItem(itemName, quantity);

        if (limitedItems.contains(itemName)) {
            Map<String, Integer> playerPurchases =
                    dailyPurchaseCounts.computeIfAbsent(player.getName(), k -> new HashMap<>());
            int purchased = playerPurchases.getOrDefault(itemName, 0);
            playerPurchases.put(itemName, purchased + quantity);
        }

        out.println("购买了 " + itemName + " x" + quantity + "！花费" + totalPrice + "元");
        out.println("剩余金钱: " + player.getMoney() + "元");
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
            currentRoom.broadcastToRoom("[系统] " + player.getName() + " 离开了房间。", player);
            currentRoom.removePlayer(this.player);
            currentRoom = nextRoom;
            currentRoom.addPlayer(this.player);
            player.setCurrentMap(currentRoom.getId());
            out.println("你进入了【" + currentRoom.getName() + "】");
            printRoomInfo();
            currentRoom.broadcastToRoom("[系统] " + player.getName() + " 进入了房间。", player);

            checkRandomEncounter();
        }
    }

    private void checkRandomEncounter() {
        PocketMon active = player.getActivePokemon();
        int playerLevel = active.getLevel();

        PocketMon wildPokemon = currentRoom.getRandomWildPokemon(playerLevel);

        if (wildPokemon != null && Math.random() < 0.3) {
            out.println("\n草丛里有什么东西在动...");
            sleep(1000);
            triggerBattle(wildPokemon);
        }
    }

    private void startActiveBattle() {
        out.println("正在寻找野生宝可梦...");
        sleep(1000);
        PocketMon active = player.getActivePokemon();
        if (active == null) {
            out.println("你还没有宝可梦，无法战斗。");
            return;
        }
        PocketMon wildPokemon = currentRoom.getRandomWildPokemon(active.getLevel());

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

        giveRandomDropAfterWildBattle();
        Player.savePlayer(player);
    }

    private void giveRandomDropAfterWildBattle() {
        double r = Math.random();
        if (r < 0.30) {
            player.addItem("伤药", 1);
        } else if (r < 0.40) {
            player.addItem("好伤药", 1);
        } else if (r < 0.45) {
            player.addItem("经验糖果", 1);
        } else {
            out.println("没有获得任何掉落。");
        }
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
        out.println("buy [物品名] [数量] - 购买商品");
        out.println("work           - 打工赚钱 (在打工场所)");
        out.println("who            - 查看在线玩家");
        out.println("where           - 查看当前房间内有哪些玩家");
        out.println("duel [玩家名]   - 向玩家发起 PvP 挑战");
        out.println("cancel         - 取消发起的挑战");
        out.println("accept/decline - 接受/拒绝挑战");
        out.println("shout [内容]    - 全服喊话（所有在线玩家可见）");
        out.println("say [内容]      - 房间聊天（同一房间玩家可见）");
        out.println("tell [玩家名] [内容] - 私聊某个在线玩家（也可用 pm）");
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

    public static void saveMarket() {
        try {
            File dir = new File("saves");
            if (!dir.exists()) dir.mkdirs();

            FileOutputStream fileOut = new FileOutputStream("saves/market_items.ser");
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(marketItems);
            out.close();
            fileOut.close();

            fileOut = new FileOutputStream("saves/player_stalls.ser");
            out = new ObjectOutputStream(fileOut);
            out.writeObject(playerStalls);
            out.close();
            fileOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadMarket() {
        try {
            File file = new File("saves/market_items.ser");
            if (file.exists()) {
                FileInputStream fileIn = new FileInputStream(file);
                ObjectInputStream in = new ObjectInputStream(fileIn);
                List<MarketItem> loadedItems = (List<MarketItem>) in.readObject();
                marketItems.clear();
                marketItems.addAll(loadedItems);
                in.close();
                fileIn.close();
            }

            file = new File("saves/player_stalls.ser");
            if (file.exists()) {
                FileInputStream fileIn = new FileInputStream(file);
                ObjectInputStream in = new ObjectInputStream(fileIn);
                Map<String, Long> loadedStalls = (Map<String, Long>) in.readObject();
                playerStalls.clear();
                playerStalls.putAll(loadedStalls);
                in.close();
                fileIn.close();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void handleMarketCommand(String subCommand, String[] parts) {
        cleanupExpiredItems();

        switch (subCommand) {
            case "stall":
                handleMarketStall();
                break;
            case "sell":
                if (parts.length < 5) {
                    out.println("指令格式：market sell [物品名] [数量] [价格]");
                } else {
                    try {
                        String itemName = parts[2];
                        int quantity = Integer.parseInt(parts[3]);
                        int price = Integer.parseInt(parts[4]);
                        handleMarketSell(itemName, quantity, price);
                    } catch (NumberFormatException e) {
                        out.println("数量和价格必须是数字");
                    }
                }
                break;
            case "buy":
                if (parts.length < 5) {
                    out.println("指令格式：market buy [物品名] [数量] [价格]");
                } else {
                    try {
                        String itemName = parts[2];
                        int quantity = Integer.parseInt(parts[3]);
                        int price = Integer.parseInt(parts[4]);
                        handleMarketBuy(itemName, quantity, price);
                    } catch (NumberFormatException e) {
                        out.println("数量和价格必须是数字");
                    }
                }
                break;
            case "list":
                handleMarketList();
                break;
            default:
                out.println("未知的市场指令：" + subCommand);
                break;
        }
    }

    private void cleanupExpiredItems() {
        long now = System.currentTimeMillis();
        marketItems.removeIf(item -> item.expireTime < now);
        playerStalls.entrySet().removeIf(entry -> entry.getValue() + 24L * 60 * 60 * 1000 < now);
        saveMarket();
    }

    private void handleMarketStall() {
        if (currentRoom == null || !"market".equals(currentRoom.getId())) {
            out.println("只有在交易市场才能创建摊位！");
            return;
        }

        if (playerStalls.containsKey(player.getName())) {
            long createdAt = playerStalls.get(player.getName());
            long remainingMs = createdAt + 24L * 60 * 60 * 1000 - System.currentTimeMillis();
            long remainingMin = Math.max(0, remainingMs / (60 * 1000));
            out.println("你已经创建了摊位，剩余时间：" + remainingMin + "分钟");
            return;
        }

        playerStalls.put(player.getName(), System.currentTimeMillis());
        out.println("你创建了一个临时摊位（持续24小时）！");
        saveMarket();
    }

    private void handleMarketSell(String itemName, int quantity, int price) {
        if (currentRoom == null || !"market".equals(currentRoom.getId())) {
            out.println("只有在交易市场才能上架物品！");
            return;
        }

        if (quantity <= 0 || price <= 0) {
            out.println("数量和价格必须大于0。");
            return;
        }

        if (!playerStalls.containsKey(player.getName())) {
            out.println("你还没有创建摊位！请先输入 market stall 创建摊位");
            return;
        }

        int playerQuantity = player.getBag().getOrDefault(itemName, 0);
        if (playerQuantity < quantity) {
            out.println("你没有足够的 " + itemName + "（当前拥有：" + playerQuantity + "）");
            return;
        }

        int minPrice = getMinMarketPrice(itemName);
        if (minPrice > 0 && price < minPrice) {
            out.println("该物品最低售价为 " + minPrice + " 元/个（商店售价的50%）。");
            return;
        }

        if (!player.removeItem(itemName, quantity)) {
            out.println("上架失败：背包物品数量不足。");
            return;
        }

        marketItems.add(new MarketItem(player.getName(), itemName, quantity, price));
        out.println("成功上架 " + itemName + " x" + quantity + "，售价：" + price + "元/个");
        Player.savePlayer(player);
        saveMarket();
    }

    private void handleMarketBuy(String itemName, int quantity, int price) {
        if (currentRoom == null || !"market".equals(currentRoom.getId())) {
            out.println("只有在交易市场才能购买物品！");
            return;
        }

        if (quantity <= 0) {
            out.println("购买数量必须大于0。");
            return;
        }

        List<MarketItem> matching = new ArrayList<>();
        int available = 0;
        for (MarketItem it : marketItems) {
            if (it.itemName.equals(itemName) && it.price == price && !it.sellerName.equals(player.getName())) {
                matching.add(it);
                available += it.quantity;
            }
        }

        if (available < quantity) {
            out.println("市场上没有足够的 " + itemName + "（当前可用：" + available + "）");
            return;
        }

        int totalPrice = price * quantity;
        if (player.getMoney() < totalPrice) {
            out.println("金钱不足！需要" + totalPrice + "元，但你只有" + player.getMoney() + "元。");
            return;
        }

        int remaining = quantity;
        Iterator<MarketItem> it = matching.iterator();
        while (remaining > 0 && it.hasNext()) {
            MarketItem mi = it.next();
            int take = Math.min(remaining, mi.quantity);

            player.addItem(itemName, take);

            int earn = take * price;
            ClientHandler sellerHandler = onlinePlayers.get(mi.sellerName);
            if (sellerHandler != null && sellerHandler.player != null) {
                sellerHandler.player.addMoney(earn);
                sellerHandler.out.println("你的 " + itemName + " x" + take + " 已被购买，获得 " + earn + " 元");
                Player.savePlayer(sellerHandler.player);
            } else {
                Player seller = Player.loadPlayer(mi.sellerName);
                if (seller != null) {
                    seller.addMoney(earn);
                    Player.savePlayer(seller);
                }
            }

            mi.quantity -= take;
            if (mi.quantity <= 0) {
                marketItems.remove(mi);
            }

            remaining -= take;
        }

        player.declineMoney(totalPrice);

        out.println("成功购买 " + itemName + " x" + quantity + "，花费：" + totalPrice + "元");
        out.println("剩余金钱: " + player.getMoney() + "元");
        Player.savePlayer(player);
        saveMarket();
    }

    private void handleMarketList() {
        cleanupExpiredItems();

        if (marketItems.isEmpty()) {
            out.println("市场上没有在售物品！");
            return;
        }

        out.println("=== 交易市场商品列表 ===");
        for (int i = 0; i < marketItems.size(); i++) {
            MarketItem item = marketItems.get(i);
            long remainingMs = item.expireTime - System.currentTimeMillis();
            long remainingHours = Math.max(0, remainingMs / (1000 * 60 * 60));
            long remainingMinutes = Math.max(0, (remainingMs % (1000 * 60 * 60)) / (1000 * 60));
            out.printf("%d. %s x%d - 卖家: %s - 价格: %d元/个 - 剩余时间: %d小时%d分钟\n",
                    i + 1, item.itemName, item.quantity, item.sellerName, item.price, remainingHours, remainingMinutes);
        }
    }

    private int getMinMarketPrice(String itemName) {
        int shopPrice = 0;
        switch (itemName) {
            case "伤药": shopPrice = 200; break;
            case "好伤药": shopPrice = 500; break;
            case "精灵球": shopPrice = 200; break;
            case "经验糖果": shopPrice = 300; break;
            case "攻击强化剂": shopPrice = 400; break;
            case "防御强化剂": shopPrice = 400; break;
            default: shopPrice = 0;
        }
        return (int) (shopPrice * 0.5);
    }

    private void sleep(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }

    private void handleShout(String rawInput) {
        String msg = rawInput.length() >= 6 ? rawInput.substring(6).trim() : "";
        if (msg.isEmpty()) {
            out.println("用法: shout [内容]");
            return;
        }

        String formatted = "[全服] " + player.getName() + ": " + msg;

        for (ClientHandler ch : onlinePlayers.values()) {
            if (ch != null && ch.getPlayer() != null) {
                ch.getPlayer().sendMessage(formatted);
            }
        }
    }

    private void handleCancelDuel(boolean notify) {
        if (duelTimer != null) {
            duelTimer.cancel();
            duelTimer = null;
        }

        duelTarget = null;

        if (notify) {
            out.println("对战请求已取消。");
        }
    }

}