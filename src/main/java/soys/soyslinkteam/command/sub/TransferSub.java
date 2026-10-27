package soys.soyslinkteam.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.command.SubCommand;
import soys.soyslinkteam.event.TeamTransferEvent;
import soys.soyslinkteam.permission.TeamAction;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.team.TeamMember;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * /team transfer &lt;玩家&gt; — 转让队长。
 */
public class TransferSub extends SubCommand {

    public TransferSub(SOYSLinkTeam plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "transfer";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("转让", "tr");
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        Player player = (Player) sender;

        if (args.length < 1) {
            msg(player, "soyslinkteam.member.transfer.usage", label);
            return;
        }

        withPlayerTeam(player, team -> {
            if (!checkAction(player, team, TeamAction.TRANSFER)) {
                return;
            }

            String targetName = args[0];
            TeamMember target = team.getMemberByName(targetName);
            if (target == null) {
                msg(player, "soyslinkteam.team.target-not-in-your-team", targetName);
                return;
            }

            if (team.isLeader(target.getUuid())) {
                msg(player, "soyslinkteam.member.transfer.cannot-transfer-self");
                return;
            }

            TeamMember oldLeaderMember = team.getLeaderMember();
            Player oldLeader = oldLeaderMember == null ? null : oldLeaderMember.getPlayer();
            TeamTransferEvent transferEvent = new TeamTransferEvent(team, oldLeader, target.getPlayer(), player);
            if (!callEvent(transferEvent, player)) {
                return;
            }

            team.transferLeader(target.getUuid());
            plugin.getTeamManager().save(team);

            msg(player, "soyslinkteam.member.transfer.success", target.getName());
            team.broadcast(plugin.getMessageManager().get("soyslinkteam.member.transfer.broadcast",
                    target.getName()));

            Player newLeader = target.getPlayer();
            if (newLeader != null && newLeader.isOnline()) {
                msg(newLeader, "soyslinkteam.member.transfer.received", team.getName());
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
        if (loaded == null || !loaded.isLeader(player.getUniqueId())) {
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
