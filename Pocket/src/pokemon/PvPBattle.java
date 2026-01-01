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

    // 处理玩家输入的战斗指令
    public void handleInput(ClientHandler sender, String input) {
        // 1. 检查是不是轮到这个人
        if (sender != currentTurn) {
            sender.sendMessage("🚫 还没轮到你！请等待对手行动...");
            return;
        }

        // 2. 检查是不是认输
        if (input.equals("run") || input.equals("逃跑")) {
            broadcast("🏳️ " + sender.getPlayer().getName() + " 认输逃跑了！");
            endBattle(sender == p1 ? p2 : p1); // 对手获胜
            return;
        }

        // 3. 尝试解析技能选择 (输入 1, 2, 3...)
        try {
            int skillIndex = Integer.parseInt(input) - 1; // 玩家输入1代表下标0
            PocketMon myPoke = sender.getPlayer().getFirstPokemon();

            // 获取技能列表
            List<Skill> skills = myPoke.getSkills();

            // 检查输入是否有效
            if (skillIndex >= 0 && skillIndex < skills.size()) {
                Skill chosenSkill = skills.get(skillIndex);
                ClientHandler target = (sender == p1) ? p2 : p1;

                // 🔥 执行技能攻击！
                performAttack(sender, target, chosenSkill);
            } else {
                sender.sendMessage("技能编号错误！请输入 1~" + skills.size());
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("请输入技能编号 (例如: 1) 或输入 run 认输。");
        }
    }

    // 执行攻击 (带技能参数)
    private void performAttack(ClientHandler attacker, ClientHandler defender, Skill skill) {
        PocketMon myPoke = attacker.getPlayer().getFirstPokemon();
        PocketMon enemyPoke = defender.getPlayer().getFirstPokemon();

        if (myPoke == null || enemyPoke == null) {
            endBattle(null);
            return;
        }

        // === 🧮 伤害计算公式 ===
        // 伤害 = (技能威力 + 攻击力) - 对手防御力
        // (为了防止不破防，最低造成 1 点伤害)
        int damage = (skill.getPower() + myPoke.getAttack()) - enemyPoke.getDefense();
        if (damage < 1) damage = 1;

        // 属性克制逻辑可以在这里扩展 (暂时略过)

        // 扣血
        enemyPoke.takeDamage(damage);

        // 广播战斗信息
        broadcast("\n⚡ " + attacker.getPlayer().getName() + " 的 " + myPoke.getName() +
                " 使用了 [" + skill.getName() + "] !");
        broadcast("💥 对 " + enemyPoke.getName() + " 造成了 " + damage + " 点伤害！");

        // 检查是否击败对手
        if (enemyPoke.isFainted()) {
            broadcast("\n🏆 " + defender.getPlayer().getName() + " 的 " + enemyPoke.getName() + " 倒下了！");
            endBattle(attacker);
        } else {
            // 交换回合
            currentTurn = defender;

            // 显示血量条
            broadcast("--------------------------------");
            broadcast(myPoke.getName() + ": " + myPoke.getHp() + "/" + myPoke.getMaxHp() + " HP");
            broadcast(enemyPoke.getName() + ": " + enemyPoke.getHp() + "/" + enemyPoke.getMaxHp() + " HP");
            broadcast("--------------------------------");

            promptTurn();
        }
    }

    private void promptTurn() {
        // 告诉当前回合的玩家：该你了，选个技能吧！
        ClientHandler activePlayer = currentTurn;
        ClientHandler waitingPlayer = (currentTurn == p1) ? p2 : p1;

        waitingPlayer.sendMessage("⏳ 等待 " + activePlayer.getPlayer().getName() + " 行动...");

        activePlayer.sendMessage("\n👉 轮到你了！请选择技能 (输入数字):");
        PocketMon myPoke = activePlayer.getPlayer().getFirstPokemon();
        List<Skill> skills = myPoke.getSkills();

        // 列出所有技能
        for (int i = 0; i < skills.size(); i++) {
            Skill s = skills.get(i);
            activePlayer.sendMessage((i + 1) + ". " + s.getName() +
                    " (威力:" + s.getPower() + " 属性:" + s.getType() + ")");
        }
        activePlayer.sendMessage("输入 'run' 认输");
    }

    private void endBattle(ClientHandler winner) {
        if (winner != null) {
            broadcast("\n🎉 胜者是: " + winner.getPlayer().getName() + "！");
            winner.getPlayer().addMoney(200); // 赢了加钱
            winner.sendMessage("你获得了 200元 奖金！");
        } else {
            broadcast("\n🤝 战斗异常结束。");
        }

        // 解除双方的战斗状态
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