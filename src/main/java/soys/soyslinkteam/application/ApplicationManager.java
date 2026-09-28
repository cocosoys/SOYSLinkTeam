package soys.soyslinkteam.application;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.team.TeamRole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 入队申请管理器。
 *
 * <p>申请是纯内存状态（与邀请一致），具有时效性，重启后自动失效。
 * 玩家对非公开队伍提交申请，队长 / 管理员在网页或游戏内审批。</p>
 */
public class ApplicationManager {

    /** 审批结果，携带可读原因 */
    public enum ApproveResult {
        SUCCESS("已批准入队"),
        NOT_FOUND("申请不存在"),
        ALREADY_PROCESSED("该申请已处理"),
        APPLICANT_OFFLINE("申请人当前离线，无法入队"),
        ALREADY_IN_TEAM("申请人已加入其它队伍"),
        TEAM_FULL("队伍已满"),
        TEAM_MISSING("队伍不存在"),
        ERROR("处理失败");

        private final String message;

        ApproveResult(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    private final SOYSLinkTeam plugin;

    /** 队伍 ID -> 待处理申请列表 */
    private final Map<UUID, List<JoinApplication>> pending = new ConcurrentHashMap<>();

    /** 「申请人 + 队伍」-> 申请，用于查重 */
    private final Map<String, JoinApplication> index = new ConcurrentHashMap<>();

    private BukkitTask cleanupTask;

    public ApplicationManager(SOYSLinkTeam plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        this.cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, this::cleanup, 100L, 100L);
    }

    public void stop() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
    }

    public void clear() {
        pending.clear();
        index.clear();
    }

    // ================================================================
    //  提交申请
    // ================================================================

    /**
     * 提交一条入队申请。
     *
     * @return 新建的申请；重复申请 / 玩家已有队伍时返回 null
     */
    public JoinApplication submit(Team team, Player applicant, String message, long expireMillis) {
        if (team == null || applicant == null) {
            return null;
        }
        if (plugin.getTeamManager().hasTeam(applicant.getUniqueId())) {
            return null;
        }
        String key = key(applicant.getUniqueId(), team.getId());
        if (index.containsKey(key)) {
            return null;
        }
        JoinApplication application = new JoinApplication(team.getId(), team.getName(),
                applicant.getUniqueId(), applicant.getName(), message, expireMillis);
        pending.computeIfAbsent(team.getId(), k -> new CopyOnWriteArrayList<>()).add(application);
        index.put(key, application);
        return application;
    }

    // ================================================================
    //  查询
    // ================================================================

    /**
     * 获取队伍的全部待处理申请（自动剔除过期）。
     */
    public List<JoinApplication> getApplications(UUID teamId) {
        List<JoinApplication> list = pending.get(teamId);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        List<JoinApplication> result = new ArrayList<>();
        Iterator<JoinApplication> iterator = list.iterator();
        while (iterator.hasNext()) {
            JoinApplication app = iterator.next();
            if (app.isExpired() || app.getStatus() != JoinApplication.Status.PENDING) {
                continue;
            }
            result.add(app);
        }
        return result;
    }

    /**
     * 获取玩家提交的全部待处理申请。
     */
    public List<JoinApplication> getMyApplications(UUID applicantId) {
        List<JoinApplication> result = new ArrayList<>();
        for (JoinApplication app : index.values()) {
            if (app.getApplicant().equals(applicantId)
                    && app.getStatus() == JoinApplication.Status.PENDING && !app.isExpired()) {
                result.add(app);
            }
        }
        return result;
    }

    /**
     * 队伍待处理申请数量。
     */
    public int countApplications(UUID teamId) {
        return getApplications(teamId).size();
    }

    // ================================================================
    //  审批
    // ================================================================

