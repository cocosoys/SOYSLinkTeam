package soys.soyslinkteam.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import soys.soyslinkteam.team.Team;

/**
 * 队长转让事件。在 {@code transferLeader} 执行前触发，可取消。
 */
public class TeamTransferEvent extends TeamEvent {

    private static final HandlerList handlers = new HandlerList();

    private final Player oldLeader;
    private final Player newLeader;

    public TeamTransferEvent(Team team, Player oldLeader, Player newLeader, Player actor) {
        super(team, actor);
        this.oldLeader = oldLeader;
        this.newLeader = newLeader;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    /** 原队长（操作前） */
    public Player getOldLeader() {
        return oldLeader;
    }

    /** 新任队长 */
    public Player getNewLeader() {
        return newLeader;
    }
}
