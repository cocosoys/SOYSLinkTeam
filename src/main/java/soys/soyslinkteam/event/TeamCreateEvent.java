package soys.soyslinkteam.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import soys.soyslinkteam.team.Team;

/**
 * 队伍创建事件。在队伍正式写入存储前触发，可取消。
 */
public class TeamCreateEvent extends TeamEvent {

    private static final HandlerList handlers = new HandlerList();

    public TeamCreateEvent(Team team, Player creator) {
        super(team, creator);
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    /** 队伍创建者 */
    public Player getCreator() {
        return actor;
    }
}
