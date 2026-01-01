package pokemon;

import java.util.*;

public class Player {
    private String name;
    private List<PocketMon> team;
    private Map<String, Integer> bag;
    private int money;

    public Player(String name) {
        this.name = name;
        // 使用 Vector 确保线程安全
        this.team = new Vector<>();
        this.bag = new HashMap<>();
        this.money = 1000;
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

    // 获取状态 (返回字符串给 ClientHandler 打印)
    public String getStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 训练家 " + name + " ===\n");
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

    // 获取背包内容
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

    // 使用物品
    public String useItem(String itemName) {
        if (!bag.containsKey(itemName) || bag.get(itemName) <= 0) {
            return "你没有" + itemName + "。";
        }

        if (team.isEmpty()) {
            return "你没有可用的宝可梦！";
        }

        PocketMon targetPokemon = team.get(0);
        String resultMsg = "";

        switch (itemName) {
            case "伤药":
                targetPokemon.heal(20);
                bag.put(itemName, bag.get(itemName) - 1);
                resultMsg = "使用了伤药！恢复了20HP";
                break;
            case "好伤药":
                targetPokemon.heal(50);
                bag.put(itemName, bag.get(itemName) - 1);
                resultMsg = "使用了好伤药！恢复了50HP";
                break;
            case "经验糖果":
                targetPokemon.gainExp(100);
                bag.put(itemName, bag.get(itemName) - 1);
                resultMsg = "使用了经验糖果！获得了100经验值";
                break;
            case "攻击强化剂":
                // 战斗外使用没有实际效果，暂作提示
                resultMsg = "使用了攻击强化剂！(需要在战斗中使用才有效)";
                bag.put(itemName, bag.get(itemName) - 1);
                break;
            default:
                return "无法在非战斗状态使用 " + itemName;
        }

        if (bag.get(itemName) <= 0) bag.remove(itemName);
        return resultMsg;
    }

    public String healTeam() {
        for (PocketMon pm : team) pm.fullHeal();
        return "所有宝可梦已完全恢复！";
    }

    // 购买道具
    public String buyItem(String itemName, int price) {
        if (money < price) {
            return "金钱不足！需要" + price + "元，但你只有" + money + "元。";
        }
        money -= price;
        bag.put(itemName, bag.getOrDefault(itemName, 0) + 1);
        return "购买了 " + itemName + "！花费" + price + "元\n剩余金钱: " + money + "元";
    }

    // 打工
    public String work() {
        int earnings = 200 + (int)(Math.random() * 100);
        money += earnings;
        return "打工完成！赚取了" + earnings + "元\n当前金钱: " + money + "元";
    }

    public void gainMoney(int amount) {
        money += amount;
    }

    public String getName() { return name; }
    public List<PocketMon> getTeam() { return team; }
    public int getMoney() { return money; }
    public Map<String, Integer> getBag() { return bag; }

    public void setStarterPokemon(PocketMon starter) {
        this.team.add(starter);
    }

    public PocketMon getFirstPokemon() {
        return team.isEmpty() ? null : team.get(0);
    }
    public void addMoney(int amount) {
        this.money += amount;
    }
}