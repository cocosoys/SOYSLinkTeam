package soys.soyslinkteam.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.command.SubCommand;
import soys.soyslinkteam.event.PlayerKickedEvent;
import soys.soyslinkteam.permission.TeamAction;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.team.TeamMember;
import soys.soyslinkteam.team.TeamRole;
import soys.soyslinkteam.util.CooldownManager;
import soys.soyslinkteam.util.Placeholders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * /team kick &lt;玩家&gt; — 将成员移出队伍。
 * <p>角色层级校验：队长可踢任何人（受 leader-can-kick-admin 约束），副队长仅可踢普通队员。</p>
 */
public class KickSub extends SubCommand {

    public KickSub(SOYSLinkTeam plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "kick";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("踢出", "k");
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        Player player = (Player) sender;

        if (args.length < 1) {
            msg(player, "member.kick.usage", Placeholders.of("label", label).build());
            return;
        }

        withPlayerTeam(player, team -> {
            if (!checkAction(player, team, TeamAction.KICK)) {
                return;
            }

            String targetName = args[0];
            TeamMember target = team.getMemberByName(targetName);
            if (target == null) {
                msg(player, "team.target-not-in-your-team",
                        Placeholders.of("player", targetName).build());
                return;
            }

            if (target.getUuid().equals(player.getUniqueId())) {
                msg(player, "member.kick.cannot-kick-self", null);
                return;
            }

            if (team.isLeader(target.getUuid())) {
                msg(player, "member.kick.cannot-kick-leader", null);
                return;
            }

            TeamMember operator = team.getMember(player.getUniqueId());
            boolean canKick;
            if (team.isLeader(player.getUniqueId())) {
                canKick = plugin.getConfigManager().isLeaderCanKickAdmin()
                        || !target.getRole().equals(TeamRole.ADMIN);
            } else {
                canKick = operator != null && operator.getRole().isHigherThan(target.getRole());
            }
            if (!canKick) {
                msg(player, "member.kick.cannot-kick-same-role", null);
                return;
            }

            // 执行踢出
            PlayerKickedEvent kickEvent = new PlayerKickedEvent(team, target.getPlayer(), player);
            if (!callEvent(kickEvent, player)) {
                return;
            }
            plugin.getTeamManager().removeMember(team, target.getUuid());

            // 被踢者加入冷却，避免反复骚扰同一队伍
            if (!plugin.getPermissionManager().canBypass(target.getPlayer())) {
                plugin.getCooldownManager().set(CooldownManager.REJOIN, target.getUuid(),
                        plugin.getConfigManager().getRejoinCooldown());
            }

            msg(player, "member.kick.success",
                    Placeholders.of("player", target.getName()).build());

            team.broadcast(plugin.getMessageManager().get("member.kick.broadcast", Placeholders
                    .of("player", target.getName())
                    .and("operator", player.getName())
                    .and("size", team.getSize())
                    .and("max", plugin.getTeamManager().getMaxSize(team))
                    .build()), target.getUuid());

            Player kicked = target.getPlayer();
            if (kicked != null && kicked.isOnline()) {
                msgList(kicked, "member.kick.kicked", Placeholders
                        .of("operator", player.getName())
                        .and("team", team.getName()).build());
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
            if (!member.getUuid().equals(pid)) {
                names.add(member.getName());
            }
        }
        return filter(names, args[0]);
    }
}
