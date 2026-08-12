package soys.soyslinkteam.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import soys.soyslinkteam.team.Team;

/**
 * 所有队伍事件的抽象基类。
 *
 * <h3>基础属性</h3>
 * <ul>
 *   <li>{@link #getTeam()} — 事件关联的队伍（永不为 null）；</li>
 *   <li>{@link #getActor()} — 触发该操作的操作者，控制台 / 系统行为为 null；</li>
 *   <li>{@link #getTimestamp()} — 事件发生时间（毫秒时间戳）。</li>
 * </ul>
 *
 * <h3>可取消</h3>
 * 基类实现 {@link Cancellable}，因此监听方可以 {@link #setCancelled(boolean)} 否决任何队伍操作，
 * 也可用 {@link #setCancelled(String)} 附带取消原因（供指令侧向玩家反馈）。
 *
 * <h3>监听层级</h3>
 * 由于 Bukkit 的事件分发会沿类层级向上查找处理器，注册 {@code TeamEvent} 的监听器将收到全部子事件，
 * 而注册具体子事件（如 {@code TeamCreateEvent}）的监听器只会收到对应事件。
 */
public abstract class TeamEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    protected final Team team;
    protected final Player actor;
    private final long timestamp;

    private boolean cancelled = false;
    private String cancelReason = null;

    protected TeamEvent(Team team, Player actor) {
        this.team = team;
        this.actor = actor;
        this.timestamp = System.currentTimeMillis();
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /** 事件关联的队伍 */
    public Team getTeam() {
        return team;
    }

    /** 触发操作的操作者（玩家或执行指令者），控制台 / 系统行为为 null */
    public Player getActor() {
        return actor;
    }

    /** 事件发生时间（毫秒时间戳） */
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    /**
     * 取消操作并附带原因（供指令侧反馈给玩家）。
     */
    public void setCancelled(String reason) {
        this.cancelled = true;
        this.cancelReason = reason;
    }

    /** 取消原因，未被取消时为 null */
    public String getCancelReason() {
        return cancelReason;
    }
}
