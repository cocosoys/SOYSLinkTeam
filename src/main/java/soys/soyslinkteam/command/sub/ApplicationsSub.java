package soys.soyslinkteam.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.application.JoinApplication;
import soys.soyslinkteam.command.SubCommand;
import soys.soyslinkteam.permission.TeamAction;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.util.Text;

import java.util.Arrays;
import java.util.List;

/**
 * /team applications — 查看本队伍的待处理入队申请列表。
 * <p>需要 {@link TeamAction#APPROVE} 权限；配合 {@link ApproveSub} / {@link RejectSub} 完成审批闭环。</p>
 */
public class ApplicationsSub extends SubCommand {

    public ApplicationsSub(SOYSLinkTeam plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "applications";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("申请列表", "apps", "applist");
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            msg(sender, "soyslinkteam.general.player-only");
            return;
        }
        Player player = (Player) sender;

        withPlayerTeam(player, team -> {
            if (!checkAction(player, team, TeamAction.APPROVE)) {
                return;
            }
            List<JoinApplication> applications = plugin.getApplicationManager().getApplications(team.getId());
            if (applications.isEmpty()) {
                msg(player, "soyslinkteam.applications.empty", team.getName());
                return;
            }
            Text.send(player, plugin.getMessageManager().get("soyslinkteam.applications.header",
                    applications.size()));
            int index = 1;
            for (JoinApplication application : applications) {
                Text.send(player, plugin.getMessageManager().get("soyslinkteam.applications.entry",
                        index, application.getApplicantName(),
                        application.getMessage().isEmpty()
                                ? "无留言" : application.getMessage(),
                        application.getRemainingSeconds()));
                index++;
            }
        });
    }
}