    /**
     * 批准申请（可在 worker 线程调用，入队操作自动切主线程）。
     */
    public ApproveResult approve(UUID teamId, UUID applicantId) {
        JoinApplication application = findApplication(teamId, applicantId);
        if (application == null) {
            return ApproveResult.NOT_FOUND;
        }
        if (application.getStatus() != JoinApplication.Status.PENDING) {
            return ApproveResult.ALREADY_PROCESSED;
        }

        Player applicant = Bukkit.getPlayer(applicantId);
        if (applicant == null || !applicant.isOnline()) {
            return ApproveResult.APPLICANT_OFFLINE;
        }
        if (plugin.getTeamManager().hasTeam(applicantId)) {
            return ApproveResult.ALREADY_IN_TEAM;
        }

        Team team = loadTeam(teamId);
        if (team == null || team.isDisbanded()) {
            return ApproveResult.TEAM_MISSING;
        }
        if (team.getSize() >= plugin.getTeamManager().getMaxSize(team)) {
            return ApproveResult.TEAM_FULL;
        }

        application.setStatus(JoinApplication.Status.APPROVED);
        removeApplication(application);

        // 入队操作切到主线程执行
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!plugin.getTeamManager().hasTeam(applicantId)) {
                plugin.getTeamManager().addMember(team, applicant, TeamRole.MEMBER);
                applicant.sendMessage("§a你的入队申请已被批准，欢迎加入 " + team.getName());
            }
        });
        return ApproveResult.SUCCESS;
    }

    /**
     * 拒绝申请。
     */
    public ApproveResult deny(UUID teamId, UUID applicantId, String reason) {
        JoinApplication application = findApplication(teamId, applicantId);
        if (application == null) {
            return ApproveResult.NOT_FOUND;
        }
        if (application.getStatus() != JoinApplication.Status.PENDING) {
            return ApproveResult.ALREADY_PROCESSED;
        }
        application.setStatus(JoinApplication.Status.DENIED);
        removeApplication(application);
        Player applicant = Bukkit.getPlayer(applicantId);
        if (applicant != null && applicant.isOnline()) {
            applicant.sendMessage("§c你加入 " + application.getTeamName() + " 的申请被拒绝"
                    + (reason == null || reason.isEmpty() ? "" : "：" + reason));
        }
        return ApproveResult.SUCCESS;
    }

    /**
     * 玩家撤回自己的申请。
     */
    public boolean cancel(UUID teamId, UUID applicantId) {
        JoinApplication application = findApplication(teamId, applicantId);
        if (application == null) {
            return false;
        }
        removeApplication(application);
        return true;
    }

    public void removeByTeam(UUID teamId) {
        List<JoinApplication> list = pending.remove(teamId);
        if (list != null) {
            for (JoinApplication app : list) {
                index.remove(key(app.getApplicant(), teamId));
            }
        }
    }

    // ================================================================
    //  内部
    // ================================================================

    private JoinApplication findApplication(UUID teamId, UUID applicantId) {
        return index.get(key(applicantId, teamId));
    }

    private void removeApplication(JoinApplication app) {
        index.remove(key(app.getApplicant(), app.getTeamId()));
        List<JoinApplication> list = pending.get(app.getTeamId());
        if (list != null) {
            list.remove(app);
            if (list.isEmpty()) {
                pending.remove(app.getTeamId());
            }
        }
    }

    private Team loadTeam(UUID teamId) {
        Team loaded = plugin.getTeamManager().getLoadedTeam(teamId);
        if (loaded != null) {
            return loaded;
        }
        try {
            return plugin.getStorageManager().loadTeam(teamId);
        } catch (Exception e) {
            return null;
        }
    }

    private String key(UUID applicant, UUID team) {
        return applicant + ":" + team;
    }

    private void cleanup() {
        for (List<JoinApplication> list : pending.values()) {
            Iterator<JoinApplication> iterator = list.iterator();
            while (iterator.hasNext()) {
                JoinApplication app = iterator.next();
                if (app.isExpired()) {
                    index.remove(key(app.getApplicant(), app.getTeamId()));
                    iterator.remove();
                }
            }
        }
        pending.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
}
