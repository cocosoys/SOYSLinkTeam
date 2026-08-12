package soys.soyslinkteam.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import soys.soyslinkteam.team.Team;

/**
 * 玩家加入队伍事件。在成员被真正加入队伍前触发，可取消。
 *
 * <p>{@link #getActor()} 为加入者本人；{@link #getInviter()} 为发出邀请的玩家，
 * 通过公开 / 口令方式加入时为 null。</p>
 */
public class PlayerJoinTeamEvent extends TeamEvent {

    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final Player inviter;

    public PlayerJoinTeamEvent(Team team, Player player, Player inviter) {
        super(team, player);
        this.player = player;
        this.inviter = inviter;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    /** 加入队伍的玩家 */
    public Player getPlayer() {
        return player;
    }

    /** 邀请者（仅邀请入队时有值），公开 / 口令入队时为 null */
    public Player getInviter() {
        return inviter;
    }
}
