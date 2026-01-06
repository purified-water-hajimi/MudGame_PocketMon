package pokemon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PvPBattle {
    private ClientHandler p1; // 挑战者
    private ClientHandler p2; // 被挑战者
    private ClientHandler currentTurn; // 当前是谁的回合

    public PvPBattle(ClientHandler p1, ClientHandler p2) {
        this.p1 = p1;
        this.p2 = p2;
        this.currentTurn = p1;
    }

    private enum TurnState {
        ACTION_SELECT,
        SKILL_SELECT,
        ITEM_SELECT
    }

    private TurnState currentState = TurnState.ACTION_SELECT;

    private List<String> tempItemList = new ArrayList<>();

    public void start() {
        broadcast("\n === PK 开始！=== ");
        broadcast(p1.getPlayer().getName() + " VS " + p2.getPlayer().getName());

        showStatus(p1);
        showStatus(p2);

        currentState = TurnState.ACTION_SELECT;
        promptTurn();
    }

    public void handleInput(ClientHandler sender, String input) {
        if (sender != currentTurn) {
            sender.sendMessage("还没轮到你！请等待对手行动...");
            return;
        }

        switch (currentState) {
            case ACTION_SELECT:
                handleActionSelect(sender, input);
                break;
            case SKILL_SELECT:
                handleSkillSelect(sender, input);
                break;
            case ITEM_SELECT:
                handleItemSelect(sender, input);
                break;
        }
    }

    private void handleActionSelect(ClientHandler sender, String input) {
        switch (input) {
            case "1":
                currentState = TurnState.SKILL_SELECT;
                promptSkillMenu(sender);
                break;
            case "2":
                currentState = TurnState.ITEM_SELECT;
                promptBagMenu(sender);
                break;
            case "3":
            case "run":
            case "逃跑":
                broadcast(sender.getPlayer().getName() + " 认输逃跑了！");
                endBattle(sender == p1 ? p2 : p1);
                break;
            default:
                sender.sendMessage("无效选择。请输入: 1(攻击), 2(物品), 3(逃跑)");
                promptTurn();
                break;
        }
    }

    private void handleSkillSelect(ClientHandler sender, String input) {
        if (input.equals("0")) {
            currentState = TurnState.ACTION_SELECT;
            promptTurn();
            return;
        }

        try {
            int skillIndex = Integer.parseInt(input) - 1;
            PocketMon myPoke = sender.getPlayer().getFirstPokemon();
            List<Skill> skills = myPoke.getSkills();

            if (skillIndex >= 0 && skillIndex < skills.size()) {
                Skill chosenSkill = skills.get(skillIndex);

                if (chosenSkill.getPp() <= 0) {
                    sender.sendMessage("该技能 PP 不足！");
                    return;
                }

                chosenSkill.use();
                ClientHandler target = (sender == p1) ? p2 : p1;
                performAttack(sender, target, chosenSkill);
            } else {
                sender.sendMessage("无效的技能编号。输入 0 返回。");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("请输入数字！");
        }
    }

    private void handleItemSelect(ClientHandler sender, String input) {
        if (input.equals("0")) {
            currentState = TurnState.ACTION_SELECT;
            promptTurn();
            return;
        }

        try {
            int index = Integer.parseInt(input) - 1;
            if (index >= 0 && index < tempItemList.size()) {
                String itemName = tempItemList.get(index);
                performItemUse(sender, itemName);
            } else {
                sender.sendMessage("无效的物品编号。输入 0 返回。");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("请输入数字！");
        }
    }

    private void performItemUse(ClientHandler user, String itemName) {
        Player p = user.getPlayer();
        PocketMon pm = p.getFirstPokemon();

        if (!p.getBag().containsKey(itemName) || p.getBag().get(itemName) <= 0) {
            user.sendMessage("道具数量不足！");
            promptBagMenu(user);
            return;
        }

        boolean used = false;
        String effectMsg = "";

        switch (itemName) {
            case "伤药":
                if (pm.getCurrentHp() >= pm.getMaxHp()) {
                    user.sendMessage("HP已经是满的了！");
                    return;
                }
                pm.heal(20);
                effectMsg = "恢复了 20 点 HP。";
                used = true;
                break;
            case "好伤药":
                if (pm.getCurrentHp() >= pm.getMaxHp()) {
                    user.sendMessage("HP已经是满的了！");
                    return;
                }
                pm.heal(50);
                effectMsg = "恢复了 50 点 HP。";
                used = true;
                break;
            case "攻击强化剂":
                pm.boostAttack(5);
                effectMsg = "攻击力提升了！";
                used = true;
                break;
            case "防御强化剂":
                pm.boostDefense(5);
                effectMsg = "防御力提升了！";
                used = true;
                break;
            default:
                user.sendMessage("这个道具无法在 PvP 中使用。");
                return;
        }

        if (used) {
            Map<String, Integer> bag = p.getBag();
            bag.put(itemName, bag.get(itemName) - 1);
            if (bag.get(itemName) <= 0) bag.remove(itemName);

            broadcast("\n💊 " + p.getName() + " 使用了 [" + itemName + "] !");
            broadcast(">> " + pm.getName() + " " + effectMsg);

            switchTurn();
        }
    }

    private void performAttack(ClientHandler attacker, ClientHandler defender, Skill skill) {
        PocketMon myPoke = attacker.getPlayer().getFirstPokemon();
        PocketMon enemyPoke = defender.getPlayer().getFirstPokemon();

        if (myPoke == null || enemyPoke == null) {
            endBattle(null);
            return;
        }

        double multiplier = getTypeMultiplier(skill.getType(), enemyPoke.getType());
        int baseDamage = (skill.getPower() + myPoke.getAttack()) - enemyPoke.getDefense();
        if (baseDamage < 1) baseDamage = 1;
        int finalDamage = (int) (baseDamage * multiplier);

        enemyPoke.takeDamage(finalDamage);

        String effectMsg = "";
        if (multiplier > 1.0) effectMsg = " (效果拔群!)";
        else if (multiplier < 1.0 && multiplier > 0) effectMsg = " (效果微弱...)";

        broadcast("\n⚡ " + attacker.getPlayer().getName() + " 的 " + myPoke.getName() +
                " 使用了 [" + skill.getName() + "] !");

        if (!effectMsg.isEmpty()) broadcast(effectMsg);

        broadcast("对 " + enemyPoke.getName() + " 造成了 " + finalDamage + " 点伤害！");

        if (enemyPoke.isFainted()) {
            broadcast("\n" + defender.getPlayer().getName() + " 的 " + enemyPoke.getName() + " 倒下了！");
            endBattle(attacker);
        } else {
            switchTurn();
        }
    }

    private void switchTurn() {
        ClientHandler nextPlayer = (currentTurn == p1) ? p2 : p1;
        currentTurn = nextPlayer;
        currentState = TurnState.ACTION_SELECT;

        showStatus(p1);

        promptTurn();
    }


    private double getTypeMultiplier(PocketMon.Type skillType, PocketMon.Type defType) {
        switch (skillType) {
            case FIRE:
                if (defType == PocketMon.Type.GRASS) return 1.5;
                if (defType == PocketMon.Type.WATER) return 0.75;
                break;
            case WATER:
                if (defType == PocketMon.Type.FIRE) return 1.5;
                if (defType == PocketMon.Type.GRASS) return 0.75;
                break;
            case ELECTRIC:
                if (defType == PocketMon.Type.WATER || defType == PocketMon.Type.FLYING) return 1.5;
                if (defType == PocketMon.Type.GRASS || defType == PocketMon.Type.ELECTRIC) return 0.75;
                break;
            case FLYING:
                if (defType == PocketMon.Type.GRASS || defType == PocketMon.Type.BUG) return 1.5;
                if (defType == PocketMon.Type.ELECTRIC) return 0.75;
                break;
            case BUG:
                if (defType == PocketMon.Type.GRASS) return 1.5;
                if (defType == PocketMon.Type.FIRE || defType == PocketMon.Type.FLYING) return 0.75;
                break;
            default:
                break;
        }
        return 1.0;
    }

    private void promptTurn() {
        ClientHandler activePlayer = currentTurn;
        ClientHandler waitingPlayer = (currentTurn == p1) ? p2 : p1;

        waitingPlayer.sendMessage("等待 " + activePlayer.getPlayer().getName() + " 行动...");

        activePlayer.sendMessage("\n--- 你的回合 (" + activePlayer.getPlayer().getFirstPokemon().getName() + ") ---");
        activePlayer.sendMessage("1. 攻击 (Attack)");
        activePlayer.sendMessage("2. 物品 (Bag)");
        activePlayer.sendMessage("3. 逃跑 (Run)");
        activePlayer.sendMessage("请选择行动 [1-3]:");
    }

    private void promptSkillMenu(ClientHandler handler) {
        handler.sendMessage("\n--- 选择技能 ---");
        List<Skill> skills = handler.getPlayer().getFirstPokemon().getSkills();

        for (int i = 0; i < skills.size(); i++) {
            Skill s = skills.get(i);
            handler.sendMessage((i + 1) + ". " + s.getName() +
                    " [PP:" + s.getPp() + "/" + s.getMaxPp() + " | 威力:" + s.getPower() + "]");
        }
        handler.sendMessage("0. 返回上一级");
    }

    private void promptBagMenu(ClientHandler handler) {
        handler.sendMessage("\n--- 选择道具 ---");
        Map<String, Integer> bag = handler.getPlayer().getBag();

        tempItemList.clear();
        int index = 1;

        for (String itemName : bag.keySet()) {
            int count = bag.get(itemName);
            // 过滤出战斗可用道具
            if (count > 0 && isBattleItem(itemName)) {
                handler.sendMessage(index + ". " + itemName + " (x" + count + ")");
                tempItemList.add(itemName);
                index++;
            }
        }

        if (tempItemList.isEmpty()) {
            handler.sendMessage("（没有可用的战斗道具）");
        }

        handler.sendMessage("0. 返回上一级");
    }

    private boolean isBattleItem(String name) {
        return name.equals("伤药") || name.equals("好伤药") ||
                name.equals("攻击强化剂") || name.equals("防御强化剂");
    }

    private void endBattle(ClientHandler winner) {
        if (winner != null) {
            broadcast("\n=========================");
            broadcast("   胜者是: " + winner.getPlayer().getName() + "！");
            broadcast("=========================");

            winner.getPlayer().addMoney(200);
            winner.sendMessage("你获得了 200元 奖金！");

            ClientHandler loser = (winner == p1) ? p2 : p1;
            loser.getPlayer().declineMoney(200);
            loser.sendMessage("遗憾！你输了，扣除200元作为惩罚。");
        } else {
            broadcast("\n战斗异常结束。");
        }

        p1.endPvP();
        p2.endPvP();
    }

    private void showStatus(ClientHandler handler) {
        PocketMon pm = handler.getPlayer().getFirstPokemon();
        if (pm != null) {
            broadcast(handler.getPlayer().getName() + " 的 " + pm.getName() +
                    " [HP: " + pm.getCurrentHp() + "/" + pm.getMaxHp() + "]");
        }
    }

    private void broadcast(String msg) {
        p1.sendMessage(msg);
        p2.sendMessage(msg);
    }
}