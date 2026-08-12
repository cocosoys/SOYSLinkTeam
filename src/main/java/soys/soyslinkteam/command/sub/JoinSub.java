package soys.soyslinkteam.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.command.SubCommand;
import soys.soyslinkteam.join.JoinContext;
import soys.soyslinkteam.join.JoinResult;
import soys.soyslinkteam.util.Placeholders;
import soys.soyslinkteam.util.Text;

import java.util.Arrays;
import java.util.List;

/**
 * /team join &lt;队伍名称&gt; [口令] — 加入队伍。
 * <p>统一走 {@link soys.soyslinkteam.join.JoinMethodRegistry#tryJoin}，由启用的入队方式决定具体逻辑。</p>
 */
public class JoinSub extends SubCommand {

    public JoinSub(SOYSLinkTeam plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "join";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("加入", "j");
    }

    @Override
    public String getPermission() {
        return "soyslinkteam.join";
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        Player player = (Player) sender;

        if (args.length < 1) {
            msg(player, "join.usage", Placeholders.of("label", label).build());
            return;
        }

        if (plugin.getTeamManager().hasTeam(player.getUniqueId())) {
            String current = currentTeamName(player);
            msg(player, "team.already-in-team", Placeholders.of("team", current).build());
            return;
        }

        String teamName = args[0];
        String argument = args.length > 1 ? Text.join(args, 1).trim() : null;

        plugin.getTeamManager().getTeamByNameAsync(teamName, team -> {
            if (team == null) {
                msg(player, "team.team-not-found", Placeholders.of("team", teamName).build());
                return;
            }
            JoinContext context = JoinContext.of(player, team, argument, label);
            JoinResult result = plugin.getJoinMethodRegistry().tryJoin(context);
            if (!result.isSuccess() && result.getMessageKey() != null) {
                msg(player, result.getMessageKey(), result.getPlaceholders());
            }
        });
    }

    private String currentTeamName(Player player) {
        java.util.UUID teamId = plugin.getTeamManager().getPlayerTeamId(player.getUniqueId());
        String name = teamId == null ? null : plugin.getTeamManager().getTeamNameIndex().get(teamId);
        return name == null ? "未知" : name;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 1 ? teamNames(args[0]) : null;
    }
}
