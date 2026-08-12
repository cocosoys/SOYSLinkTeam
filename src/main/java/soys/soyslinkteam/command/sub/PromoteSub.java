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
 * /team promote &lt;玩家&gt; — 提升为副队长。
 */
public class PromoteSub extends SubCommand {

    public PromoteSub(SOYSLinkTeam plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "promote";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("提升", "prom");
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        Player player = (Player) sender;

        if (args.length < 1) {
            msg(player, "member.promote.usage", Placeholders.of("label", label).build());
            return;
        }

        withPlayerTeam(player, team -> {
            if (!checkAction(player, team, TeamAction.PROMOTE)) {
                return;
            }

            String targetName = args[0];
            TeamMember target = team.getMemberByName(targetName);
            if (target == null) {
                msg(player, "team.target-not-in-your-team",
                        Placeholders.of("player", targetName).build());
                return;
            }

            if (target.getRole() == TeamRole.ADMIN) {
                msg(player, "member.promote.already-admin",
                        Placeholders.of("player", target.getName()).build());
                return;
            }

            int limit = plugin.getConfigManager().getMaxTeamAdmins();
            if (limit > 0 && team.countRole(TeamRole.ADMIN) >= limit) {
                msg(player, "member.promote.limit-reached",
                        Placeholders.of("max", limit).build());
                return;
            }

            TeamRoleChangeEvent roleEvent = new TeamRoleChangeEvent(
                    team, target, TeamRole.MEMBER, TeamRole.ADMIN, player);
            if (!callEvent(roleEvent, player)) {
                return;
            }
            team.setRole(target.getUuid(), TeamRole.ADMIN);
            plugin.getTeamManager().save(team);

            msg(player, "member.promote.success",
                    Placeholders.of("player", target.getName()).build());

            Player promoted = target.getPlayer();
            if (promoted != null && promoted.isOnline()) {
                msg(promoted, "member.promote.received",
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
        UUID pid = player.getUniqueId();
        List<String> names = new ArrayList<>();
        for (TeamMember member : loaded.getSortedMembers()) {
            if (!member.getUuid().equals(pid) && member.getRole() != TeamRole.ADMIN) {
                names.add(member.getName());
            }
        }
        return filter(names, args[0]);
    }
}
