package soys.soyslinkteam.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import soys.soyslinkteam.team.Team;

/**
 * 队伍解散事件。在队伍从存储与内存移除前触发，可取消。
 */
public class TeamDisbandEvent extends TeamEvent {

    private static final HandlerList handlers = new HandlerList();

    public TeamDisbandEvent(Team team, Player disbander) {
        super(team, disbander);
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    /** 执行解散的操作者（玩家或管理员），系统行为为 null */
    public Player getDisbander() {
        return actor;
    }
}
