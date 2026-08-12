package soys.soyslinkteam.command.sub;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.command.SubCommand;
import soys.soyslinkteam.join.Invite;
import soys.soyslinkteam.util.Placeholders;
import soys.soyslinkteam.util.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * /team deny [队伍] — 拒绝队伍邀请。
 */
public class DenySub extends SubCommand {

    public DenySub(SOYSLinkTeam plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "deny";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("拒绝", "refuse");
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        Player player = (Player) sender;

        List<Invite> pending = plugin.getInviteManager().getPending(player.getUniqueId());
        if (pending.isEmpty()) {
            msg(player, "join.invite.accept.no-pending", null);
            return;
        }

        Invite target;
        if (args.length >= 1) {
            String teamName = Text.join(args, 0).trim();
            target = null;
            for (Invite invite : pending) {
                if (invite.getTeamName().equalsIgnoreCase(teamName)) {
                    target = invite;
                    break;
                }
            }
            if (target == null) {
                msg(player, "join.invite.accept.not-found",
                        Placeholders.of("team", teamName).build());
                return;
            }
        } else if (pending.size() == 1) {
            target = pending.get(0);
        } else {
            List<String> names = new ArrayList<>();
            for (Invite invite : pending) {
                names.add(invite.getTeamName());
            }
            msg(player, "join.invite.accept.multiple-hint",
                    Placeholders.of("teams", String.join("&7, &f", names)).build());
            return;
        }

        plugin.getInviteManager().remove(player.getUniqueId(), target.getTeamId());
        msg(player, "join.invite.deny.success",
                Placeholders.of("team", target.getTeamName()).build());

        // 通知邀请人
        Player inviter = Bukkit.getPlayer(target.getInviter());
        if (inviter != null && inviter.isOnline()) {
            msg(inviter, "join.invite.deny.notify-inviter",
                    Placeholders.of("player", player.getName()).build());
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length != 1 || !(sender instanceof Player)) {
            return null;
        }
        List<String> names = new ArrayList<>();
        for (Invite invite : plugin.getInviteManager()
                .getPending(((Player) sender).getUniqueId())) {
            names.add(invite.getTeamName());
        }
        return filter(names, args[0]);
    }
}
