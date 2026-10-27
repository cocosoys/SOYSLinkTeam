package soys.soyslinkteam.spring.support;

import org.bukkit.Bukkit;
import org.bukkit.potion.PotionEffectType;
import soys.soyslinkteam.application.JoinApplication;
import soys.soyslinkteam.buff.TeamBuff;
import soys.soyslinkteam.spring.entity.vo.ApplicationVO;
import soys.soyslinkteam.spring.entity.vo.BuffVO;
import soys.soyslinkteam.spring.entity.vo.LogVO;
import soys.soyslinkteam.spring.entity.vo.MemberVO;
import soys.soyslinkteam.spring.entity.vo.TeamDetailVO;
import soys.soyslinkteam.spring.entity.vo.TeamRowVO;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.team.TeamMember;
import soys.soyslinkteam.web.WebLogManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 领域对象 → ERP 视图对象（VO）装配器。
 *
 * <p>集中收敛序列化逻辑，替代原 {@code TeamWebDtos} 的 Map 手撕；
 * 仅在 service 实现层使用，controller 不感知。</p>
 */
public final class TeamVoAssembler {

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private TeamVoAssembler() {
    }

    public static String formatTime(long millis) {
        if (millis <= 0) {
            return "";
        }
        return DATE_FMT.format(new Date(millis));
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

    public static String potionDisplayName(String effectKey) {
        PotionEffectType type = PotionEffectType.getByName(effectKey);
        if (type == null) {
            return effectKey;
        }
        String name = type.getName();
        return name == null ? effectKey : name.replace('_', ' ');
    }

    public static TeamRowVO toRow(Team team, int maxSize, int buffCount, int appCount) {
        TeamRowVO v = new TeamRowVO();
        v.setId(team.getId().toString());
        v.setName(team.getName());
        v.setLeader(team.getLeader() == null ? null : team.getLeader().toString());
        v.setLeaderName(team.getLeaderName());
        v.setSize(team.getSize());
        v.setOnlineCount(team.getOnlineCount());
        v.setMaxSize(maxSize);
        v.setOpen(team.getSettings().isOpen());
        v.setHasPassword(team.getSettings().hasPassword());
        v.setHasCost(team.getSettings().hasEconomyCost());
        v.setCost(team.getSettings().getEconomyCost());
        v.setTag(team.getSettings().getTag());
        v.setBuffCount(buffCount);
        v.setApplicationCount(appCount);
        v.setCreatedAt(team.getCreatedAt());
        v.setCreatedAtText(formatTime(team.getCreatedAt()));
        v.setLoaded(true);
        return v;
    }

    public static TeamDetailVO toDetail(Team team, int maxSize, List<TeamBuff> buffs) {
        TeamDetailVO v = new TeamDetailVO();
        v.setId(team.getId().toString());
        v.setName(team.getName());
        v.setLeader(team.getLeader() == null ? null : team.getLeader().toString());
        v.setLeaderName(team.getLeaderName());
        v.setSize(team.getSize());
        v.setOnlineCount(team.getOnlineCount());
        v.setMaxSize(maxSize);
        v.setCreatedAt(team.getCreatedAt());
        v.setCreatedAtText(formatTime(team.getCreatedAt()));
        v.setLastActiveAt(team.getLastActiveAt());
        v.setLastActiveText(formatTime(team.getLastActiveAt()));

        v.setOpen(team.getSettings().isOpen());
        v.setHasPassword(team.getSettings().hasPassword());
        v.setNotice(team.getSettings().getNotice());
        v.setTag(team.getSettings().getTag());
        v.setCost(team.getSettings().getEconomyCost());
        v.setHasCost(team.getSettings().hasEconomyCost());

        List<MemberVO> members = new ArrayList<>();
        for (TeamMember m : team.getSortedMembers()) {
            members.add(toMember(m));
        }
        v.setMembers(members);

        List<BuffVO> buffList = new ArrayList<>();
        for (TeamBuff b : buffs) {
            buffList.add(toBuff(b));
        }
        v.setBuffs(buffList);
        return v;
    }

    public static MemberVO toMember(TeamMember m) {
        MemberVO v = new MemberVO();
        v.setUuid(m.getUuid().toString());
        v.setName(m.getName());
        v.setRole(m.getRole().name());
        v.setRoleDisplay(roleDisplay(m.getRole().name()));
        v.setOnline(m.isOnline());
        v.setJoinedAt(m.getJoinedAt());
        v.setJoinedAtText(formatTime(m.getJoinedAt()));
        v.setLastSeen(m.getLastSeen());
        v.setChatChannel(m.isChatChannel());
        return v;
    }

    public static BuffVO toBuff(TeamBuff b) {
        BuffVO v = new BuffVO();
        v.setId(b.getId());
        v.setKind(b.getKind().getId());
        v.setKindDisplay(b.getKind().getDisplayName());
        v.setEffectKey(b.getEffectKey());
        v.setDisplayName(b.getDisplayName() != null ? b.getDisplayName() : potionDisplayName(b.getEffectKey()));
        v.setAmplifier(b.getAmplifier());
        v.setLevel(b.getLevel());
        v.setLevelDisplay(romanLevel(b.getLevel()));
        v.setDuration(b.getDurationSeconds());
        v.setRemaining(b.getRemainingSeconds());
        v.setRemainingText(formatDuration(b.getRemainingSeconds()));
        v.setStartedAt(b.getStartedAt());
        v.setGrantedBy(b.getGrantedBy());
        return v;
    }

    public static ApplicationVO toApplication(JoinApplication a) {
        ApplicationVO v = new ApplicationVO();
        v.setTeamId(a.getTeamId().toString());
        v.setTeamName(a.getTeamName());
        v.setApplicant(a.getApplicant().toString());
        v.setApplicantName(a.getApplicantName());
        v.setMessage(a.getMessage());
        v.setCreatedAt(a.getCreatedAt());
        v.setCreatedAtText(formatTime(a.getCreatedAt()));
        v.setRemaining(a.getRemainingSeconds());
        org.bukkit.entity.Player online = Bukkit.getPlayer(a.getApplicant());
        v.setOnline(online != null && online.isOnline());
        return v;
    }

    public static LogVO toLog(WebLogManager.LogEntry e) {
        LogVO v = new LogVO();
        v.setTime(e.getTime());
        v.setTimeText(e.getTimeText());
        v.setLevel(e.getLevel());
        v.setLevelDisplay(e.getLevel());
        v.setOperator(e.getOperator());
        v.setAction(e.getAction());
        v.setDetail(e.getDetail());
        v.setIp(e.getIp());
        return v;
    }
}
