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
import soys.soyslinkteam.util.Placeholders;
import soys.soyslinkteam.util.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
                    msg(sender, "team.team-not-found",
                            Placeholders.of("team", teamName).build());
                    return;
                }
                sendInfo(sender, team);
            });
            return;
        }

        if (!(sender instanceof Player)) {
            msg(sender, "general.player-only", null);
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
        lines.add(mm.get("info.header"));
        lines.add(mm.get("info.name", Placeholders.of("name", team.getName()).build()));
        if (tagEnabled && team.getSettings().hasTag()) {
            lines.add(mm.get("info.tag",
                    Placeholders.of("tag", team.getSettings().getTag()).build()));
        }
        lines.add(mm.get("info.leader",
                Placeholders.of("leader", team.getLeaderName()).build()));
        lines.add(mm.get("info.size", Placeholders.of("size", team.getSize())
                .and("max", plugin.getTeamManager().getMaxSize(team))
                .and("online", team.getOnlineCount()).build()));
        lines.add(mm.get("info.created",
                Placeholders.of("created", sdf.format(new java.util.Date(team.getCreatedAt())))
                        .build()));
        lines.add(mm.get("info.public",
                Placeholders.of("public", team.getSettings().isOpen() ? trueText : falseText)
                        .build()));
        lines.add(mm.get("info.locked",
                Placeholders.of("locked", team.getSettings().hasPassword() ? trueText : falseText)
                        .build()));
        if (noticeEnabled && team.getSettings().hasNotice()) {
            lines.add(mm.get("info.notice",
                    Placeholders.of("notice", team.getSettings().getNotice()).build()));
        }
        lines.add(mm.get("info.members-header"));
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
                    ? mm.get("info.status-online")
                    : mm.get("info.status-offline");
            lines.add(mm.get("info.member-entry", Placeholders.of("role", roleText)
                    .and("player", member.getName())
                    .and("status", status).build()));
        }
        lines.add(mm.get("info.footer"));

        for (String line : lines) {
            Text.send(sender, line);
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 1 ? teamNames(args[0]) : null;
    }
}
