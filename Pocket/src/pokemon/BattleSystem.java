package pokemon;

import java.io.*;
import java.util.*;

public class BattleSystem {
    private Player player;
    private PocketMon wildPokemon;

    private PrintWriter out;
    private BufferedReader in;

    private boolean battleActive;

    // 战斗平衡性参数
    private static final double BASE_DODGE_CHANCE = 0.10; // 降低一点闪避率，不然打得太慢
    private static final double BASE_CRITICAL_CHANCE = 0.12;
    private static final double CRITICAL_MULTIPLIER = 1.5; // 暴击伤害从 1.25 提升到 1.5
    private static final double ENEMY_HEAL_CHANCE = 0.15;

    public BattleSystem(Player player, PocketMon wildPokemon, PrintWriter out, BufferedReader in) {
        this.player = player;
        this.wildPokemon = wildPokemon;
        this.out = out;
        this.in = in;
        this.battleActive = true;
    }

    private void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void send(String msg) {
        out.println(msg);
    }

    public void startBattle() {
        send("\n=================================");
        send(" 野生宝可梦出现了！");
        send("=================================");
        sleep(800);
        send("野生的 " + wildPokemon.getBattleStatus() + " 冲了过来！");
        sleep(800);

        PocketMon playerPokemon = player.getFirstPokemon();
        // 检查首位精灵是否存活，如果晕了就找下一个活着的
        if (playerPokemon == null || playerPokemon.isFainted()) {
            playerPokemon = null;
            for(PocketMon pm : player.getTeam()) {
                if(!pm.isFainted()) {
                    playerPokemon = pm;
                    break;
                }
            }
        }

        if (playerPokemon == null) {
            send("你没有可用于战斗的宝可梦！只能逃跑了！");
            return;
        }

        send("去吧！ " + playerPokemon.getName() + "！");
        sleep(800);

        while (battleActive && !playerPokemon.isFainted() && !wildPokemon.isFainted()) {
            showBattleStatus(playerPokemon);

            try {
                playerTurn(playerPokemon);
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }

            if (!battleActive) break;
            if (wildPokemon.isFainted()) break;

            if (!playerPokemon.isFainted()) {
                enemyTurn(playerPokemon);
            }
        }

        if (wildPokemon.isFainted()) {
            battleWin(playerPokemon);
        } else if (playerPokemon.isFainted()) {
            battleLose(playerPokemon);
        }
    }

    private void playerTurn(PocketMon playerPokemon) throws IOException {
        send("\n--- 你的回合 (" + playerPokemon.getName() + ") ---");
        send("1. 攻击 (Attack)");
        send("2. 物品 (Bag)");
        send("3. 逃跑 (Run)");
        send("请选择行动 [1-3]: ");

        String choice = in.readLine();
        if (choice == null) return;
        choice = choice.trim();

        switch (choice) {
            case "1":
                attackMenu(playerPokemon);
                break;
            case "2":
                useItemMenu(playerPokemon);
                break;
            case "3":
                if (attemptEscape()) {
                    send("你成功从战斗中逃脱了！");
                    battleActive = false;
                    return;
                } else {
                    send("逃跑失败！被野生宝可梦拦住了！");
                    sleep(800);
                }
                break;
            default:
                send("无效的指令，请重新选择。");
                playerTurn(playerPokemon);
                break;
        }
    }

    private void attackMenu(PocketMon playerPokemon) throws IOException {
        send("\n--- 选择技能 ---");
        List<Skill> skills = playerPokemon.getSkills();

        if (skills.isEmpty()) {
            send("该宝可梦没有学会任何技能！使用了默认的【挣扎】！");
            executeBasicAttack(playerPokemon, wildPokemon, playerPokemon.getName());
            return;
        }

        for (int i = 0; i < skills.size(); i++) {
            Skill skill = skills.get(i);
            send((i + 1) + ". " + skill.getName() + " [PP:" + skill.getPp() + "/" + skill.getMaxPp() + "] (威力:" + skill.getPower() + ")");
        }
        send("0. 返回上一级");

        try {
            String input = in.readLine();
            if (input == null) return;

            int choice = Integer.parseInt(input.trim());
            if (choice == 0) {
                playerTurn(playerPokemon); // 返回上一级
                return;
            }

            int skillIndex = choice - 1;
            if (skillIndex >= 0 && skillIndex < skills.size()) {
                Skill skill = skills.get(skillIndex);
                if (skill.use()) {
                    executeAttack(playerPokemon, skill, wildPokemon, playerPokemon.getName());
                } else {
                    send(">> " + skill.getName() + " 的PP不足，无法使用！");
                    attackMenu(playerPokemon);
                }
            } else {
                send("无效的选择。");
                attackMenu(playerPokemon);
            }
        } catch (NumberFormatException e) {
            send("请输入数字！");
            attackMenu(playerPokemon);
        }
    }

