package soys.soyslinkteam.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import soys.soyslinkteam.team.Team;

/**
 * 玩家被踢出队伍事件。是 {@link PlayerLeaveTeamEvent} 的特例（原因 {@code KICKED}），在成员被移出前触发，可取消。
 */
public class PlayerKickedEvent extends PlayerLeaveTeamEvent {

    private static final HandlerList handlers = new HandlerList();

    public PlayerKickedEvent(Team team, Player kicked, Player kicker) {
        super(team, kicked, Reason.KICKED, kicker);
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    /** 执行踢出的操作者（玩家或管理员），控制台操作时为 null */
    public Player getKicker() {
        return getActor();
    }
}
