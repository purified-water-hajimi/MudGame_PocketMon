package pokemon;

import java.io.*;

public class BattleSystem {
    private Player player;
    private PocketMon wildPokemon;

    private PrintWriter out;
    private BufferedReader in;

    private boolean battleActive;

    private static final double BASE_DODGE_CHANCE = 0.15;
    private static final double BASE_CRITICAL_CHANCE = 0.10;

    private static final double CRITICAL_MULTIPLIER = 1.25;

    private static final double ENEMY_HEAL_CHANCE = 0.20;

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
        send("\n=== 野生宝可梦出现了！ ===");
        sleep(800);
        send("野生的 " + wildPokemon.getBattleStatus() + " 出现了！");
        sleep(800);

        PocketMon playerPokemon = player.getFirstPokemon();
        if (playerPokemon == null) {
            send("你没有可用的宝可梦！");
            return;
        }

        send("去吧！ " + playerPokemon.getBattleStatus());
        sleep(800);

        while (battleActive && !playerPokemon.isFainted() && !wildPokemon.isFainted()) {
            showBattleStatus(playerPokemon);

            try {
                playerTurn(playerPokemon);
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }

            if (!battleActive || playerPokemon.isFainted() || wildPokemon.isFainted()) break;

            // 对手回合
            enemyTurn(playerPokemon);
            if (playerPokemon.isFainted() || wildPokemon.isFainted()) break;
        }

