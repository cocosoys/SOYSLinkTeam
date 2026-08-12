package soys.soyslinkteam.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.command.SubCommand;
import soys.soyslinkteam.event.TeamRenameEvent;
import soys.soyslinkteam.event.TeamSettingChangeEvent;
import soys.soyslinkteam.permission.TeamAction;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.util.NameValidator;
import soys.soyslinkteam.util.Placeholders;
import soys.soyslinkteam.util.Text;

import java.util.Arrays;
import java.util.List;

/**
 * /team set &lt;name|tag|notice|public|password&gt; ... — 队伍设置。
 * <p>按子动作分别做权限校验，每个子动作的权限节点定义在 permission.roles 中。</p>
 */
public class SetSub extends SubCommand {

    public SetSub(SOYSLinkTeam plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "set";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("设置", "settings");
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        Player player = (Player) sender;

        if (args.length < 1) {
            msgList(player, "settings.usage", Placeholders.of("label", label).build());
            return;
        }

        String key = args[0].toLowerCase();
        String joined = Text.join(args, 1).trim();

        withPlayerTeam(player, team -> {
            switch (key) {
                case "name":
                    setName(player, team, label, joined);
                    break;
                case "tag":
                    setTag(player, team, label, joined);
                    break;
                case "notice":
                    setNotice(player, team, label, joined);
                    break;
                case "public":
                    setPublic(player, team, label, joined);
                    break;
                case "password":
                    setPassword(player, team, label, joined);
                    break;
                default:
                    msgList(player, "settings.usage", Placeholders.of("label", label).build());
            }
        });
    }

    private void setName(Player player, Team team, String label, String value) {
        if (!checkAction(player, team, TeamAction.RENAME)) {
            return;
        }
        if (value.isEmpty()) {
            msgList(player, "settings.usage", Placeholders.of("label", label).build());
            return;
        }
        NameValidator.Result result = plugin.getNameValidator().validateTeamName(value, team.getId());
        if (!result.isValid()) {
            msg(player, result.getMessageKey(), result.getPlaceholders());
            return;
        }
        TeamRenameEvent renameEvent = new TeamRenameEvent(team, team.getName(), value, player);
        if (!callEvent(renameEvent, player)) {
            return;
        }
        plugin.getTeamManager().renameTeam(team, value);
        msg(player, "settings.name.success", Placeholders.of("name", value).build());
        team.broadcast(plugin.getMessageManager().get("settings.name.broadcast",
                Placeholders.of("player", player.getName()).and("name", value).build()));
    }

    private void setTag(Player player, Team team, String label, String value) {
        if (!checkAction(player, team, TeamAction.SET_TAG)) {
            return;
        }
        if (!plugin.getConfigManager().isTagEnabled()) {
            msg(player, "settings.tag.disabled", null);
            return;
        }
        if (value.isEmpty()) {
            TeamSettingChangeEvent tagEvent = new TeamSettingChangeEvent(team,
                    TeamSettingChangeEvent.SettingType.TAG, team.getSettings().getTag(), "", player);
            if (!callEvent(tagEvent, player)) {
                return;
            }
            team.getSettings().setTag("");
            plugin.getTeamManager().save(team);
            msg(player, "settings.tag.success",
                    Placeholders.of("tag", plugin.getConfigManager().getTagFallback()).build());
            return;
        }
        NameValidator.Result result = plugin.getNameValidator().validateTag(value);
        if (!result.isValid()) {
            msg(player, result.getMessageKey(), result.getPlaceholders());
            return;
        }
        TeamSettingChangeEvent tagEvent = new TeamSettingChangeEvent(team,
                TeamSettingChangeEvent.SettingType.TAG, team.getSettings().getTag(), value, player);
        if (!callEvent(tagEvent, player)) {
            return;
        }
        team.getSettings().setTag(value);
        plugin.getTeamManager().save(team);
        msg(player, "settings.tag.success", Placeholders.of("tag", value).build());
    }

