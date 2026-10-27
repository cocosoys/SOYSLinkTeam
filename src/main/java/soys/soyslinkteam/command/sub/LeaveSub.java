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

            team.broadcast(plugin.getMessageManager().get("soyslinkteam.member.leave-broadcast",
                    player.getName(), team.getSize(),
                    plugin.getTeamManager().getMaxSize(team)));

            msg(player, "soyslinkteam.member.leave-success", teamName);
        });
    }

    /**
     * 队长退出：按配置转让、解散或拒绝。
     */
    private void handleLeaderLeave(Player player, Team team, String label) {
        String action = plugin.getConfigManager().getLeaderQuitAction();

        if ("DENY".equals(action)) {
            msg(player, "soyslinkteam.member.leave-leader-denied", label);
            return;
        }

        String teamName = team.getName();

        if ("DISBAND".equals(action)) {
            if (!plugin.getTeamManager().disbandTeam(team, player)) {
                msg(player, "soyslinkteam.event.cancelled", "");
                return;
            }
            team.broadcast(plugin.getMessageManager().get("soyslinkteam.team.disband.broadcast",
                    teamName, player.getName()));
            plugin.getInviteManager().removeByTeam(team.getId());
            applyRejoinCooldown(player);
            msg(player, "soyslinkteam.member.leave-leader-disbanded");
            return;
        }

        // 默认 TRANSFER：转让给继任者，无人时解散
        TeamMember successor = team.pickSuccessor(player.getUniqueId());
        if (successor == null) {
            if (!plugin.getTeamManager().disbandTeam(team, player)) {
                msg(player, "soyslinkteam.event.cancelled", "");
                return;
            }
            plugin.getInviteManager().removeByTeam(team.getId());
            applyRejoinCooldown(player);
            msg(player, "soyslinkteam.member.leave-leader-disbanded");
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

        team.broadcast(plugin.getMessageManager().get("soyslinkteam.member.leave-broadcast",
                player.getName(), team.getSize(),
                plugin.getTeamManager().getMaxSize(team)));
        team.broadcast(plugin.getMessageManager().get("soyslinkteam.member.transfer.broadcast",
                successor.getName()));

        Player newLeader = successor.getPlayer();
        if (newLeader != null && newLeader.isOnline()) {
            msg(newLeader, "soyslinkteam.member.transfer.received", teamName);
        }

        msg(player, "soyslinkteam.member.leave-leader-transferred", successor.getName());
    }

    private void applyRejoinCooldown(Player player) {
        if (plugin.getPermissionManager().canBypass(player)) {
            return;
        }
        plugin.getCooldownManager().set(CooldownManager.REJOIN, player.getUniqueId(),
                plugin.getConfigManager().getRejoinCooldown());
    }
}
