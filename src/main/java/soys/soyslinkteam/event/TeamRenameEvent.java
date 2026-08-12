package soys.soyslinkteam.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import soys.soyslinkteam.team.Team;

/**
 * 队伍改名事件。在名称写入与索引更新前触发，可取消。
 */
public class TeamRenameEvent extends TeamEvent {

    private static final HandlerList handlers = new HandlerList();

    private final String oldName;
    private final String newName;

    public TeamRenameEvent(Team team, String oldName, String newName, Player actor) {
        super(team, actor);
        this.oldName = oldName;
        this.newName = newName;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    /** 改名前的名称 */
    public String getOldName() {
        return oldName;
    }

    /** 改名后的名称 */
    public String getNewName() {
        return newName;
    }
}
