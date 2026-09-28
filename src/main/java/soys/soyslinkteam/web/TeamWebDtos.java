package soys.soyslinkteam.web;

import org.bukkit.potion.PotionEffectType;
import soys.soyslinkteam.application.JoinApplication;
import soys.soyslinkteam.buff.TeamBuff;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.team.TeamMember;
import soys.soyslinkteam.web.WebLogManager.LogEntry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 领域对象 → 前端 JSON 结构（Map）转换工具。
 * <p>集中收敛序列化逻辑，保证 API 返回结构稳定、字段命名统一。</p>
 */
public final class TeamWebDtos {

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private TeamWebDtos() {
    }

    // ===== 队伍 =====

    public static Map<String, Object> teamSummary(Team team, int maxSize, int buffCount, int applicationCount) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", team.getId().toString());
        map.put("name", team.getName());
        map.put("leader", team.getLeader() == null ? null : team.getLeader().toString());
        map.put("leaderName", team.getLeaderName());
        map.put("size", team.getSize());
        map.put("onlineCount", team.getOnlineCount());
        map.put("maxSize", maxSize);
        map.put("open", team.getSettings().isOpen());
        map.put("hasPassword", team.getSettings().hasPassword());
        map.put("hasCost", team.getSettings().hasEconomyCost());
        map.put("cost", team.getSettings().getEconomyCost());
        map.put("tag", team.getSettings().getTag());
        map.put("buffCount", buffCount);
        map.put("applicationCount", applicationCount);
        map.put("createdAt", team.getCreatedAt());
        map.put("createdAtText", DATE_FMT.format(new Date(team.getCreatedAt())));
        return map;
    }

    public static Map<String, Object> teamDetail(Team team, int maxSize, List<TeamBuff> buffs) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", team.getId().toString());
        map.put("name", team.getName());
        map.put("leader", team.getLeader() == null ? null : team.getLeader().toString());
        map.put("leaderName", team.getLeaderName());
        map.put("size", team.getSize());
        map.put("onlineCount", team.getOnlineCount());
        map.put("maxSize", maxSize);
        map.put("createdAt", team.getCreatedAt());
        map.put("createdAtText", DATE_FMT.format(new Date(team.getCreatedAt())));
        map.put("lastActiveAt", team.getLastActiveAt());
        map.put("lastActiveText", DATE_FMT.format(new Date(team.getLastActiveAt())));

        // 设置
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("open", team.getSettings().isOpen());
        settings.put("hasPassword", team.getSettings().hasPassword());
        settings.put("notice", team.getSettings().getNotice());
        settings.put("tag", team.getSettings().getTag());
        settings.put("cost", team.getSettings().getEconomyCost());
        settings.put("hasCost", team.getSettings().hasEconomyCost());
        map.put("settings", settings);

        // 成员
        List<Map<String, Object>> members = new ArrayList<>();
        for (TeamMember member : team.getSortedMembers()) {
            members.add(member(member));
        }
        map.put("members", members);

        // 增幅
        List<Map<String, Object>> buffList = new ArrayList<>();
        for (TeamBuff buff : buffs) {
            buffList.add(buff(buff));
        }
        map.put("buffs", buffList);
        return map;
    }

    // ===== 成员 =====

    public static Map<String, Object> member(TeamMember member) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("uuid", member.getUuid().toString());
        map.put("name", member.getName());
        map.put("role", member.getRole().name());
        map.put("roleDisplay", roleDisplay(member.getRole().name()));
        map.put("online", member.isOnline());
        map.put("joinedAt", member.getJoinedAt());
        map.put("joinedAtText", DATE_FMT.format(new Date(member.getJoinedAt())));
        map.put("lastSeen", member.getLastSeen());
        map.put("chatChannel", member.isChatChannel());
        return map;
    }

    public static String roleDisplay(String role) {
        if (role == null) {
            return "成员";
        }
        switch (role) {
            case "LEADER":
                return "队长";
            case "ADMIN":
                return "副队长";
            default:
                return "成员";
        }
    }

    // ===== 增幅 =====

    public static Map<String, Object> buff(TeamBuff buff) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", buff.getId());
        map.put("kind", buff.getKind().getId());
        map.put("kindDisplay", buff.getKind().getDisplayName());
        map.put("effectKey", buff.getEffectKey());
        map.put("displayName", buff.getDisplayName() != null ? buff.getDisplayName()
                : potionDisplayName(buff.getEffectKey()));
        map.put("amplifier", buff.getAmplifier());
        map.put("level", buff.getLevel());
        map.put("levelDisplay", romanLevel(buff.getLevel()));
        map.put("duration", buff.getDurationSeconds());
        map.put("remaining", buff.getRemainingSeconds());
        map.put("remainingText", formatDuration(buff.getRemainingSeconds()));
        map.put("startedAt", buff.getStartedAt());
        map.put("grantedBy", buff.getGrantedBy());
        return map;
    }

    /**
     * 药水效果的显示名（优先 Bukkit 名称，去除下划线）。
     */
    public static String potionDisplayName(String effectKey) {
        PotionEffectType type = PotionEffectType.getByName(effectKey);
        if (type == null) {
            return effectKey;
        }
        String name = type.getName();
        return name == null ? effectKey : name.replace('_', ' ');
    }

    public static String romanLevel(int level) {
        switch (level) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            case 5: return "V";
            case 6: return "VI";
            case 7: return "VII";
            case 8: return "VIII";
            case 9: return "IX";
            case 10: return "X";
            default: return String.valueOf(level);
        }
    }

    /**
     * 秒数 → 可读时长（如 1天2小时 / 3分20秒）。
     */
    public static String formatDuration(long seconds) {
        if (seconds <= 0) {
            return "已结束";
        }
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("天");
        }
        if (hours > 0) {
            sb.append(hours).append("小时");
        }
        if (minutes > 0 && days == 0) {
            sb.append(minutes).append("分");
        }
        if (secs > 0 && days == 0 && hours == 0) {
            sb.append(secs).append("秒");
        }
        return sb.length() == 0 ? "不足1秒" : sb.toString();
    }

    // ===== 申请 =====

    public static Map<String, Object> application(JoinApplication app) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("teamId", app.getTeamId().toString());
        map.put("teamName", app.getTeamName());
        map.put("applicant", app.getApplicant().toString());
        map.put("applicantName", app.getApplicantName());
        map.put("message", app.getMessage());
        map.put("createdAt", app.getCreatedAt());
        map.put("createdAtText", DATE_FMT.format(new Date(app.getCreatedAt())));
        map.put("remaining", app.getRemainingSeconds());
        org.bukkit.entity.Player online = org.bukkit.Bukkit.getPlayer(app.getApplicant());
        map.put("online", online != null && online.isOnline());
        return map;
    }

    // ===== 日志 =====

    public static Map<String, Object> log(LogEntry entry) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("time", entry.getTime());
        map.put("timeText", entry.getTimeText());
        map.put("level", entry.getLevel());
        map.put("levelDisplay", entry.getLevel());
        map.put("operator", entry.getOperator());
        map.put("action", entry.getAction());
        map.put("detail", entry.getDetail());
        map.put("ip", entry.getIp());
        return map;
    }
}
