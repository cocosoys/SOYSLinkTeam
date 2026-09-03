package soys.soyslinkteam.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.command.SubCommand;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.util.Placeholders;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * /steam chat [on|off|toggle] — 切换队伍频道聊天。别名 c。
 */
public class ChatSub extends SubCommand {

    public ChatSub(SOYSLinkTeam plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "chat";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("c");
    }

    @Override
    public String getPermission() {
        return "soyslinkteam.chat";
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            msg(sender, "general.player-only");
            return;
        }
        Player player = (Player) sender;
        if (!plugin.getConfigManager().isTeamChatEnabled()) {
            msg(player, "chat.disabled-feature");
            return;
        }
        withPlayerTeam(player, team -> {
            boolean on;
            if (args.length == 0) {
                on = plugin.getTeamChannelManager().toggleTeamChat(player.getUniqueId());
            } else {
                String a = args[0].toLowerCase();
                if (a.equals("on") || a.equals("true") || a.equals("开启")) {
                    on = true;
                } else if (a.equals("off") || a.equals("false") || a.equals("关闭")) {
                    on = false;
                } else if (a.equals("toggle") || a.equals("切换")) {
                    on = plugin.getTeamChannelManager().toggleTeamChat(player.getUniqueId());
                } else {
                    msg(player, "chat.usage");
                    return;
                }
                if (!(a.equals("toggle") || a.equals("切换"))) {
                    plugin.getTeamChannelManager().setTeamChat(player.getUniqueId(), on);
                }
            }
            msg(player, on ? "chat.enabled" : "chat.disabled",
                    Placeholders.of("team", team.getName()).build());
        });
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("on", "off", "toggle"), args[0]);
        }
        return Collections.emptyList();
    }
}
