package soys.soyslinkteam.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.team.TeamMember;
import soys.soyslinkteam.team.TeamRole;

/**
 * 成员角色变更事件（提升 / 降级副队长）。在角色写入前触发，可取消。
 */
public class TeamRoleChangeEvent extends TeamEvent {

    private static final HandlerList handlers = new HandlerList();

    private final TeamMember member;
    private final TeamRole oldRole;
    private final TeamRole newRole;

    public TeamRoleChangeEvent(Team team, TeamMember member, TeamRole oldRole, TeamRole newRole, Player actor) {
        super(team, actor);
        this.member = member;
        this.oldRole = oldRole;
        this.newRole = newRole;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    /** 被调整角色的成员 */
    public TeamMember getMember() {
        return member;
    }

    /** 被调整角色的玩家（离线时为 null） */
    public Player getTarget() {
        return member == null ? null : member.getPlayer();
    }

    /** 变更前的角色 */
    public TeamRole getOldRole() {
        return oldRole;
    }

    /** 变更后的角色 */
    public TeamRole getNewRole() {
        return newRole;
    }
}
