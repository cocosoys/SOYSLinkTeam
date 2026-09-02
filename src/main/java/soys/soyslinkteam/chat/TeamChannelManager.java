package soys.soyslinkteam.chat;

import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.team.TeamMember;

import java.util.UUID;

/**
 * 队伍频道状态管理。
 * <p>开关状态持久化于 {@link TeamMember}（随队伍存储落盘），本类仅提供读写入口，
 * 不直接持有任何会话状态。</p>
 */
public class TeamChannelManager {

    private final SOYSLinkTeam plugin;

    public TeamChannelManager(SOYSLinkTeam plugin) {
        this.plugin = plugin;
    }

    /**
     * 玩家是否处于队伍频道模式。
     * 队伍未装载或玩家不在任何队伍中时返回 false。
     */
    public boolean isTeamChat(UUID uuid) {
        Team team = plugin.getTeamManager().getLoadedPlayerTeam(uuid);
        if (team == null) {
            return false;
        }
        TeamMember member = team.getMember(uuid);
        return member != null && member.isChatChannel();
    }

    /**
     * 设置玩家的队伍频道开关并异步落盘。
     */
    public void setTeamChat(UUID uuid, boolean on) {
        Team team = plugin.getTeamManager().getLoadedPlayerTeam(uuid);
        if (team == null) {
            return;
        }
        TeamMember member = team.getMember(uuid);
        if (member == null || member.isChatChannel() == on) {
            return;
        }
        member.setChatChannel(on);
        plugin.getStorageManager().saveTeamAsync(team);
    }

    /**
     * 切换开关，返回切换后的状态（无法定位队伍时返回 false 且不产生任何变更）。
     */
    public boolean toggleTeamChat(UUID uuid) {
        boolean next = !isTeamChat(uuid);
        setTeamChat(uuid, next);
        return next;
    }
}
