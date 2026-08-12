package soys.soyslinkteam.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.command.SubCommand;
import soys.soyslinkteam.event.PlayerLeaveTeamEvent;
import soys.soyslinkteam.event.TeamTransferEvent;
import soys.soyslinkteam.permission.TeamAction;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.team.TeamMember;
import soys.soyslinkteam.util.CooldownManager;
import soys.soyslinkteam.util.Placeholders;

import java.util.Arrays;
import java.util.List;

/**
 * /team leave — 退出队伍。
 * <p>队长退出的处理方式由 {@code team.behavior.leader-quit-action} 决定。</p>
 */
public class LeaveSub extends SubCommand {

    public LeaveSub(SOYSLinkTeam plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "leave";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("quit", "退出", "离队");
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        Player player = (Player) sender;

        withPlayerTeam(player, team -> {
            if (!checkAction(player, team, TeamAction.LEAVE)) {
                return;
            }

            if (team.isLeader(player.getUniqueId())) {
                handleLeaderLeave(player, team, label);
                return;
            }

            String teamName = team.getName();

            PlayerLeaveTeamEvent leaveEvent = new PlayerLeaveTeamEvent(
                    team, player, PlayerLeaveTeamEvent.Reason.VOLUNTARY, player);
            if (!callEvent(leaveEvent, player)) {
                return;
            }

            plugin.getTeamManager().removeMember(team, player.getUniqueId());
            applyRejoinCooldown(player);

            team.broadcast(plugin.getMessageManager().get("member.leave-broadcast", Placeholders
                    .of("player", player.getName())
                    .and("size", team.getSize())
                    .and("max", plugin.getTeamManager().getMaxSize(team))
                    .build()));

            msg(player, "member.leave-success", Placeholders.of("team", teamName).build());
        });
    }

    /**
     * 队长退出：按配置转让、解散或拒绝。
     */
    private void handleLeaderLeave(Player player, Team team, String label) {
        String action = plugin.getConfigManager().getLeaderQuitAction();

        if ("DENY".equals(action)) {
            msg(player, "member.leave-leader-denied", Placeholders.of("label", label).build());
            return;
        }

        String teamName = team.getName();

        if ("DISBAND".equals(action)) {
            if (!plugin.getTeamManager().disbandTeam(team, player)) {
                msg(player, "event.cancelled", Placeholders.of("reason", "").build());
                return;
            }
            team.broadcast(plugin.getMessageManager().get("team.disband.broadcast", Placeholders
                    .of("team", teamName).and("player", player.getName()).build()));
            plugin.getInviteManager().removeByTeam(team.getId());
            applyRejoinCooldown(player);
            msg(player, "member.leave-leader-disbanded", null);
            return;
        }

        // 默认 TRANSFER：转让给继任者，无人时解散
        TeamMember successor = team.pickSuccessor(player.getUniqueId());
        if (successor == null) {
            if (!plugin.getTeamManager().disbandTeam(team, player)) {
                msg(player, "event.cancelled", Placeholders.of("reason", "").build());
                return;
            }
            plugin.getInviteManager().removeByTeam(team.getId());
            applyRejoinCooldown(player);
            msg(player, "member.leave-leader-disbanded", null);
            return;
        }

        PlayerLeaveTeamEvent leaveEvent = new PlayerLeaveTeamEvent(
                team, player, PlayerLeaveTeamEvent.Reason.TRANSFER, player);
        if (!callEvent(leaveEvent, player)) {
            return;
        }
        TeamTransferEvent transferEvent = new TeamTransferEvent(
                team, player, successor.getPlayer(), player);
        if (!callEvent(transferEvent, player)) {
            return;
        }

        plugin.getTeamManager().removeMember(team, player.getUniqueId());
        team.transferLeader(successor.getUuid());
        plugin.getTeamManager().save(team);
        applyRejoinCooldown(player);

        team.broadcast(plugin.getMessageManager().get("member.leave-broadcast", Placeholders
                .of("player", player.getName())
                .and("size", team.getSize())
                .and("max", plugin.getTeamManager().getMaxSize(team))
                .build()));
        team.broadcast(plugin.getMessageManager().get("member.transfer.broadcast",
                Placeholders.of("player", successor.getName()).build()));

        Player newLeader = successor.getPlayer();
        if (newLeader != null && newLeader.isOnline()) {
            msg(newLeader, "member.transfer.received",
                    Placeholders.of("team", teamName).build());
        }

        msg(player, "member.leave-leader-transferred",
                Placeholders.of("player", successor.getName()).build());
    }

    private void applyRejoinCooldown(Player player) {
        if (plugin.getPermissionManager().canBypass(player)) {
            return;
        }
        plugin.getCooldownManager().set(CooldownManager.REJOIN, player.getUniqueId(),
                plugin.getConfigManager().getRejoinCooldown());
    }
}
