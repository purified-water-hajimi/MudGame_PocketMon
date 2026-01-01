package pokemon;

import java.util.List;

public class PvPBattle {
    private ClientHandler p1; // 挑战者
    private ClientHandler p2; // 被挑战者
    private ClientHandler currentTurn; // 当前是谁的回合

    public PvPBattle(ClientHandler p1, ClientHandler p2) {
        this.p1 = p1;
        this.p2 = p2;
        this.currentTurn = p1; // 挑战者先手
    }

    public void start() {
        broadcast("\n⚔️ === PK 开始！=== ⚔️");
        broadcast(p1.getPlayer().getName() + " VS " + p2.getPlayer().getName());

        showStatus(p1);
        showStatus(p2);

        promptTurn();
    }

    public void handleInput(ClientHandler sender, String input) {
        if (sender != currentTurn) {
            sender.sendMessage("🚫 还没轮到你！请等待对手行动...");
            return;
        }

        if (input.equalsIgnoreCase("run") || input.equals("逃跑")) {
            broadcast("🏳️ " + sender.getPlayer().getName() + " 认输逃跑了！");
            endBattle(sender == p1 ? p2 : p1);
            return;
        }

        try {
            int skillIndex = Integer.parseInt(input) - 1;
            PocketMon myPoke = sender.getPlayer().getFirstPokemon();
            List<Skill> skills = myPoke.getSkills();

            if (skillIndex >= 0 && skillIndex < skills.size()) {
                Skill chosenSkill = skills.get(skillIndex);
                ClientHandler target = (sender == p1) ? p2 : p1;
                performAttack(sender, target, chosenSkill);
            } else {
                sender.sendMessage("技能编号错误！请输入 1~" + skills.size());
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("请输入技能编号 (例如: 1) 或输入 run 认输。");
        }
    }

    private void performAttack(ClientHandler attacker, ClientHandler defender, Skill skill) {
        PocketMon myPoke = attacker.getPlayer().getFirstPokemon();
        PocketMon enemyPoke = defender.getPlayer().getFirstPokemon();

        if (myPoke == null || enemyPoke == null) {
            endBattle(null);
            return;
        }

        // 1. 获取属性克制倍率
        double multiplier = getTypeMultiplier(skill.getType(), enemyPoke.getType());

        // 2. 基础伤害公式: (技能威力 + 攻击力) - 对手防御
        int baseDamage = (skill.getPower() + myPoke.getAttack()) - enemyPoke.getDefense();
        if (baseDamage < 1) baseDamage = 1; // 保底伤害

        // 3. 计算最终伤害
        int finalDamage = (int) (baseDamage * multiplier);

        // 4. 扣血
        enemyPoke.takeDamage(finalDamage);

        // 5. 构建战斗信息
        String effectMsg = "";
        if (multiplier > 1.0) effectMsg = " (效果拔群!)";
        else if (multiplier < 1.0 && multiplier > 0) effectMsg = " (效果微弱...)";

        broadcast("\n⚡ " + attacker.getPlayer().getName() + " 的 " + myPoke.getName() +
                " 使用了 [" + skill.getName() + "] !");

        if (!effectMsg.isEmpty()) broadcast(effectMsg); // 发送克制提示

        broadcast("💥 对 " + enemyPoke.getName() + " 造成了 " + finalDamage + " 点伤害！");

        // 6. 结算或下一回合
        if (enemyPoke.isFainted()) {
            broadcast("\n🏆 " + defender.getPlayer().getName() + " 的 " + enemyPoke.getName() + " 倒下了！");
            endBattle(attacker);
        } else {
            currentTurn = defender; // 交换回合

            // 显示血条
            broadcast("--------------------------------");
            broadcast(myPoke.getName() + ": " + myPoke.getHp() + "/" + myPoke.getMaxHp() + " HP");
            broadcast(enemyPoke.getName() + ": " + enemyPoke.getHp() + "/" + enemyPoke.getMaxHp() + " HP");
            broadcast("--------------------------------");

            promptTurn();
        }
    }

    // === 🔥 核心：属性克制表 ===
    // 返回值: 2.0=克制, 0.5=被克, 1.0=正常
    private double getTypeMultiplier(PocketMon.Type skillType, PocketMon.Type defType) {
        switch (skillType) {
            case FIRE: // 火系
                if (defType == PocketMon.Type.GRASS || defType == PocketMon.Type.BUG) return 2.0;
                if (defType == PocketMon.Type.WATER || defType == PocketMon.Type.FIRE) return 0.5;
                break;
            case WATER: // 水系
                if (defType == PocketMon.Type.FIRE) return 2.0;
                if (defType == PocketMon.Type.WATER || defType == PocketMon.Type.GRASS) return 0.5;
                break;
            case GRASS: // 草系
                if (defType == PocketMon.Type.WATER) return 2.0;
                if (defType == PocketMon.Type.FIRE || defType == PocketMon.Type.GRASS ||
                        defType == PocketMon.Type.FLYING || defType == PocketMon.Type.BUG) return 0.5;
                break;
            case ELECTRIC: // 电系
                if (defType == PocketMon.Type.WATER || defType == PocketMon.Type.FLYING) return 2.0;
                if (defType == PocketMon.Type.GRASS || defType == PocketMon.Type.ELECTRIC) return 0.5;
                break;
            case FLYING: // 飞行系
                if (defType == PocketMon.Type.GRASS || defType == PocketMon.Type.BUG) return 2.0;
                if (defType == PocketMon.Type.ELECTRIC) return 0.5;
                break;
            case BUG: // 虫系
                if (defType == PocketMon.Type.GRASS) return 2.0;
                if (defType == PocketMon.Type.FIRE || defType == PocketMon.Type.FLYING) return 0.5;
                break;
            default: // 普通系等
                break;
        }
        return 1.0; // 默认 1 倍
    }

    private void promptTurn() {
        ClientHandler activePlayer = currentTurn;
        ClientHandler waitingPlayer = (currentTurn == p1) ? p2 : p1;

        waitingPlayer.sendMessage("⏳ 等待 " + activePlayer.getPlayer().getName() + " 行动...");

        activePlayer.sendMessage("\n👉 轮到你了！请选择技能 (输入数字):");
        List<Skill> skills = activePlayer.getPlayer().getFirstPokemon().getSkills();

        for (int i = 0; i < skills.size(); i++) {
            Skill s = skills.get(i);
            // 这里显示技能属性，方便玩家判断克制
            activePlayer.sendMessage((i + 1) + ". " + s.getName() +
                    " [威力:" + s.getPower() + " | " + s.getType() + "]");
        }
        activePlayer.sendMessage("输入 'run' 认输");
    }

    private void endBattle(ClientHandler winner) {
        if (winner != null) {
            broadcast("\n🎉 胜者是: " + winner.getPlayer().getName() + "！");
            winner.getPlayer().addMoney(200);
            winner.sendMessage("你获得了 200元 奖金！");

            ClientHandler loser = (winner == p1) ? p2 : p1;
            loser.getPlayer().deductMoney(200);
            loser.sendMessage("遗憾！你输了，扣除200元作为惩罚");
        } else {
            broadcast("\n🤝 战斗异常结束。");
        }
        p1.endPvP();
        p2.endPvP();
    }

    private void showStatus(ClientHandler handler) {
        PocketMon pm = handler.getPlayer().getFirstPokemon();
        if (pm != null) {
            broadcast(handler.getPlayer().getName() + " 派出了 Lv." + pm.getLevel() + " " + pm.getName());
        }
    }

    private void broadcast(String msg) {
        p1.sendMessage(msg);
        p2.sendMessage(msg);
    }

}