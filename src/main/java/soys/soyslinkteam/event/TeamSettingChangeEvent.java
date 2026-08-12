package soys.soyslinkteam.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import soys.soyslinkteam.team.Team;

/**
 * 队伍设置项变更事件（简称 / 公告 / 公开状态 / 口令）。
 * 在设置写入前触发，可取消。新旧值均以字符串呈现，便于监听器统一处理。
 */
public class TeamSettingChangeEvent extends TeamEvent {

    private static final HandlerList handlers = new HandlerList();

    /** 受影响的设置项 */
    public enum SettingType {
        /** 队伍简称 */
        TAG,
        /** 队伍公告 */
        NOTICE,
        /** 公开状态 */
        PUBLIC,
        /** 入队口令 */
        PASSWORD
    }

    private final SettingType setting;
    private final String oldValue;
    private final String newValue;

    public TeamSettingChangeEvent(Team team, SettingType setting, String oldValue, String newValue, Player actor) {
        super(team, actor);
        this.setting = setting;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    /** 发生变更的设置项 */
    public SettingType getSetting() {
        return setting;
    }

    /** 变更前的取值（字符串） */
    public String getOldValue() {
        return oldValue;
    }

    /** 变更后的取值（字符串） */
    public String getNewValue() {
        return newValue;
    }
}
