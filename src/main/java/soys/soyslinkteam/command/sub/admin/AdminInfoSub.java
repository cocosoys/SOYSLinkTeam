package soys.soyslinkteam.command.sub.admin;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.command.SubCommand;
import soys.soyslinkteam.command.sub.InfoSub;
import soys.soyslinkteam.util.Placeholders;
import soys.soyslinkteam.util.Text;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * /teamadmin info &lt;队伍|玩家&gt; — 查看任意队伍详情。
 * <p>先按队伍名匹配，未命中再按玩家名解析其所在队伍。</p>
 */
public class AdminInfoSub extends SubCommand {

    public AdminInfoSub(SOYSLinkTeam plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "info";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("信息", "detail");
    }

    @Override
    public String getPermission() {
        return "soyslinkteam.admin.inspect";
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        if (args.length < 1) {
            msgList(sender, "admin.usage", null);
            return;
        }
        String input = Text.join(args, 0).trim();

        plugin.getTeamManager().getTeamByNameAsync(input, team -> {
            if (team != null) {
                new InfoSub(plugin).sendInfo(sender, team);
                return;
            }
            UUID playerId = resolvePlayer(input);
            if (playerId == null) {
                msg(sender, "team.team-not-found", Placeholders.of("team", input).build());
                return;
            }
            plugin.getTeamManager().getPlayerTeamAsync(playerId, playerTeam -> {
                if (playerTeam == null) {
                    msg(sender, "team.target-not-in-team",
                            Placeholders.of("player", input).build());
                    return;
                }
                new InfoSub(plugin).sendInfo(sender, playerTeam);
            });
        });
    }

    private UUID resolvePlayer(String name) {
        org.bukkit.entity.Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online.getUniqueId();
        }
        @SuppressWarnings("deprecation")
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        if (offline != null && offline.hasPlayedBefore()) {
            return offline.getUniqueId();
        }
        return null;
    }
}
