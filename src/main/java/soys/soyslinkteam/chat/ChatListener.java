package soys.soyslinkteam.chat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.util.Text;

import java.util.UUID;

/**
 * 队伍频道聊天监听。
 * <p>处于频道模式的玩家发言将被拦截公共广播，改为仅队伍在线成员可见；
 * 拥有 {@code soyslinkteam.chat.spy} 权限的管理员可旁听。</p>
 * <p>与「聊天前缀」特性互不干扰：前缀由 PAPI 在外部聊天格式中解析，本监听仅在频道开启时接管事件。</p>
 */
public class ChatListener implements Listener {

    private final SOYSLinkTeam plugin;

    public ChatListener(SOYSLinkTeam plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = false)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!plugin.getTeamChannelManager().isTeamChat(uuid)) {
            return;
        }
        Team team = plugin.getTeamManager().getLoadedPlayerTeam(uuid);
        if (team == null) {
            // 队伍已不可见（极端竞态），安全关闭并放行公共聊天
            plugin.getTeamChannelManager().setTeamChat(uuid, false);
            return;
        }

        // 接管本次发言：取消公共广播，定向到队伍频道
        event.setCancelled(true);

        String raw = plugin.getConfigManager().getTeamChatFormat();
        String message = raw
                .replace("{player}", player.getDisplayName())
                .replace("{message}", event.getMessage());
        String colored = Text.color(message);

        for (Player member : team.getOnlinePlayers()) {
            member.sendMessage(colored);
        }

        // 管理员旁听（spy）
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.hasPermission("soyslinkteam.chat.spy")
                    && !team.hasMember(viewer.getUniqueId())) {
                viewer.sendMessage(Text.color("&8[spy] " + message));
            }
        }
    }
}
