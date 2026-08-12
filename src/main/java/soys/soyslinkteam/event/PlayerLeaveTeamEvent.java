package soys.soyslinkteam.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import soys.soyslinkteam.team.Team;

/**
 * 玩家离开队伍事件（基础事件）。
 *
 * <p>涵盖主动退出、被踢出、因转让队长而退出、因队伍解散而退出等场景，
 * 通过 {@link #getReason()} 区分。在成员被移出队伍前触发，可取消。</p>
 *
 * <p>被踢出的场景会触发子类 {@link PlayerKickedEvent}，注册本事件的监听器同样会收到。</p>
 */
public class PlayerLeaveTeamEvent extends TeamEvent {

    private static final HandlerList handlers = new HandlerList();

    /** 离开原因 */
    public enum Reason {
        /** 主动退出 */
        VOLUNTARY,
        /** 被踢出 */
        KICKED,
        /** 因转让队长而退出（原队长） */
        TRANSFER,
        /** 因队伍解散而退出 */
        DISBAND
    }

    private final Player player;
    private final Reason reason;

    public PlayerLeaveTeamEvent(Team team, Player player, Reason reason, Player actor) {
        super(team, actor);
        this.player = player;
        this.reason = reason;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    /** 离开队伍的玩家 */
    public Player getPlayer() {
        return player;
    }

    /** 离开原因 */
    public Reason getReason() {
        return reason;
    }
}
