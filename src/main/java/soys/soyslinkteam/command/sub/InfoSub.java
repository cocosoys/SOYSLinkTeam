package soys.soyslinkteam.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.command.SubCommand;
import soys.soyslinkteam.config.ConfigManager;
import soys.soyslinkteam.config.MessageManager;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.team.TeamMember;
import soys.soyslinkteam.team.TeamRole;
import soys.soyslinkteam.util.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * /team info [队伍] — 查看队伍详情。
 * <p>无参数时查看自己所在队伍；带参数时按名称查看任意队伍。</p>
 */
public class InfoSub extends SubCommand {

    public InfoSub(SOYSLinkTeam plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "info";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("信息", "detail");
    }

    @Override
    public String getPermission() {
        return "soyslinkteam.use";
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        if (args.length >= 1) {
            String teamName = Text.join(args, 0).trim();
            plugin.getTeamManager().getTeamByNameAsync(teamName, team -> {
                if (team == null) {
                    msg(sender, "soyslinkteam.team.team-not-found", teamName);
                    return;
                }
                sendInfo(sender, team);
            });
            return;
        }

        if (!(sender instanceof Player)) {
            msg(sender, "soyslinkteam.general.player-only");
            return;
        }
        withPlayerTeam((Player) sender, team -> sendInfo(sender, team));
    }

    public void sendInfo(CommandSender sender, Team team) {
        ConfigManager config = plugin.getConfigManager();
        MessageManager mm = plugin.getMessageManager();

        boolean tagEnabled = config.isTagEnabled();
        boolean noticeEnabled = config.isNoticeEnabled();
        String trueText = config.getPlaceholderTrue();
        String falseText = config.getPlaceholderFalse();
        String roleLeader = config.getRoleDisplay("LEADER", "队长");
        String roleAdmin = config.getRoleDisplay("ADMIN", "副队长");
        String roleMember = config.getRoleDisplay("MEMBER", "队员");

        SimpleDateFormat sdf = new SimpleDateFormat(config.getDateFormat());

        List<String> lines = new ArrayList<>();
        lines.add(mm.get("soyslinkteam.info.header"));
        lines.add(mm.get("soyslinkteam.info.name", team.getName()));
        if (tagEnabled && team.getSettings().hasTag()) {
            lines.add(mm.get("soyslinkteam.info.tag", team.getSettings().getTag()));
        }
        lines.add(mm.get("soyslinkteam.info.leader", team.getLeaderName()));
        lines.add(mm.get("soyslinkteam.info.size", team.getSize(),
                plugin.getTeamManager().getMaxSize(team), team.getOnlineCount()));
        lines.add(mm.get("soyslinkteam.info.created",
                sdf.format(new java.util.Date(team.getCreatedAt()))));
        lines.add(mm.get("soyslinkteam.info.public",
                team.getSettings().isOpen() ? trueText : falseText));
        lines.add(mm.get("soyslinkteam.info.locked",
                team.getSettings().hasPassword() ? trueText : falseText));
        if (team.getSettings().hasEconomyCost()) {
            double cost = team.getSettings().getEconomyCost();
            String costText = (cost == Math.floor(cost))
                    ? String.valueOf((long) cost)
                    : String.format("%.2f", cost);
            lines.add(mm.get("soyslinkteam.info.cost", costText));
        }
        if (noticeEnabled && team.getSettings().hasNotice()) {
            lines.add(mm.get("soyslinkteam.info.notice", team.getSettings().getNotice()));
        }
        lines.add(mm.get("soyslinkteam.info.members-header"));
        for (TeamMember member : team.getSortedMembers()) {
            String roleText;
            switch (member.getRole()) {
                case LEADER:
                    roleText = roleLeader;
                    break;
                case ADMIN:
                    roleText = roleAdmin;
                    break;
                default:
                    roleText = roleMember;
            }
            String status = member.isOnline()
                    ? mm.get("soyslinkteam.info.status-online")
                    : mm.get("soyslinkteam.info.status-offline");
            lines.add(mm.get("soyslinkteam.info.member-entry",
                    roleText, member.getName(), status));
        }
        lines.add(mm.get("soyslinkteam.info.footer"));

        for (String line : lines) {
            Text.send(sender, line);
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 1 ? teamNames(args[0]) : null;
    }
}
