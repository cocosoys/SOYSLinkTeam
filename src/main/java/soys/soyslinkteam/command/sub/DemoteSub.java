package soys.soyslinkteam.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.command.SubCommand;
import soys.soyslinkteam.event.TeamRoleChangeEvent;
import soys.soyslinkteam.permission.TeamAction;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.team.TeamMember;
import soys.soyslinkteam.team.TeamRole;
import soys.soyslinkteam.util.Placeholders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * /team demote &lt;玩家&gt; — 将副队长降为普通队员。
 */
public class DemoteSub extends SubCommand {

    public DemoteSub(SOYSLinkTeam plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "demote";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("降级", "dem");
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        Player player = (Player) sender;

        if (args.length < 1) {
            msg(player, "member.demote.usage", Placeholders.of("label", label).build());
            return;
        }

        withPlayerTeam(player, team -> {
            if (!checkAction(player, team, TeamAction.DEMOTE)) {
                return;
            }

            String targetName = args[0];
            TeamMember target = team.getMemberByName(targetName);
            if (target == null) {
                msg(player, "team.target-not-in-your-team",
                        Placeholders.of("player", targetName).build());
                return;
            }

            if (target.getRole() != TeamRole.ADMIN) {
                msg(player, "member.demote.not-admin",
                        Placeholders.of("player", target.getName()).build());
                return;
            }

            TeamRoleChangeEvent roleEvent = new TeamRoleChangeEvent(
                    team, target, TeamRole.ADMIN, TeamRole.MEMBER, player);
            if (!callEvent(roleEvent, player)) {
                return;
            }
            team.setRole(target.getUuid(), TeamRole.MEMBER);
            plugin.getTeamManager().save(team);

            msg(player, "member.demote.success",
                    Placeholders.of("player", target.getName()).build());

            Player demoted = target.getPlayer();
            if (demoted != null && demoted.isOnline()) {
                msg(demoted, "member.demote.received",
                        Placeholders.of("team", team.getName()).build());
            }
        });
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length != 1 || !(sender instanceof Player)) {
            return null;
        }
        Player player = (Player) sender;
        Team loaded = plugin.getTeamManager().getLoadedPlayerTeam(player.getUniqueId());
        if (loaded == null) {
            return null;
        }
        List<String> names = new ArrayList<>();
        for (TeamMember member : loaded.getSortedMembers()) {
            if (member.getRole() == TeamRole.ADMIN) {
                names.add(member.getName());
            }
        }
        return filter(names, args[0]);
    }
}