    private void useItemMenu(PocketMon playerPokemon) throws IOException {
        send("\n--- 战斗道具 ---");

        Map<String, Integer> bag = player.getBag();
        boolean hasBattleItems = false;
        List<String> usableItems = new ArrayList<>();

        int index = 1;
        for (String itemName : bag.keySet()) {
            if (bag.get(itemName) > 0 && isBattleItem(itemName)) {
                send(index + ". " + itemName + " (持有: " + bag.get(itemName) + ")");
                usableItems.add(itemName);
                hasBattleItems = true;
                index++;
            }
        }

        if (!hasBattleItems) {
            send("背包里没有可以在战斗中使用的道具！");
            sleep(600);
            playerTurn(playerPokemon);
            return;
        }
        send("0. 返回");

        try {
            String input = in.readLine();
            if (input == null) return;
            int choice = Integer.parseInt(input.trim());

            if (choice == 0) {
                playerTurn(playerPokemon);
                return;
            }

            if (choice > 0 && choice <= usableItems.size()) {
                String itemName = usableItems.get(choice - 1);
                boolean success = useBattleItem(itemName, playerPokemon);
                // 只有成功使用了道具，回合才结束；否则重新选
                if (!success) {
                    useItemMenu(playerPokemon);
                }
            } else {
                send("无效选择");
                useItemMenu(playerPokemon);
            }
        } catch (Exception e) {
            send("请输入正确的数字。");
            useItemMenu(playerPokemon);
        }
    }

    private boolean isBattleItem(String itemName) {
        return itemName.equals("伤药") || itemName.equals("好伤药") ||
                itemName.equals("攻击强化剂") || itemName.equals("防御强化剂") ||
                itemName.equals("精灵球"); // 预留精灵球逻辑
    }

    private boolean useBattleItem(String itemName, PocketMon targetPokemon) {
        boolean used = false;

        switch (itemName) {
            case "伤药":
                if (targetPokemon.getCurrentHp() == targetPokemon.getMaxHp()) {
                    send("HP已经是满的了！");
                    return false;
                }
                targetPokemon.heal(20);
                send(">> 使用了【伤药】，恢复了 20 HP！");
                used = true;
                break;
            case "好伤药":
                if (targetPokemon.getCurrentHp() == targetPokemon.getMaxHp()) {
                    send("HP已经是满的了！");
                    return false;
                }
                targetPokemon.heal(50);
                send(">> 使用了【好伤药】，恢复了 50 HP！");
                used = true;
                break;
            case "攻击强化剂":
                targetPokemon.boostAttack(5);
                send(">> 使用了【攻击强化剂】，攻击力暂时提升了！");
                used = true;
                break;
            case "防御强化剂":
                targetPokemon.boostDefense(5);
                send(">> 使用了【防御强化剂】，防御力暂时提升了！");
                used = true;
                break;
            default:
                send("暂时无法使用此道具。");
                return false;
        }

        if (used) {
            consumeFromBag(itemName);
            return true;
        }
        return false;
    }

    private void consumeFromBag(String itemName) {
        Map<String, Integer> bag = player.getBag();
        if (bag.containsKey(itemName)) {
            int current = bag.get(itemName);
            if (current <= 1) {
                bag.remove(itemName);
            } else {
                bag.put(itemName, current - 1);
            }
        }
    }

    private void enemyTurn(PocketMon playerPokemon) {
        send("\n--- 对手回合 (" + wildPokemon.getName() + ") ---");
        sleep(600);

        // 简单的AI：血量低时概率回血
        if (Math.random() < ENEMY_HEAL_CHANCE && wildPokemon.getCurrentHp() < wildPokemon.getMaxHp() / 3) {
            send("野生 " + wildPokemon.getName() + " 似乎在休息...");
            sleep(600);
            int healAmount = wildPokemon.getMaxHp() / 4;
            wildPokemon.heal(healAmount);
            send("野生 " + wildPokemon.getName() + " 恢复了 " + healAmount + " HP！");
            sleep(600);
            return;
        }

        Skill enemySkill = selectEnemySkill();
        if (enemySkill != null) {
            send("野生 " + wildPokemon.getName() + " 使用了 " + enemySkill.getName() + "！");
            enemySkill.use(); // 扣除PP
            sleep(500);
            executeAttack(wildPokemon, enemySkill, playerPokemon, "野生 " + wildPokemon.getName());
        } else {
            send("野生 " + wildPokemon.getName() + " 使用了 猛撞！");
            sleep(500);
            executeBasicAttack(wildPokemon, playerPokemon, "野生 " + wildPokemon.getName());
        }
    }

