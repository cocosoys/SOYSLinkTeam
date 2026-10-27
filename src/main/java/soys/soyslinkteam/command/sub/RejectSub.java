package soys.soyslinkteam.command.sub;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.application.ApplicationManager;
import soys.soyslinkteam.command.SubCommand;
import soys.soyslinkteam.permission.TeamAction;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.util.Text;

import java.util.Arrays;
import java.util.List;

/**
 * /team reject &lt;玩家&gt; [理由] — 拒绝某玩家对自己队伍的入队申请。
 * <p>与 {@link ApproveSub} 成对，后端由 ApplicationManager 托管。</p>
 */
public class RejectSub extends SubCommand {

    public RejectSub(SOYSLinkTeam plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "reject";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("驳回", "denyapp");
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            msg(sender, "soyslinkteam.general.player-only");
            return;
        }
        Player player = (Player) sender;

        if (args.length < 1) {
            msg(player, "soyslinkteam.reject.usage", label);
            return;
        }

        Player applicant = Bukkit.getPlayer(args[0]);
        if (applicant == null) {
            msg(player, "soyslinkteam.apply.target-offline", args[0]);
            return;
        }

        // 理由 = 除玩家名外的其余参数
        String reason = args.length > 1
                ? Text.join(args, 1).trim()
                : "";

        withPlayerTeam(player, team -> {
            if (!checkAction(player, team, TeamAction.APPROVE)) {
                return;
            }
            ApplicationManager.ApproveResult result =
                    plugin.getApplicationManager().deny(team.getId(), applicant.getUniqueId(), reason);
            switch (result) {
                case SUCCESS:
                    msg(player, "soyslinkteam.reject.success",
                            applicant.getName(), reason.isEmpty() ? "未填写" : reason);
                    // 申请人反馈由 ApplicationManager 统一发送（apply.denied）
                    break;
                case NOT_FOUND:
                    msg(player, "soyslinkteam.reject.not-found", applicant.getName());
                    break;
                case ALREADY_PROCESSED:
                    msg(player, "soyslinkteam.reject.already", applicant.getName());
                    break;
                default:
                    msg(player, "soyslinkteam.reject.error");
            }
        });
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length != 1 || !(sender instanceof Player)) {
            return null;
        }
        // 候选 = 向自己队伍提交过申请的在线玩家
        return filter(onlinePlayerNames(null), args[0]);
    }
}
