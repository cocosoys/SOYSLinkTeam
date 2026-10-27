package soys.soyslinkteam.command.sub;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.application.ApplicationManager;
import soys.soyslinkteam.command.SubCommand;
import soys.soyslinkteam.permission.TeamAction;
import soys.soyslinkteam.team.Team;

import java.util.Arrays;

/**
 * /team approve &lt;玩家&gt; — 批准某玩家对自己队伍的入队申请。
 */
public class ApproveSub extends SubCommand {

    public ApproveSub(SOYSLinkTeam plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "approve";
    }

    @Override
    public java.util.List<String> getAliases() {
        return Arrays.asList("批准", "acceptapp");
    }

    @Override
    public void execute(org.bukkit.command.CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            msg(sender, "soyslinkteam.general.player-only");
            return;
        }
        Player player = (Player) sender;

        if (args.length < 1) {
            msg(player, "soyslinkteam.approve.usage", label);
            return;
        }

        Player applicant = Bukkit.getPlayer(args[0]);
        if (applicant == null) {
            msg(player, "soyslinkteam.apply.target-offline", args[0]);
            return;
        }

        withPlayerTeam(player, team -> {
            if (!checkAction(player, team, TeamAction.APPROVE)) {
                return;
            }
            ApplicationManager.ApproveResult result =
                    plugin.getApplicationManager().approve(team.getId(), applicant.getUniqueId());
            switch (result) {
                case SUCCESS:
                    msg(player, "soyslinkteam.approve.success",
                            applicant.getName(), team.getName());
                    // 申请人反馈由 ApplicationManager 统一发送（apply.approved）
                    break;
                case NOT_FOUND:
                    msg(player, "soyslinkteam.approve.not-found", applicant.getName());
                    break;
                case ALREADY_PROCESSED:
                    msg(player, "soyslinkteam.approve.already", applicant.getName());
                    break;
                case APPLICANT_OFFLINE:
                    msg(player, "soyslinkteam.approve.offline", applicant.getName());
                    break;
                case ALREADY_IN_TEAM:
                    msg(player, "soyslinkteam.approve.in-team", applicant.getName());
                    break;
                case TEAM_FULL:
                    msg(player, "soyslinkteam.approve.full", team.getName());
                    break;
                default:
                    msg(player, "soyslinkteam.approve.error");
            }
        });
    }
}
