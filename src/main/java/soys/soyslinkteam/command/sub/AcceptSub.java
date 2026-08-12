package soys.soyslinkteam.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.command.SubCommand;
import soys.soyslinkteam.join.Invite;
import soys.soyslinkteam.join.JoinContext;
import soys.soyslinkteam.join.JoinResult;
import soys.soyslinkteam.util.Placeholders;
import soys.soyslinkteam.util.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * /team accept [队伍] — 接受队伍邀请。
 * <p>只有一条待处理邀请时可省略队伍名。</p>
 */
public class AcceptSub extends SubCommand {

    public AcceptSub(SOYSLinkTeam plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "accept";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("接受", "同意", "acc");
    }

    @Override
    public String getPermission() {
        return "soyslinkteam.join";
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        Player player = (Player) sender;

        if (plugin.getTeamManager().hasTeam(player.getUniqueId())) {
            msg(player, "team.already-in-team", Placeholders.of("team", currentTeamName(player)).build());
            return;
        }

        List<Invite> pending = plugin.getInviteManager().getPending(player.getUniqueId());
        if (pending.isEmpty()) {
            msg(player, "join.invite.accept.no-pending", null);
            return;
        }

        Invite target;
        if (args.length >= 1) {
            String teamName = Text.join(args, 0).trim();
            target = findByName(pending, teamName);
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

        final Invite invite = target;
        plugin.getTeamManager().getTeamAsync(invite.getTeamId(), team -> {
            if (team == null) {
                plugin.getInviteManager().remove(player.getUniqueId(), invite.getTeamId());
                msg(player, "join.invite.cancelled",
                        Placeholders.of("team", invite.getTeamName()).build());
                return;
            }

            JoinContext context = JoinContext.of(player, team, null, label);
            JoinResult result = plugin.getJoinMethodRegistry().tryJoin(context);

            if (!result.isSuccess()) {
                if (result.getMessageKey() != null) {
                    msg(player, result.getMessageKey(), result.getPlaceholders());
                }
                return;
            }

            // 入队成功消息已由注册表发送，这里补一条队内广播
            team.broadcast(plugin.getMessageManager().get("join.invite.accept.broadcast",
                            Placeholders.of("player", player.getName()).build()),
                    player.getUniqueId());
        });
    }

    private Invite findByName(List<Invite> pending, String teamName) {
        for (Invite invite : pending) {
            if (invite.getTeamName().equalsIgnoreCase(teamName)) {
                return invite;
            }
        }
        return null;
    }

    private String currentTeamName(Player player) {
        java.util.UUID teamId = plugin.getTeamManager().getPlayerTeamId(player.getUniqueId());
        String name = teamId == null ? null : plugin.getTeamManager().getTeamNameIndex().get(teamId);
        return name == null ? "未知" : name;
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
