package pokemon;

import java.util.Timer;
import java.util.TimerTask;

public class DuelManager {
    private ClientHandler handler;
    private Timer duelTimer;
    private boolean isDuelInitiator = false;
    private ClientHandler duelTarget;

    public DuelManager(ClientHandler handler) {
        this.handler = handler;
    }

    public void handleRequest(String targetName) {
        if (targetName.equals(handler.getPlayer().getName())) {
            handler.sendMessage("你不能和自己打架！");
            return;
        }

        ClientHandler targetHandler = ClientHandler.onlinePlayers.get(targetName);
        if (targetHandler == null) {
            handler.sendMessage("找不到玩家: " + targetName);
            return;
        }

        if (targetHandler.getCurrentRoom() != handler.getCurrentRoom()) {
            handler.sendMessage("必须在同一房间！");
            return;
        }

        if (targetHandler.getActiveBattle() != null || targetHandler.getDuelManager().hasPendingDuel()) {
            handler.sendMessage("对方正忙。");
            return;
        }

        this.duelTarget = targetHandler;
        this.isDuelInitiator = true;

        targetHandler.getDuelManager().receiveRequest(handler);
        handler.sendMessage("已向 " + targetName + " 发起挑战！等待30秒...");

        stopTimer();
        duelTimer = new Timer();
        duelTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (handler.getActiveBattle() == null && duelTarget != null) {
                    duelTarget.sendMessage("\n>> 响应超时，挑战失效。");
                    if (duelTarget.getDuelManager().getDuelTarget() == handler) {
                        duelTarget.getDuelManager().clearDuelTarget();
                    }
                    handler.sendMessage("\n>> 对方响应超时。");
                    handler.printPrompt();
                    cancelDuel(false);
                }
            }
        }, 30000);
    }

    public void receiveRequest(ClientHandler challenger) {
        this.duelTarget = challenger;
        handler.sendMessage("\n收到挑战！玩家 [" + challenger.getPlayer().getName() + "] 想和你 PK！");
        handler.sendMessage("输入 'yes' (接受) 或 'no' (拒绝)");
    }

    public void handleResponse(boolean accept) {
        if (duelTarget == null) {
            handler.sendMessage("没有挑战请求。");
            return;
        }

        if (accept) {
            handler.sendMessage("接受了挑战！");
            duelTarget.sendMessage(handler.getPlayer().getName() + " 接受了挑战！");

            duelTarget.getDuelManager().stopTimer();
            duelTarget.getDuelManager().setInitiator(false);
            this.stopTimer();

            PvPBattle battle = new PvPBattle(duelTarget, handler);
            handler.setActiveBattle(battle);
            duelTarget.setActiveBattle(battle);
            battle.start();

            this.duelTarget = null;
        } else {
            handler.sendMessage("拒绝了挑战。");
            duelTarget.sendMessage(handler.getPlayer().getName() + " 拒绝了你的挑战。");
            duelTarget.getDuelManager().cancelDuel(false);
            this.duelTarget = null;
        }
    }

    public void cancelDuel(boolean notify) {
        stopTimer();
        isDuelInitiator = false;
        if (duelTarget != null) {
            if (notify) duelTarget.sendMessage("\n>> 对方取消了挑战。");
            if (duelTarget.getDuelManager().getDuelTarget() == handler) {
                duelTarget.getDuelManager().clearDuelTarget();
            }
            duelTarget = null;
            if (notify) handler.sendMessage("取消了挑战请求。");
        }
    }

    public void stopTimer() {
        if (duelTimer != null) {
            duelTimer.cancel();
            duelTimer = null;
        }
    }

    public boolean isInitiator() { return isDuelInitiator; }
    public void setInitiator(boolean b) { this.isDuelInitiator = b; }
    public boolean hasPendingDuel() { return duelTarget != null; }
    public ClientHandler getDuelTarget() { return duelTarget; }
    public void clearDuelTarget() { this.duelTarget = null; }
}