package pokemon;

import java.util.*;
import java.io.*;

public class Player implements Serializable {
    private static final long serialVersionUID = 2L;

    private String name;
    private List<PocketMon> team;
    private Map<String, Integer> bag;
    private int money;
    private String currentMap;
    private transient PrintWriter out;

    public Player(String name) {
        this.name = name;
        this.team = new ArrayList<>();
        this.bag = new HashMap<>();
        this.money = 1000;
        this.currentMap = "home";
        initializePlayer();
    }

    private void initializePlayer() {
        bag.put("伤药", 2);
        bag.put("好伤药", 1);
        bag.put("精灵球", 5);
        bag.put("经验糖果", 1);
        bag.put("攻击强化剂", 1);
        bag.put("防御强化剂", 1);
    }

    public void setOut(PrintWriter out) {
        this.out = out;
    }

    public void sendMessage(String msg) {
        if (out != null) {
            out.println(msg);
        } else {
            System.out.println(msg);
        }
    }

    public String getStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 训练家 " + name + " ===\n");
        sb.append("当前位置: " + currentMap + "\n");
        sb.append("金钱: " + money + "元\n");
        sb.append("队伍:\n");
        if (team.isEmpty()) {
            sb.append(" (暂无宝可梦)\n");
        } else {
            for (int i = 0; i < team.size(); i++) {
                PocketMon pm = team.get(i);
                sb.append((i + 1) + ". " + pm.getBattleStatus() + "\n");
                sb.append("   " + pm.getExpInfo() + "\n");
            }
        }
        return sb.toString();
    }

    public String getBagContent() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 背包 ===\n");
        if (bag.isEmpty()) {
            sb.append("背包是空的。\n");
        } else {
            int totalItems = 0;
            for (Map.Entry<String, Integer> entry : bag.entrySet()) {
                sb.append(entry.getKey() + " x" + entry.getValue() + "\n");
                totalItems += entry.getValue();
            }
            sb.append("\n总计: " + totalItems + "个道具\n");
        }
        sb.append("金钱: " + money + "元");
        return sb.toString();
    }

    public void useItem(String itemName) {
        if (!bag.containsKey(itemName) || bag.get(itemName) <= 0) {
            sendMessage("你没有" + itemName + "。");
            return;
        }

        if (team.isEmpty()) {
            sendMessage("你没有可用的宝可梦！");
            return;
        }

        PocketMon targetPokemon = team.get(0);
        String resultMsg = "";
        boolean isUsed = false;

        switch (itemName) {
            case "伤药":
                if (targetPokemon.getCurrentHp() == targetPokemon.getMaxHp()) {
                    sendMessage("HP已经是满的了！");
                    return;
                }
                targetPokemon.heal(20);
                resultMsg = "使用了伤药！" + targetPokemon.getName() + "恢复了20HP";
                isUsed = true;
                break;
            case "好伤药":
                if (targetPokemon.getCurrentHp() == targetPokemon.getMaxHp()) {
                    sendMessage("HP已经是满的了！");
                    return;
                }
                targetPokemon.heal(50);
                resultMsg = "使用了好伤药！" + targetPokemon.getName() + "恢复了50HP";
                isUsed = true;
                break;
            case "经验糖果":
                targetPokemon.gainExp(100);
                resultMsg = "使用了经验糖果！获得了100经验值";
                isUsed = true;
                break;
            case "攻击强化剂":
                resultMsg = "使用了攻击强化剂！(需要在战斗中使用才有效)";
                isUsed = true;
                break;
            default:
                sendMessage("无法在非战斗状态使用 " + itemName);
                return;
        }

        if (isUsed) {
            bag.put(itemName, bag.get(itemName) - 1);
            if (bag.get(itemName) <= 0) bag.remove(itemName);

            sendMessage("--------------------------------");
            sendMessage(resultMsg);
            sendMessage("--------------------------------");
        }
    }

    public void healTeam() {
        for (PocketMon pm : team) pm.fullHeal();
        sendMessage("所有宝可梦已完全恢复！");
    }

    public void buyItem(String itemName, int price) {
        if (money < price) {
            sendMessage("金钱不足！需要" + price + "元，但你只有" + money + "元。");
            return;
        }
        money -= price;
        bag.put(itemName, bag.getOrDefault(itemName, 0) + 1);
        sendMessage("购买了 " + itemName + "！花费" + price + "元");
        sendMessage("剩余金钱: " + money + "元");
    }

    public void work() {
        int earnings = 200 + (int)(Math.random() * 100);
        money += earnings;
        sendMessage("打工完成！赚取了" + earnings + "元");
        sendMessage("当前金钱: " + money + "元");
    }

    public String getName() { return name; }
    public List<PocketMon> getTeam() { return team; }
    public int getMoney() { return money; }
    public Map<String, Integer> getBag() { return bag; }

    public String getCurrentMap() { return currentMap; }
    public void setCurrentMap(String map) { this.currentMap = map; }

    public void setStarterPokemon(PocketMon starter) {
        this.team.add(starter);
    }

    public PocketMon getFirstPokemon() {
        return team.isEmpty() ? null : team.get(0);
    }

    public void addMoney(int amount) {
        this.money += amount;
    }

    public void declineMoney(int amount) {
        if(this.money < amount) {
            this.money = 0;
        } else {
            this.money -= amount;
        }
    }

    public static boolean savePlayer(Player player) {
        try {
            File dir = new File("saves");
            if (!dir.exists()) dir.mkdirs();

            FileOutputStream fileOut = new FileOutputStream("saves/" + player.getName() + ".ser");
            ObjectOutputStream out = new ObjectOutputStream(fileOut);

            out.writeObject(player);

            out.close();
            fileOut.close();
            return true; // 返回成功
        } catch (IOException e) {
            e.printStackTrace();
            return false; // 返回失败
        }
    }

    public static Player loadPlayer(String name) {
        try {
            File f = new File("saves/" + name + ".ser");
            if (!f.exists()) {
                return new Player(name);
            }

            FileInputStream fileIn = new FileInputStream(f);
            ObjectInputStream in = new ObjectInputStream(fileIn);

            Player p = (Player) in.readObject();

            if (p.getCurrentMap() == null) {
                p.setCurrentMap("home");
            }

            in.close();
            fileIn.close();
            return p;
        } catch (Exception e) {
            return new Player(name);
        }
    }
}