        if (wildPokemon.isFainted()) {
            battleWin(playerPokemon);
        } else if (playerPokemon.isFainted()) {
            battleLose(playerPokemon);
        }
    }

    private void playerTurn(PocketMon playerPokemon) throws IOException {
        send("\n--- 你的回合 ---");
        send("选择行动:");
        send("1. 攻击");
        send("2. 使用道具");
        send("3. 逃跑");

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
                    send("成功逃跑了！");
                    battleActive = false;
                    return;
                } else {
                    send("逃跑失败了！");
                }
                break;
            default:
                send("无效选择");
                playerTurn(playerPokemon);
                break;
        }
    }

    private void attackMenu(PocketMon playerPokemon) throws IOException {
        send("\n选择技能:");
        for (int i = 0; i < playerPokemon.getSkills().size(); i++) {
            Skill skill = playerPokemon.getSkill(i);
            send((i + 1) + ". " + skill.getName() + " (PP:" + skill.getPp() + "/" + skill.getMaxPp() + ")");
        }

        try {
            String input = in.readLine();
            if (input == null) return;

            int choice = Integer.parseInt(input.trim()) - 1;
            if (choice >= 0 && choice < playerPokemon.getSkills().size()) {
                Skill skill = playerPokemon.getSkill(choice);
                if (skill.use()) {
                    executeAttack(playerPokemon, skill, wildPokemon, playerPokemon.getName());
                } else {
                    send(skill.getName() + "的PP不足！");
                    attackMenu(playerPokemon);
                }
            } else {
                send("无效选择");
                attackMenu(playerPokemon);
            }
        } catch (NumberFormatException e) {
            send("请输入数字！");
            attackMenu(playerPokemon);
        }
    }

    private void useItemMenu(PocketMon playerPokemon) throws IOException {
        send("\n=== 战斗道具 ===");

        boolean hasBattleItems = false;
        for (String itemName : player.getBag().keySet()) {
            int quantity = player.getBag().get(itemName);
            if (quantity > 0 && isBattleItem(itemName)) {
                send("- " + itemName + " x" + quantity);
                hasBattleItems = true;
            }
        }

        if (!hasBattleItems) {
            send("没有可用的战斗道具！");
            sleep(600);
            playerTurn(playerPokemon);
            return;
        }

        send("\n输入要使用的道具名（或输入 'back' 返回）: ");
        String itemName = in.readLine();
        if (itemName == null) return;
        itemName = itemName.trim();

        if (itemName.equalsIgnoreCase("back")) {
            playerTurn(playerPokemon);
            return;
        }

        if (player.getBag().containsKey(itemName) && player.getBag().get(itemName) > 0) {
            boolean used = useBattleItem(itemName, playerPokemon);
            if (!used) {
                sleep(600);
                useItemMenu(playerPokemon);
            }
        } else {
            send("你没有这个道具或道具已用完！");
            sleep(600);
            useItemMenu(playerPokemon);
        }
    }

    private boolean isBattleItem(String itemName) {
        return itemName.equals("伤药") || itemName.equals("好伤药") ||
                itemName.equals("攻击强化剂") || itemName.equals("防御强化剂");
    }

    private boolean useBattleItem(String itemName, PocketMon targetPokemon) {
        switch (itemName) {
            case "伤药":
                targetPokemon.heal(20);
                consumeFromBag(itemName);
                send("你使用了【伤药】，恢复 20 HP。");
                send(targetPokemon.getBattleStatus());
                return true;
            case "好伤药":
                targetPokemon.heal(50);
                consumeFromBag(itemName);
                send("你使用了【好伤药】，恢复 50 HP。");
                send(targetPokemon.getBattleStatus());
                return true;
            case "攻击强化剂":
                targetPokemon.boostAttack(5);
                consumeFromBag(itemName);
                send("你使用了【攻击强化剂】，攻击 +5。");
                return true;
            case "防御强化剂":
                targetPokemon.boostDefense(5);
                consumeFromBag(itemName);
                send("你使用了【防御强化剂】，防御 +5。");
                return true;
            default:
                send("这个道具不能在战斗中使用！");
                return false;
        }
    }

    private void consumeFromBag(String itemName) {
        int left = player.getBag().getOrDefault(itemName, 0) - 1;
        if (left <= 0) player.getBag().remove(itemName);
        else player.getBag().put(itemName, left);
    }

    private void enemyTurn(PocketMon playerPokemon) {
        send("\n--- 对手的回合 ---");
        sleep(600);

        if (Math.random() < ENEMY_HEAL_CHANCE && wildPokemon.getCurrentHp() < wildPokemon.getMaxHp() / 2) {
            send("野生 " + wildPokemon.getName() + " 使用了自我再生！");
            sleep(600);
            int healAmount = wildPokemon.getMaxHp() / 3;
            wildPokemon.heal(healAmount);
            send("野生 " + wildPokemon.getName() + " 恢复了 " + healAmount + " HP！");
            sleep(600);
            return;
        }

        Skill enemySkill = selectEnemySkill();
        if (enemySkill != null) {
            enemySkill.use();
            send("野生 " + wildPokemon.getName() + " 使用了 " + enemySkill.getName() + "！");
            sleep(600);
            executeAttack(wildPokemon, enemySkill, playerPokemon, "野生 " + wildPokemon.getName());
        } else {
            send("野生 " + wildPokemon.getName() + " 使用了 撞击！");
            sleep(600);
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
            send(attackerName + " 的技能没有命中！");
            sleep(600);
            return;
        }

        int damage = calculateDamage(attacker, skill, defender);
        if (damage > 0) {
            defender.takeDamage(damage);
            send(attackerName + " 对 " + defender.getName() + " 造成了 " + damage + " 点伤害！");
            sleep(700);
        }
    }

    private void executeBasicAttack(PocketMon attacker, PocketMon defender, String attackerName) {
        if (checkDodge(defender.getName())) {
            return;
        }

        int baseDamage = Math.max(1, attacker.getAttack() / 2);
        int damage = applyCriticalHit(baseDamage);

        defender.takeDamage(damage);
        send(attackerName + " 对 " + defender.getName() + " 造成了 " + damage + " 点伤害！");
        sleep(700);
    }

    private boolean checkDodge(String defenderName) {
        if (Math.random() < BASE_DODGE_CHANCE) {
            send(defenderName + " 成功闪避了攻击！");
            sleep(600);
            return true;
        }
        return false;
    }

    private int calculateDamage(PocketMon attacker, Skill skill, PocketMon defender) {
        if (skill.getPower() == 0) {
            return 0;
        }

        int baseDamage = skill.calculateDamage(attacker, defender);
        return applyCriticalHit(baseDamage);
    }

    private int applyCriticalHit(int baseDamage) {
        if (Math.random() < BASE_CRITICAL_CHANCE) {
            send("会心一击！");
            sleep(400);
            return (int) Math.max(1, Math.round(baseDamage * CRITICAL_MULTIPLIER));
        }
        return baseDamage;
    }

    private boolean attemptEscape() {
        return Math.random() > 0.3;
    }

    private void battleWin(PocketMon playerPokemon) {
        send("\n胜利！野生 " + wildPokemon.getName() + " 倒下了！");
        sleep(700);

        int expGain = wildPokemon.getLevel() * 10;
        playerPokemon.gainExp(expGain);
        send(playerPokemon.getName() + " 获得了 " + expGain + " 经验。");
        sleep(600);

        int moneyGain = wildPokemon.getLevel() * 5;
        player.gainMoney(moneyGain);
        send("你获得了 " + moneyGain + " 金币。");
        sleep(600);

        int expToNext = playerPokemon.getExpToNextLevel();
        if (expToNext > 0) {
            send(playerPokemon.getName() + " 距离下一级还需: " + expToNext + " 经验");
        } else {
            send(playerPokemon.getName() + " 可以升级了！");
        }
        sleep(1000);

        battleActive = false;
    }

    private void battleLose(PocketMon playerPokemon) {
        send("\n" + playerPokemon.getName() + " 倒下了！");
        sleep(700);
        send("你被打败了...");
        sleep(700);
        battleActive = false;
    }

    private void showBattleStatus(PocketMon playerPokemon) {
        send("\n====================");
        send("野生 " + wildPokemon.getBattleStatus());
        send("你的 " + playerPokemon.getBattleStatus());
        send("====================");
    }
}
