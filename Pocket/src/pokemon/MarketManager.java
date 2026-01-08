package pokemon;

import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MarketManager {
    private ClientHandler handler;

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

    public static final List<String> limitedItems = Arrays.asList("好伤药", "攻击强化剂", "防御强化剂", "经验糖果");
    public static final int MAX_DAILY_PURCHASE = 5;
    public static final Map<String, Map<String, Integer>> dailyPurchaseCounts = new ConcurrentHashMap<>();
    public static LocalDate lastPurchaseResetDate = LocalDate.now();

    public MarketManager(ClientHandler handler) {
        this.handler = handler;
    }

    public void handleCommand(String subCommand, String[] parts) {
        cleanupExpiredItems();
        switch (subCommand) {
            case "stall": handleStall(); break;
            case "sell":
                if (parts.length < 5) handler.sendMessage("指令格式：market sell [物品名] [数量] [价格]");
                else {
                    try {
                        handleSell(parts[2], Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
                    } catch (NumberFormatException e) { handler.sendMessage("数量和价格必须是数字"); }
                }
                break;
            case "buy":
                if (parts.length < 5) handler.sendMessage("指令格式：market buy [物品名] [数量] [价格]");
                else {
                    try {
                        handleMarketBuy(parts[2], Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
                    } catch (NumberFormatException e) { handler.sendMessage("数量和价格必须是数字"); }
                }
                break;
            case "list": handleList(); break;
            default: handler.sendMessage("未知的市场指令：" + subCommand); break;
        }
    }

    public void showShop() {
        ensureDailyUpdates();
        handler.sendMessage("\n=== 友好商店 ===");
        try { Thread.sleep(500); } catch (Exception e) {}
        handler.sendMessage("欢迎光临！请问需要点什么？");

        Map<String, Integer> playerPurchases = dailyPurchaseCounts.computeIfAbsent(handler.getPlayer().getName(), k -> new HashMap<>());

        handler.sendMessage(formatShopItem("伤药", "恢复20HP", 200, playerPurchases));
        handler.sendMessage(formatShopItem("好伤药", "恢复50HP", 500, playerPurchases));
        handler.sendMessage(formatShopItem("精灵球", "捕捉宝可梦", 200, playerPurchases));
        handler.sendMessage(formatShopItem("经验糖果", "增加100经验", 300, playerPurchases));
        handler.sendMessage(formatShopItem("攻击强化剂", "提升攻击力", 400, playerPurchases));
        handler.sendMessage(formatShopItem("防御强化剂", "提升防御力", 400, playerPurchases));

        handler.sendMessage("\n使用 'buy [物品名] [数量]' 来购买。余额: " + handler.getPlayer().getMoney());
    }

    public void buyShopItem(String itemName, int quantity) {
        if (quantity <= 0) {
            handler.sendMessage("购买数量必须大于0。");
            return;
        }
        ensureDailyUpdates();

        int price = getShopPrice(itemName);
        if (price == 0) {
            handler.sendMessage("店员：没有这种商品哦。");
            return;
        }

        if (outOfStockItems.contains(itemName)) {
            handler.sendMessage("店员：不好意思，" + itemName + " 目前缺货。");
            return;
        }

        if (limitedItems.contains(itemName)) {
            Map<String, Integer> playerPurchases = dailyPurchaseCounts.computeIfAbsent(handler.getPlayer().getName(), k -> new HashMap<>());
            int purchased = playerPurchases.getOrDefault(itemName, 0);
            if (purchased + quantity > MAX_DAILY_PURCHASE) {
                int remaining = MAX_DAILY_PURCHASE - purchased;
                handler.sendMessage("店员：该物品每日限购" + MAX_DAILY_PURCHASE + "个，今日剩余：" + remaining);
                return;
            }
        }

        int totalPrice = price * quantity;
        if (handler.getPlayer().getMoney() < totalPrice) {
            handler.sendMessage("金钱不足！需要" + totalPrice + "元，但你只有" + handler.getPlayer().getMoney() + "元。");
            return;
        }

        handler.getPlayer().declineMoney(totalPrice);
        handler.getPlayer().addItem(itemName, quantity);

        if (limitedItems.contains(itemName)) {
            Map<String, Integer> playerPurchases = dailyPurchaseCounts.computeIfAbsent(handler.getPlayer().getName(), k -> new HashMap<>());
            playerPurchases.put(itemName, playerPurchases.getOrDefault(itemName, 0) + quantity);
        }

        handler.sendMessage("购买了 " + itemName + " x" + quantity + "！花费" + totalPrice + "元");
        Player.savePlayer(handler.getPlayer());
    }

    private void handleStall() {
        if (!isAtMarket()) return;
        if (playerStalls.containsKey(handler.getPlayer().getName())) {
            handler.sendMessage("你已经创建了摊位。");
            return;
        }
        playerStalls.put(handler.getPlayer().getName(), System.currentTimeMillis());
        handler.sendMessage("你创建了一个临时摊位（持续24小时）！");
        saveMarket();
    }

    private void handleSell(String itemName, int quantity, int price) {
        if (!isAtMarket()) return;
        if (quantity <= 0 || price <= 0) { handler.sendMessage("数量和价格必须大于0。"); return; }
        if (!playerStalls.containsKey(handler.getPlayer().getName())) {
            handler.sendMessage("请先输入 market stall 创建摊位");
            return;
        }
        if (!handler.getPlayer().removeItem(itemName, quantity)) {
            handler.sendMessage("背包物品不足。");
            return;
        }

        int minPrice = (int)(getShopPrice(itemName) * 0.5);
        if (minPrice > 0 && price < minPrice) {
            handler.sendMessage("最低售价为 " + minPrice + " 元。");
            handler.getPlayer().addItem(itemName, quantity);
            return;
        }

        marketItems.add(new MarketItem(handler.getPlayer().getName(), itemName, quantity, price));
        handler.sendMessage("上架成功！");
        Player.savePlayer(handler.getPlayer());
        saveMarket();
    }

    private void handleMarketBuy(String itemName, int quantity, int price) {
        if (!isAtMarket()) return;
        if (quantity <= 0) return;

        List<MarketItem> matching = new ArrayList<>();
        int available = 0;
        for (MarketItem it : marketItems) {
            if (it.itemName.equals(itemName) && it.price == price && !it.sellerName.equals(handler.getPlayer().getName())) {
                matching.add(it);
                available += it.quantity;
            }
        }

        if (available < quantity) {
            handler.sendMessage("库存不足。");
            return;
        }

        int totalPrice = price * quantity;
        if (handler.getPlayer().getMoney() < totalPrice) {
            handler.sendMessage("金钱不足。");
            return;
        }

        int remaining = quantity;
        Iterator<MarketItem> it = matching.iterator();
        while (remaining > 0 && it.hasNext()) {
            MarketItem mi = it.next();
            int take = Math.min(remaining, mi.quantity);
            handler.getPlayer().addItem(itemName, take);

            int earn = take * price;
            ClientHandler sellerHandler = ClientHandler.onlinePlayers.get(mi.sellerName);
            if (sellerHandler != null && sellerHandler.getPlayer() != null) {
                sellerHandler.getPlayer().addMoney(earn);
                sellerHandler.sendMessage("你的 " + itemName + " x" + take + " 已被购买，获得 " + earn + " 元");
                Player.savePlayer(sellerHandler.getPlayer());
            } else {
                Player seller = Player.loadPlayer(mi.sellerName);
                if (seller != null) {
                    seller.addMoney(earn);
                    Player.savePlayer(seller);
                }
            }

            mi.quantity -= take;
            if (mi.quantity <= 0) marketItems.remove(mi);
            remaining -= take;
        }

        handler.getPlayer().declineMoney(totalPrice);
        handler.sendMessage("购买成功！");
        Player.savePlayer(handler.getPlayer());
        saveMarket();
    }

    private void handleList() {
        cleanupExpiredItems();
        if (marketItems.isEmpty()) { handler.sendMessage("市场空空如也。"); return; }
        handler.sendMessage("=== 商品列表 ===");
        for (int i = 0; i < marketItems.size(); i++) {
            MarketItem item = marketItems.get(i);
            handler.sendMessage((i + 1) + ". " + item.itemName + " x" + item.quantity + " 价格:" + item.price + " 卖家:" + item.sellerName);
        }
    }

    private boolean isAtMarket() {
        if (handler.getCurrentRoom() == null || !"market".equals(handler.getCurrentRoom().getId())) {
            handler.sendMessage("只有在交易市场才能进行此操作！");
            return false;
        }
        return true;
    }

    private void cleanupExpiredItems() {
        long now = System.currentTimeMillis();
        marketItems.removeIf(item -> item.expireTime < now);
        playerStalls.entrySet().removeIf(entry -> entry.getValue() + 24L * 60 * 60 * 1000 < now);
        saveMarket();
    }

    private int getShopPrice(String itemName) {
        switch (itemName) {
            case "伤药": return 200;
            case "好伤药": return 500;
            case "精灵球": return 200;
            case "经验糖果": return 300;
            case "攻击强化剂": return 400;
            case "防御强化剂": return 400;
            default: return 0;
        }
    }

    private String formatShopItem(String name, String description, int price, Map<String, Integer> purchases) {
        String status = "";
        if (outOfStockItems.contains(name)) status = " [缺货]";
        else if (limitedItems.contains(name)) {
            int remaining = MAX_DAILY_PURCHASE - purchases.getOrDefault(name, 0);
            status = " [今日剩余: " + remaining + "]";
        }
        return String.format("%-15s | 价格: %4d元%s", name + " - " + description, price, status);
    }

    private static void ensureDailyUpdates() {
        LocalDate today = LocalDate.now();
        if (!today.equals(lastOutOfStockRefreshDate)) {
            outOfStockItems.clear();
            List<String> allItems = Arrays.asList("伤药", "好伤药", "精灵球", "经验糖果", "攻击强化剂", "防御强化剂");
            for (String item : allItems) {
                if (Math.random() < 0.05) outOfStockItems.add(item);
            }
            lastOutOfStockRefreshDate = today;
        }
        if (!today.equals(lastPurchaseResetDate)) {
            dailyPurchaseCounts.clear();
            lastPurchaseResetDate = today;
        }
    }

    public static void saveMarket() {
        try {
            File dir = new File("saves");
            if (!dir.exists()) dir.mkdirs();
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("saves/market_items.ser"));
            out.writeObject(marketItems); out.close();
            out = new ObjectOutputStream(new FileOutputStream("saves/player_stalls.ser"));
            out.writeObject(playerStalls); out.close();
        } catch (IOException e) {}
    }

    @SuppressWarnings("unchecked")
    public static void loadMarket() {
        try {
            File file = new File("saves/market_items.ser");
            if (file.exists()) {
                ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
                marketItems.addAll((List<MarketItem>) in.readObject()); in.close();
            }
            file = new File("saves/player_stalls.ser");
            if (file.exists()) {
                ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
                playerStalls.putAll((Map<String, Long>) in.readObject()); in.close();
            }
        } catch (Exception e) {}
    }
}