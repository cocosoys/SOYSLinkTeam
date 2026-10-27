package soys.soyslinkteam.command.sub.admin;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.command.SubCommand;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.util.Text;

import java.util.Arrays;
import java.util.List;

/**
 * /teamadmin disband &lt;队伍&gt; — 强制解散任意队伍。
 */
public class AdminDisbandSub extends SubCommand {

    public AdminDisbandSub(SOYSLinkTeam plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "disband";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("解散");
    }

    @Override
    public String getPermission() {
        return "soyslinkteam.admin.manage";
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        if (args.length < 1) {
            msgList(sender, "soyslinkteam.admin.usage");
            return;
        }
        String teamName = Text.join(args, 0).trim();
        plugin.getTeamManager().getTeamByNameAsync(teamName, team -> {
            if (team == null) {
                msg(sender, "soyslinkteam.team.team-not-found", teamName);
                return;
            }
            String name = team.getName();
            Player actor = sender instanceof Player ? (Player) sender : null;
            if (!plugin.getTeamManager().disbandTeam(team, actor)) {
                msg(sender, "soyslinkteam.event.cancelled", "");
                return;
            }
            team.broadcast(plugin.getMessageManager().get("soyslinkteam.team.disband.admin-broadcast",
                    team.getName()));
            plugin.getInviteManager().removeByTeam(team.getId());
            msg(sender, "soyslinkteam.admin.force.disband-success", name);
        });
    }
}