    private void setNotice(Player player, Team team, String label, String value) {
        if (!checkAction(player, team, TeamAction.SET_NOTICE)) {
            return;
        }
        if (!plugin.getConfigManager().isNoticeEnabled()) {
            msg(player, "settings.notice.disabled", null);
            return;
        }
        int max = plugin.getConfigManager().getNoticeMaxLength();
        if (value.isEmpty()) {
            TeamSettingChangeEvent noticeEvent = new TeamSettingChangeEvent(team,
                    TeamSettingChangeEvent.SettingType.NOTICE, team.getSettings().getNotice(), "", player);
            if (!callEvent(noticeEvent, player)) {
                return;
            }
            team.getSettings().setNotice("");
            plugin.getTeamManager().save(team);
            msg(player, "settings.notice.cleared", null);
            return;
        }
        if (value.length() > max) {
            msg(player, "settings.notice.too-long", Placeholders.of("max", max).build());
            return;
        }
        TeamSettingChangeEvent noticeEvent = new TeamSettingChangeEvent(team,
                TeamSettingChangeEvent.SettingType.NOTICE, team.getSettings().getNotice(), value, player);
        if (!callEvent(noticeEvent, player)) {
            return;
        }
        team.getSettings().setNotice(value);
        plugin.getTeamManager().save(team);
        msg(player, "settings.notice.success", null);
        team.broadcast(plugin.getMessageManager().get("settings.notice.broadcast",
                Placeholders.of("notice", value).build()));
    }

    private void setPublic(Player player, Team team, String label, String value) {
        if (!checkAction(player, team, TeamAction.SET_PUBLIC)) {
            return;
        }
        if (!plugin.getConfigManager().isJoinMethodEnabled("public")) {
            msg(player, "settings.public.method-disabled", null);
            return;
        }
        Boolean open = parseBoolean(value);
        if (open == null) {
            msgList(player, "settings.usage", Placeholders.of("label", label).build());
            return;
        }
        TeamSettingChangeEvent pubEvent = new TeamSettingChangeEvent(team,
                TeamSettingChangeEvent.SettingType.PUBLIC,
                String.valueOf(team.getSettings().isOpen()), String.valueOf(open), player);
        if (!callEvent(pubEvent, player)) {
            return;
        }
        team.getSettings().setOpen(open);
        plugin.getTeamManager().save(team);
        if (open) {
            msg(player, "settings.public.enabled",
                    Placeholders.of("team", team.getName()).and("label", label).build());
        } else {
            msg(player, "settings.public.disabled", null);
        }
    }

    private void setPassword(Player player, Team team, String label, String value) {
        if (!checkAction(player, team, TeamAction.SET_PASSWORD)) {
            return;
        }
        if (!plugin.getConfigManager().isJoinMethodEnabled("password")) {
            msg(player, "settings.password.method-disabled", null);
            return;
        }
        if (value.isEmpty() || value.equalsIgnoreCase("off")) {
            TeamSettingChangeEvent pwEvent = new TeamSettingChangeEvent(team,
                    TeamSettingChangeEvent.SettingType.PASSWORD,
                    team.getSettings().getPassword() == null ? "" : team.getSettings().getPassword(),
                    "", player);
            if (!callEvent(pwEvent, player)) {
                return;
            }
            team.getSettings().setPassword(null);
            plugin.getTeamManager().save(team);
            msg(player, "settings.password.cleared", null);
            return;
        }
        int min = plugin.getConfigManager().getPasswordMinLength();
        int max = plugin.getConfigManager().getPasswordMaxLength();
        if (value.length() < min) {
            msg(player, "settings.password.too-short", Placeholders.of("min", min).build());
            return;
        }
        if (value.length() > max) {
            msg(player, "settings.password.too-long", Placeholders.of("max", max).build());
            return;
        }
        TeamSettingChangeEvent pwEvent = new TeamSettingChangeEvent(team,
                TeamSettingChangeEvent.SettingType.PASSWORD,
                team.getSettings().getPassword() == null ? "" : team.getSettings().getPassword(),
                value, player);
        if (!callEvent(pwEvent, player)) {
            return;
        }
        team.getSettings().setPassword(value);
        plugin.getTeamManager().save(team);
        msg(player, "settings.password.set", Placeholders.of("password", value).build());
    }

    /**
     * 解析 on/off/true/false/1/0 等布尔文本，无法识别时返回 null。
     */
    private Boolean parseBoolean(String value) {
        if (value == null) {
            return null;
        }
        switch (value.toLowerCase()) {
            case "on":
            case "true":
            case "1":
            case "开":
                return Boolean.TRUE;
            case "off":
            case "false":
            case "0":
            case "关":
                return Boolean.FALSE;
            default:
                return null;
        }
    }
}
