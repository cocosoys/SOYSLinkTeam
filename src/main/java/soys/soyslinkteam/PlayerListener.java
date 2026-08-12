package soys.soyslinkteam;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.team.TeamMember;

/**
 * 玩家上下线监听：维护队伍在线状态与内存激活。
 */
public class PlayerListener implements Listener {

    private final SOYSLinkTeam plugin;

    public PlayerListener(SOYSLinkTeam plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfigManager().isLoadOnJoin()) {
            return;
        }
        org.bukkit.entity.Player player = event.getPlayer();
        if (!plugin.getTeamManager().hasTeam(player.getUniqueId())) {
            return;
        }
        plugin.getTeamManager().getPlayerTeamAsync(player.getUniqueId(), team -> {
            if (team != null) {
                TeamMember member = team.getMember(player.getUniqueId());
                if (member != null) {
                    member.touchLastSeen();
                }
                // 玩家上线后检查本队队长是否离线过久，触发自动转让
                plugin.getTeamManager().maybeAutoTransfer(team);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        org.bukkit.entity.Player player = event.getPlayer();
        Team team = plugin.getTeamManager().getLoadedPlayerTeam(player.getUniqueId());
        if (team != null) {
            TeamMember member = team.getMember(player.getUniqueId());
            if (member != null) {
                member.touchLastSeen();
            }
        }
    }
}
