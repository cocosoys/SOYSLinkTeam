package soys.soyslinkteam.command.sub;

import org.bukkit.entity.Player;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.command.SubCommand;
import soys.soyslinkteam.team.Team;

import java.util.Arrays;

/**
 * /team apply <队伍名称> [留言] — 向非公开队伍提交入队申请。
 * <p>审批由队长/管理员通过 /team approve &lt;玩家&gt; 完成，后端由 ApplicationManager 托管。</p>
 */
public class ApplySub extends SubCommand {

    public ApplySub(SOYSLinkTeam plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "apply";
    }

    @Override
    public java.util.List<String> getAliases() {
        return Arrays.asList("申请", "applyto");
    }

    @Override
    public String getPermission() {
        return null;
    }

    @Override
    public void execute(org.bukkit.command.CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            msg(sender, "soyslinkteam.general.player-only");
            return;
        }
        Player player = (Player) sender;

        if (args.length < 1) {
            msg(player, "soyslinkteam.apply.usage", label);
            return;
        }

        if (plugin.getTeamManager().hasTeam(player.getUniqueId())) {
            msg(player, "soyslinkteam.apply.already-in-team");
            return;
        }

        String teamName = args[0];
        String message = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "";
        long expireMillis = plugin.getConfigManager().getApplicationExpireSeconds() * 1000L;

        plugin.getTeamManager().getTeamByNameAsync(teamName, team -> {
            if (team == null) {
                msg(player, "soyslinkteam.apply.not-found", teamName);
                return;
            }
            soys.soyslinkteam.application.JoinApplication application =
                    plugin.getApplicationManager().submit(team, player, message, expireMillis);
            if (application == null) {
                // 重复申请或玩家已在该队（hasTeam 已校验，此处多为重复申请）
                msg(player, "soyslinkteam.apply.already-submitted", team.getName());
                return;
            }
            msg(player, "soyslinkteam.apply.submitted", team.getName());
            // 通知队伍内有权限者
            for (org.bukkit.entity.Player member : team.getOnlinePlayers()) {
                if (plugin.getPermissionManager().can(member, team, soys.soyslinkteam.permission.TeamAction.APPROVE)) {
                    plugin.getMessageManager().send(member, "soyslinkteam.apply.notify-leader",
                            player.getName(), team.getName(), label);
                }
            }
        });
    }
}