    private Skill selectEnemySkill() {
        if (wildPokemon.getSkills().isEmpty()) {
            return null;
        }
        int skillIndex = (int) (Math.random() * wildPokemon.getSkills().size());
        return wildPokemon.getSkill(skillIndex);
    }

    private void executeAttack(PocketMon attacker, Skill skill, PocketMon defender, String attackerName) {
        if (checkDodge(defender.getName())) {
            return;
        }

        if (!skill.checkHit()) {
            send(attackerName + " 的攻击没有命中！");
            sleep(600);
            return;
        }

        int damage = calculateDamage(attacker, skill, defender);
        defender.takeDamage(damage);
        send(">> 造成了 " + damage + " 点伤害！");


        sleep(700);
    }

    private void executeBasicAttack(PocketMon attacker, PocketMon defender, String attackerName) {
        if (checkDodge(defender.getName())) {
            return;
        }

        int baseDamage = Math.max(1, attacker.getAttack() - (defender.getDefense() / 3));
        int damage = applyCriticalHit(baseDamage);

        defender.takeDamage(damage);
        send(">> 造成了 " + damage + " 点伤害！");
        sleep(700);
    }

    private boolean checkDodge(String defenderName) {
        if (Math.random() < BASE_DODGE_CHANCE) {
            send(defenderName + " 灵巧地闪避了攻击！");
            sleep(600);
            return true;
        }
        return false;
    }

    private int calculateDamage(PocketMon attacker, Skill skill, PocketMon defender) {
        if (skill.getPower() == 0) return 0;

        double rawDamage = (double)attacker.getAttack() * skill.getPower() / Math.max(1, defender.getDefense());
        rawDamage = rawDamage * 0.5 + 2;

        int finalDamage = (int) rawDamage;
        return applyCriticalHit(finalDamage);
    }

    private int applyCriticalHit(int baseDamage) {
        if (Math.random() < BASE_CRITICAL_CHANCE) {
            send("  *** 会心一击！(Critical Hit) ***");
            sleep(400);
            return (int) Math.max(1, Math.round(baseDamage * CRITICAL_MULTIPLIER));
        }
        return baseDamage;
    }

    private boolean attemptEscape() {
        return Math.random() > 0.4;
    }

    private void battleWin(PocketMon playerPokemon) {
        send("\n============================");
        send(" 胜 利！");
        send("============================");
        send("野生 " + wildPokemon.getName() + " 倒下了！");
        sleep(700);

        int expGain = wildPokemon.getLevel() * 15 + 10;
        playerPokemon.gainExp(expGain);
        send(">> " + playerPokemon.getName() + " 获得了 " + expGain + " 点经验值。");


        int expToNext = playerPokemon.getExpToNextLevel();
        if (expToNext > 0) {
            send("   (距离升级还需要 " + expToNext + " 点经验)");
        } else {
            send("   (即将升级！)");
        }
        sleep(600);

        int moneyGain = wildPokemon.getLevel() * 20;
        player.addMoney(moneyGain); // 使用 Player 类的新方法
        send(">> 你拾取了 " + moneyGain + " 元。");
        send("当前金钱: " + player.getMoney() + "元");

        sleep(1500);
        battleActive = false;
    }

    private void battleLose(PocketMon playerPokemon) {
        send("\n============================");
        send("  失 败 ... ");
        send("============================");
        send(playerPokemon.getName() + " 倒下了！");
        sleep(700);
        send("你眼前一黑...");
        sleep(1000);

        // 失败惩罚：扣钱并治疗
        int lostMoney = player.getMoney() / 2;
        player.addMoney(-lostMoney); // 扣除一半金钱
        player.healTeam(); // 自动送去治疗

        send("你慌忙逃到了安全的地方，并在精灵中心恢复了所有宝可梦。");
        send("在逃跑过程中不小心遗失了 " + lostMoney + " 元...");
        sleep(1500);
        battleActive = false;
    }

    private void showBattleStatus(PocketMon playerPokemon) {
        send("\n--------------------------------");
        send("【敌】 " + wildPokemon.getBattleStatus());
        send("       HP: " + wildPokemon.getCurrentHp() + "/" + wildPokemon.getMaxHp());
        send("");
        send("【我】 " + playerPokemon.getBattleStatus());
        send("       HP: " + playerPokemon.getCurrentHp() + "/" + playerPokemon.getMaxHp());
        send("--------------------------------");
    }
}