package soys.soyslinkteam;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import soys.soyslinkteam.config.ConfigManager;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.team.TeamMember;
import soys.soyslinkteam.team.TeamRole;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * PlaceholderAPI 变量扩展。
 * <p>提供 %soyslinkteam_*% 系列变量，详见 config.yml 的 placeholder 段说明。</p>
 */
public class PlaceholderHook extends PlaceholderExpansion {

    private final SOYSLinkTeam plugin;

    public PlaceholderHook(SOYSLinkTeam plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "soyslinkteam";
    }

    @Override
    public String getAuthor() {
        return "SOYS";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null || params == null) {
            return null;
        }
        String p = params.toLowerCase();
        ConfigManager cfg = plugin.getConfigManager();
        Team team = plugin.getTeamManager().getLoadedPlayerTeam(player.getUniqueId());

        if (team == null) {
            if ("total_teams".equals(p)) {
                return String.valueOf(plugin.getTeamManager().getTotalCount());
            }
            if ("has_team".equals(p)) {
                return cfg.getPlaceholderFalse();
            }
            return cfg.getPlaceholderNoTeam();
        }

        switch (p) {
            case "has_team":
                return cfg.getPlaceholderTrue();
            case "name":
                return team.getName();
            case "tag":
                return team.getSettings().hasTag()
                        ? team.getSettings().getTag()
                        : cfg.getTagFallback().replace("{name}", team.getName());
            case "id":
                return team.getId().toString();
            case "leader":
                return team.getLeaderName();
            case "role": {
                TeamMember m = team.getMember(player.getUniqueId());
                TeamRole role = m == null ? TeamRole.MEMBER : m.getRole();
                return cfg.getRoleDisplay(role.name(), defaultRole(role));
            }
            case "size":
                return String.valueOf(team.getSize());
            case "max_size":
                return String.valueOf(plugin.getTeamManager().getMaxSize(team));
            case "online":
                return String.valueOf(team.getOnlineCount());
            case "offline":
                return String.valueOf(team.getSize() - team.getOnlineCount());
            case "notice":
                return team.getSettings().hasNotice() ? team.getSettings().getNotice() : "";
            case "public":
                return team.getSettings().isOpen() ? cfg.getPlaceholderTrue() : cfg.getPlaceholderFalse();
            case "locked":
                return team.getSettings().hasPassword() ? cfg.getPlaceholderTrue() : cfg.getPlaceholderFalse();
            case "created":
                return new SimpleDateFormat(cfg.getDateFormat())
                        .format(new java.util.Date(team.getCreatedAt()));
            case "members":
                return joinNames(team, false);
            case "members_online":
                return joinNames(team, true);
            case "is_leader":
                return team.isLeader(player.getUniqueId())
                        ? cfg.getPlaceholderTrue() : cfg.getPlaceholderFalse();
            case "total_teams":
                return String.valueOf(plugin.getTeamManager().getTotalCount());
            default:
                return null;
        }
    }

    private String defaultRole(TeamRole role) {
        switch (role) {
            case LEADER:
                return "队长";
            case ADMIN:
                return "副队长";
            default:
                return "队员";
        }
    }

    private String joinNames(Team team, boolean onlineOnly) {
        ConfigManager cfg = plugin.getConfigManager();
        int limit = cfg.getPlaceholderListLimit();
        String sep = cfg.getPlaceholderListSeparator();
        List<String> names = new ArrayList<>();
        for (TeamMember m : team.getSortedMembers()) {
            if (onlineOnly && !m.isOnline()) {
                continue;
            }
            names.add(m.getName());
        }
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (String n : names) {
            if (limit > 0 && shown >= limit) {
                sb.append(sep).append("...");
                break;
            }
            if (sb.length() > 0) {
                sb.append(sep);
            }
            sb.append(n);
            shown++;
        }
        return sb.length() == 0 ? cfg.getPlaceholderNoTeam() : sb.toString();
    }
}
