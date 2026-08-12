package soys.soyslinkteam.command.sub.admin;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.command.SubCommand;
import soys.soyslinkteam.event.PlayerKickedEvent;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.util.CooldownManager;
import soys.soyslinkteam.util.Placeholders;
import soys.soyslinkteam.util.Text;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * /teamadmin kick &lt;玩家&gt; — 强制将任意玩家踢出其所处队伍。
 */
public class AdminKickSub extends SubCommand {

    public AdminKickSub(SOYSLinkTeam plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "kick";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("踢出");
    }

    @Override
    public String getPermission() {
        return "soyslinkteam.admin.manage";
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        if (args.length < 1) {
            msgList(sender, "admin.usage", null);
            return;
        }
        String targetName = args[0];
        UUID targetId = resolve(targetName);
        if (targetId == null) {
            msg(sender, "general.player-not-found", Placeholders.of("player", targetName).build());
            return;
        }

        UUID teamId = plugin.getTeamManager().getPlayerTeamId(targetId);
        if (teamId == null) {
            msg(sender, "team.target-not-in-team", Placeholders.of("player", targetName).build());
            return;
        }

        plugin.getTeamManager().getTeamAsync(teamId, team -> {
            if (team == null) {
                msg(sender, "team.target-not-in-team", Placeholders.of("player", targetName).build());
                return;
            }
            Player kicked = Bukkit.getPlayer(targetId);
            Player actor = sender instanceof Player ? (Player) sender : null;
            PlayerKickedEvent kickEvent = new PlayerKickedEvent(team, kicked, actor);
            if (!callEvent(kickEvent, sender)) {
                return;
            }
            plugin.getTeamManager().removeMember(team, targetId);
            if (!plugin.getPermissionManager().canBypass(Bukkit.getPlayer(targetId))) {
                plugin.getCooldownManager().set(CooldownManager.REJOIN, targetId,
                        plugin.getConfigManager().getRejoinCooldown());
            }
            msg(sender, "admin.force.kick-success",
                    Placeholders.of("player", targetName).and("team", team.getName()).build());
            // 若该成员是最后一名，队伍已因空自动解散
            if (team.isDisbanded()) {
                msg(sender, "team.disband.success", Placeholders.of("team", team.getName()).build());
            }

            kicked = Bukkit.getPlayer(targetId);
            if (kicked != null && kicked.isOnline()) {
                msgList(kicked, "member.kick.kicked",
                        Placeholders.of("operator", sender.getName()).and("team", team.getName()).build());
            }
        });
    }

    private UUID resolve(String name) {
        Player online = Bukkit.getPlayerExact(name);
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
