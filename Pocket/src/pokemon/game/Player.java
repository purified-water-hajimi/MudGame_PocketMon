package pokemon.game;

import java.util.*;

public class Player {
    private String name;
    private List<PocketMon> team;
    private Map<String, Integer> bag;
    private int money;

    public Player(String name) {
        this.name = name;
        this.team = new ArrayList<>();
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

    public void showStatus() {
        System.out.println("=== 训练家 " + name + " ===");
        System.out.println("金钱: " + money + "元");
        System.out.println("\n队伍:");
        for (int i = 0; i < team.size(); i++) {
            PocketMon pm = team.get(i);
            System.out.println((i + 1) + ". " + pm.getBattleStatus());
            System.out.println("   " + pm.getExpInfo());
        }
    }

    public void showBag() {
        System.out.println("=== 背包 ===");
        if (bag.isEmpty()) {
            System.out.println("背包是空的。");
        } else {
            int totalItems = 0;
            for (Map.Entry<String, Integer> entry : bag.entrySet()) {
                System.out.println(entry.getKey() + " x" + entry.getValue());
                totalItems += entry.getValue();
            }
            System.out.println("\n总计: " + totalItems + "个道具");
        }
        System.out.println("金钱: " + money + "元");
    }

    public void useItem(String itemName) {
        if (!bag.containsKey(itemName) || bag.get(itemName) <= 0) {
            System.out.println("你没有" + itemName + "。");
            return;
        }

        if (team.isEmpty()) {
            System.out.println("你没有可用的宝可梦！");
            return;
        }

        PocketMon targetPokemon = team.get(0);

        switch (itemName) {
            case "伤药":
                targetPokemon.heal(20);
                bag.put(itemName, bag.get(itemName) - 1);
                System.out.println("使用了伤药！恢复了20HP");
                break;
            case "好伤药":
                targetPokemon.heal(50);
                bag.put(itemName, bag.get(itemName) - 1);
                System.out.println("使用了好伤药！恢复了50HP");
                break;
            case "经验糖果":
                targetPokemon.gainExp(100);
                bag.put(itemName, bag.get(itemName) - 1);
                System.out.println("使用了经验糖果！获得了100经验值");
                break;
            case "攻击强化剂":
                System.out.println("使用了攻击强化剂！攻击力暂时提升");
                bag.put(itemName, bag.get(itemName) - 1);
                break;
            default:
                System.out.println("无法使用 " + itemName);
                return;
        }

        if (bag.get(itemName) <= 0) bag.remove(itemName);
    }

    public void healTeam() {
        for (PocketMon pm : team) pm.fullHeal();
        System.out.println("所有宝可梦已完全恢复！");
    }

    public boolean buyItem(String itemName, int price) {
        if (money < price) {
            System.out.println("金钱不足！需要" + price + "元，但你只有" + money + "元。");
            return false;
        }
        money -= price;
        bag.put(itemName, bag.getOrDefault(itemName, 0) + 1);
        System.out.println("购买了 " + itemName + "！花费" + price + "元");
        System.out.println("剩余金钱: " + money + "元");
        return true;
    }

    public void work() {
        int earnings = 200 + (int)(Math.random() * 100);
        money += earnings;
        System.out.println("打工完成！赚取了" + earnings + "元");
        System.out.println("当前金钱: " + money + "元");
    }

    public void gainMoney(int amount) {
        money += amount;
        System.out.println("获得了 " + amount + "元！");
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
}