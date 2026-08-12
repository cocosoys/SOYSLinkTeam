package soys.soyslinkteam.command.sub;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.command.SubCommand;
import soys.soyslinkteam.join.Invite;
import soys.soyslinkteam.join.impl.InviteJoinMethod;
import soys.soyslinkteam.permission.TeamAction;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.util.Placeholders;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * /team invite &lt;玩家&gt; — 邀请玩家加入队伍。
 */
public class InviteSub extends SubCommand {

    public InviteSub(SOYSLinkTeam plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "invite";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("邀请", "inv");
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        Player player = (Player) sender;

        if (args.length < 1) {
            msg(player, "join.invite.usage", Placeholders.of("label", label).build());
            return;
        }

        InviteJoinMethod method = plugin.getJoinMethodRegistry().getInviteMethod();
        if (method == null || !method.isEnabled()) {
            msg(player, "join.method-disabled", Placeholders.of("method", "队长邀请").build());
            return;
        }

        String targetName = args[0];

        withPlayerTeam(player, team -> {
            if (!checkAction(player, team, TeamAction.INVITE)) {
                return;
            }

            // 解析目标玩家
            Player online = Bukkit.getPlayerExact(targetName);
            UUID targetId;
            String resolvedName;

            if (online != null && online.isOnline()) {
                targetId = online.getUniqueId();
                resolvedName = online.getName();
            } else {
                if (!method.isAllowOffline()) {
                    msg(player, "general.player-offline",
                            Placeholders.of("player", targetName).build());
                    return;
                }
                @SuppressWarnings("deprecation")
                OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
                if (offline == null || !offline.hasPlayedBefore()) {
                    msg(player, "general.player-not-found",
                            Placeholders.of("player", targetName).build());
                    return;
                }
                targetId = offline.getUniqueId();
                resolvedName = offline.getName() == null ? targetName : offline.getName();
            }

            if (targetId.equals(player.getUniqueId())) {
                msg(player, "join.invite.invite-self", null);
                return;
            }

            if (plugin.getTeamManager().hasTeam(targetId)) {
                msg(player, "join.invite.already-in-team",
                        Placeholders.of("player", resolvedName).build());
                return;
            }

            if (plugin.getTeamManager().isFull(team)) {
                msg(player, "team.team-full", Placeholders
                        .of("team", team.getName())
                        .and("size", team.getSize())
                        .and("max", plugin.getTeamManager().getMaxSize(team))
                        .build());
                return;
            }

            // 重复邀请冷却
            int cooldown = plugin.getInviteManager()
                    .getResendCooldown(team.getId(), targetId, method.getResendCooldown());
            if (cooldown > 0) {
                msg(player, "join.invite.already-invited", Placeholders
                        .of("player", resolvedName).and("seconds", cooldown).build());
                return;
            }

            // 待处理邀请上限
            int maxPending = method.getMaxPending();
            if (maxPending > 0 && plugin.getInviteManager().countPending(targetId) >= maxPending) {
                msg(player, "join.invite.pending-limit",
                        Placeholders.of("player", resolvedName).build());
                return;
            }

            // 登记邀请
            Invite invite = new Invite(team.getId(), team.getName(),
                    player.getUniqueId(), player.getName(), targetId, method.getExpireMillis());
            plugin.getInviteManager().add(invite);
            plugin.getTeamManager().save(team);

            msg(player, "join.invite.sent", Placeholders
                    .of("player", resolvedName)
                    .and("seconds", method.getExpireSeconds())
                    .build());

            notifyTarget(targetId, team, player, label);
        });
    }

    /**
     * 向在线的被邀请者推送提示。
     */
    private void notifyTarget(UUID targetId, Team team, Player inviter, String label) {
        Player target = Bukkit.getPlayer(targetId);
        if (target == null || !target.isOnline()) {
            return;
        }
        msg(target, "join.invite.received", Placeholders
                .of("player", inviter.getName()).and("team", team.getName()).build());
        msg(target, "join.invite.received-hint", Placeholders
                .of("team", team.getName()).and("label", label).build());
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 1 ? onlinePlayerNames(args[0]) : null;
    }
}
