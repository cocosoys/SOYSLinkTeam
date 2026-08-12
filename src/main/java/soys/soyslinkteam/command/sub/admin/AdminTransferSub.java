package soys.soyslinkteam.command.sub.admin;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.command.SubCommand;
import soys.soyslinkteam.event.TeamTransferEvent;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.team.TeamMember;
import soys.soyslinkteam.util.Placeholders;
import soys.soyslinkteam.util.Text;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * /teamadmin transfer &lt;队伍&gt; &lt;玩家&gt; — 强制转让任意队伍的队长。
 */
public class AdminTransferSub extends SubCommand {

    public AdminTransferSub(SOYSLinkTeam plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "transfer";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("转让");
    }

    @Override
    public String getPermission() {
        return "soyslinkteam.admin.manage";
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            msgList(sender, "admin.usage", null);
            return;
        }
        String teamName = args[0];
        String targetName = args[1];

        UUID targetId = resolve(targetName);
        if (targetId == null) {
            msg(sender, "general.player-not-found", Placeholders.of("player", targetName).build());
            return;
        }

        plugin.getTeamManager().getTeamByNameAsync(teamName, team -> {
            if (team == null) {
                msg(sender, "team.team-not-found", Placeholders.of("team", teamName).build());
                return;
            }
            TeamMember target = team.getMember(targetId);
            if (target == null) {
                msg(sender, "team.target-not-in-your-team",
                        Placeholders.of("player", targetName).build());
                return;
            }
            Player actor = sender instanceof Player ? (Player) sender : null;
            TeamMember oldLeaderMember = team.getLeaderMember();
            Player oldLeader = oldLeaderMember == null ? null : oldLeaderMember.getPlayer();
            TeamTransferEvent transferEvent = new TeamTransferEvent(team, oldLeader, target.getPlayer(), actor);
            if (!callEvent(transferEvent, sender)) {
                return;
            }
            team.transferLeader(targetId);
            plugin.getTeamManager().save(team);
            msg(sender, "admin.force.transfer-success",
                    Placeholders.of("team", team.getName()).and("player", target.getName()).build());

            org.bukkit.entity.Player newLeader = target.getPlayer();
            if (newLeader != null && newLeader.isOnline()) {
                msg(newLeader, "member.transfer.received",
                        Placeholders.of("team", team.getName()).build());
            }
        });
    }

    private UUID resolve(String name) {
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
