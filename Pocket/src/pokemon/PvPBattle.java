package pokemon;

import java.util.List;
import java.util.Map;

public class PvPBattle {
    private ClientHandler p1;
    private ClientHandler p2;
    private ClientHandler currentTurn;
    private boolean isSelectingItem = false;

    public PvPBattle(ClientHandler p1, ClientHandler p2) {
        this.p1 = p1;
        this.p2 = p2;
        this.currentTurn = p1;
    }

    public void start() {
        broadcast("\n === PK 开始！=== ");
        broadcast(p1.getPlayer().getName() + " VS " + p2.getPlayer().getName());

        showHpStatus();

        promptTurn();
    }

    public void handleInput(ClientHandler sender, String input) {
        if (sender != currentTurn) {
            sender.sendMessage("还没轮到你！请等待对手行动...");
            return;
        }

        if (input.equalsIgnoreCase("run") || input.equals("逃跑")) {
            broadcast(sender.getPlayer().getName() + " 认输逃跑了！");
            endBattle(sender == p1 ? p2 : p1);
            return;
        }

        try {
            if (isSelectingItem) {
                handleItemSelection(sender, input);
                return;
            }

            int optionIndex = Integer.parseInt(input) - 1;
            PocketMon myPoke = sender.getPlayer().getFirstPokemon();
            List<Skill> skills = myPoke.getSkills();
            int skillCount = skills.size();

            if (optionIndex >= 0 && optionIndex < skillCount) {
                Skill chosenSkill = skills.get(optionIndex);
                ClientHandler target = (sender == p1) ? p2 : p1;
                performAttack(sender, target, chosenSkill);
            }
            else if (optionIndex == skillCount) {
                showItemList(sender);
            } else {
                sender.sendMessage("选项编号错误！请输入正确的选项编号");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("请输入正确的选项编号 (例如: 1) 或输入 run 认输。");
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
        int level = myPoke.getLevel();

        double powerMultiplier = 1.0 + (level - 1) * 0.05;
        int effectivePower = (int) (skill.getPower() * powerMultiplier);
        int attack = myPoke.getAttack();
        int defense = Math.max(1, enemyPoke.getDefense());

        double rawDamage =
                (((4.0 * level / 5 + 2)
                        * effectivePower
                        * attack / defense) / 15)
                        + 20;

        int finalDamage = (int) Math.max(1, rawDamage * multiplier);

        enemyPoke.takeDamage(finalDamage);

        String effectMsg = "";
        if (multiplier > 1.0) effectMsg = " (效果拔群!)";
        else if (multiplier < 1.0 && multiplier > 0) effectMsg = " (效果微弱...)";

        broadcast("\n" + attacker.getPlayer().getName() + " 的 " + myPoke.getName() +
                " 使用了 [" + skill.getName() + "] !");

        if (!effectMsg.isEmpty()) broadcast(effectMsg);

        broadcast("对 " + enemyPoke.getName() + " 造成了 " + finalDamage + " 点伤害！");

        if (enemyPoke.isFainted()) {
            broadcast("\n" + defender.getPlayer().getName() + " 的 " + enemyPoke.getName() + " 倒下了！");
            endBattle(attacker);
        } else {
            currentTurn = defender;

            broadcast("--------------------------------");
            broadcast(myPoke.getName() + ": " + myPoke.getHp() + "/" + myPoke.getMaxHp() + " HP");
            broadcast(enemyPoke.getName() + ": " + enemyPoke.getHp() + "/" + enemyPoke.getMaxHp() + " HP");
            broadcast("--------------------------------");

            promptTurn();
        }
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

        activePlayer.sendMessage("\n轮到你了！请选择操作:");
        List<Skill> skills = activePlayer.getPlayer().getFirstPokemon().getSkills();
        int optionCount = 0;

        for (int i = 0; i < skills.size(); i++) {
            optionCount++;
            Skill s = skills.get(i);
            activePlayer.sendMessage(optionCount + ". 使用技能: " + s.getName() +
                    " [威力:" + s.getPower() + " | " + s.getType() + "]");
        }

        optionCount++;
        activePlayer.sendMessage(optionCount + ". 使用道具");

        activePlayer.sendMessage("输入 'run' 认输");
    }

    private void showItemList(ClientHandler player) {
        isSelectingItem = true;
        Map<String, Integer> bag = player.getPlayer().getBag();

        if (bag.isEmpty()) {
            player.sendMessage("背包是空的，没有道具可以使用！");
            isSelectingItem = false;
            promptTurn();
            return;
        }

        player.sendMessage("\n选择要使用的道具:");
        int itemIndex = 0;
        for (Map.Entry<String, Integer> entry : bag.entrySet()) {
            itemIndex++;
            player.sendMessage(itemIndex + ". " + entry.getKey() + " x" + entry.getValue());
        }
        player.sendMessage("输入 'cancel' 取消使用道具");
    }

    private void handleItemSelection(ClientHandler player, String input) {
        if (input.equalsIgnoreCase("cancel")) {
            isSelectingItem = false;
            promptTurn();
            return;
        }

        try {
            int itemIndex = Integer.parseInt(input) - 1;
            Map<String, Integer> bag = player.getPlayer().getBag();

            if (itemIndex >= 0 && itemIndex < bag.size()) {
                String[] itemNames = bag.keySet().toArray(new String[0]);
                String selectedItem = itemNames[itemIndex];
                useItemInBattle(player, selectedItem);
            } else {
                player.sendMessage("道具编号错误！请输入正确的道具编号");
            }
        } catch (NumberFormatException e) {
            player.sendMessage("请输入道具编号 (例如: 1) 或输入 'cancel' 取消");
        }
    }

    private void useItemInBattle(ClientHandler player, String itemName) {
        Player p = player.getPlayer();
        PocketMon pokemon = p.getFirstPokemon();
        String resultMsg = "";
        boolean isUsed = false;

        if (!p.getBag().containsKey(itemName) || p.getBag().get(itemName) <= 0) {
            player.sendMessage("你没有" + itemName + "。");
            isSelectingItem = false;
            promptTurn();
            return;
        }

        switch (itemName) {
            case "伤药":
                if (pokemon.getCurrentHp() == pokemon.getMaxHp()) {
                    player.sendMessage(pokemon.getName() + "的HP已经是满的了！");
                    break;
                }
                pokemon.heal(20);
                resultMsg = p.getName() + "使用了" + itemName + "！" + pokemon.getName() + "恢复了20HP";
                isUsed = true;
                break;
            case "好伤药":
                if (pokemon.getCurrentHp() == pokemon.getMaxHp()) {
                    player.sendMessage(pokemon.getName() + "的HP已经是满的了！");
                    break;
                }
                pokemon.heal(50);
                resultMsg = p.getName() + "使用了" + itemName + "！" + pokemon.getName() + "恢复了50HP";
                isUsed = true;
                break;
            case "经验糖果":
                pokemon.gainExp(100);
                resultMsg = p.getName() + "使用了" + itemName + "！" + pokemon.getName() + "获得了100经验值";
                isUsed = true;
                break;
            case "攻击强化剂":
                pokemon.boostAttack(10);
                resultMsg = p.getName() + "使用了" + itemName + "！" + pokemon.getName() + "的攻击力提升了10点";
                isUsed = true;
                break;
            case "防御强化剂":
                pokemon.boostDefense(10);
                resultMsg = p.getName() + "使用了" + itemName + "！" + pokemon.getName() + "的防御力提升了10点";
                isUsed = true;
                break;
            default:
                player.sendMessage("这个道具无法在战斗中使用！");
                break;
        }

        if (isUsed) {
            p.removeItem(itemName, 1);
            broadcast("--------------------------------");
            broadcast(resultMsg);
            broadcast("--------------------------------");

            ClientHandler opponent = (player == p1) ? p2 : p1;
            if (opponent.getPlayer().getFirstPokemon().isFainted()) {
                endBattle(player);
                return;
            }
        }

        isSelectingItem = false;

        currentTurn = (currentTurn == p1) ? p2 : p1;

        showHpStatus();
        promptTurn();
    }

    private boolean finished = false;

    public synchronized void handleDisconnect(ClientHandler leaver) {
        if (finished) return;
        broadcast("\n[系统] 玩家 " + leaver.getPlayer().getName() + " 掉线，对战结束。");

        ClientHandler winner = (leaver == p1) ? p2 : p1;
        endBattle(winner);
    }

    private void endBattle(ClientHandler winner) {
        if (finished) return;
        finished = true;

        if (winner != null) {
            broadcast("\n=========================");
            broadcast("   胜者是: " + winner.getPlayer().getName() + "！");
            broadcast("=========================");

            winner.getPlayer().addMoney(200);
            winner.sendMessage("你获得了 200元 奖金！");

            if (Math.random() < 0.35) {
                winner.getPlayer().addItem("伤药", 1);
            } else if (Math.random() < 0.15) {
                winner.getPlayer().addItem("经验糖果", 1);
            } else {
                winner.sendMessage("本次对战没有获得额外道具掉落。");
            }

            ClientHandler loser = (winner == p1) ? p2 : p1;
            loser.getPlayer().declineMoney(200);
            loser.sendMessage("遗憾！你输了，扣除200元作为惩罚。");
        } else {
            broadcast("\n战斗异常结束。");
        }

        p1.endPvP();
        p2.endPvP();
    }

    private void showHpStatus() {
        PocketMon pm1 = p1.getPlayer().getFirstPokemon();
        PocketMon pm2 = p2.getPlayer().getFirstPokemon();

        broadcast("\n----------------------------");
        broadcast(p1.getPlayer().getName() + "：Lv." + pm1.getLevel() + " " + pm1.getName()
                + " " + pm1.getHp() + "/" + pm1.getMaxHp() + " HP");

        broadcast(p2.getPlayer().getName() + "：Lv." + pm2.getLevel() + " " + pm2.getName()
                + " " + pm2.getHp() + "/" + pm2.getMaxHp() + " HP");
        broadcast("----------------------------");
    }



    private void broadcast(String msg) {
        p1.sendMessage(msg);
        p2.sendMessage(msg);
    }
}