package soys.soyslinkteam.join;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import soys.soyslinkteam.SOYSLinkTeam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 邀请状态管理器。
 * <p>
 * 邀请是纯内存状态，不参与持久化：重启后所有待处理邀请自动失效，
 * 这符合"邀请具有时效性"的语义，也避免了无谓的存储开销。
 * </p>
 */
public class InviteManager {

    private final SOYSLinkTeam plugin;

    /** 被邀请者 UUID -> (队伍 ID -> 邀请) */
    private final Map<UUID, Map<UUID, Invite>> pending = new ConcurrentHashMap<>();

    /** 「队伍 ID + 目标 UUID」-> 上次发送时间，用于重复邀请冷却 */
    private final Map<String, Long> resendCooldown = new ConcurrentHashMap<>();

    private BukkitTask cleanupTask;

    public InviteManager(SOYSLinkTeam plugin) {
        this.plugin = plugin;
    }

    /**
     * 启动过期邀请清理任务（每 5 秒一次）。
     */
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
        resendCooldown.clear();
    }

    // ================================================================
    //  操作
    // ================================================================

    /**
     * 登记一条邀请。
     */
    public void add(Invite invite) {
        pending.computeIfAbsent(invite.getTarget(), key -> new ConcurrentHashMap<>())
                .put(invite.getTeamId(), invite);
        resendCooldown.put(cooldownKey(invite.getTeamId(), invite.getTarget()),
                System.currentTimeMillis());
    }

    /**
     * 获取指定队伍对指定玩家的邀请，不存在或已过期时返回 null。
     */
    public Invite get(UUID target, UUID teamId) {
        Map<UUID, Invite> invites = pending.get(target);
        if (invites == null) {
            return null;
        }
        Invite invite = invites.get(teamId);
        if (invite == null) {
            return null;
        }
        if (invite.isExpired()) {
            invites.remove(teamId);
            return null;
        }
        return invite;
    }

    /**
     * 获取玩家全部有效邀请。
     */
    public List<Invite> getPending(UUID target) {
        Map<UUID, Invite> invites = pending.get(target);
        if (invites == null || invites.isEmpty()) {
            return Collections.emptyList();
        }
        List<Invite> result = new ArrayList<>();
        Iterator<Map.Entry<UUID, Invite>> iterator = invites.entrySet().iterator();
        while (iterator.hasNext()) {
            Invite invite = iterator.next().getValue();
            if (invite.isExpired()) {
                iterator.remove();
            } else {
                result.add(invite);
            }
        }
        return result;
    }

    /**
     * 玩家当前的有效邀请数量。
     */
    public int countPending(UUID target) {
        return getPending(target).size();
    }

    /**
     * 移除一条邀请。
     */
    public Invite remove(UUID target, UUID teamId) {
        Map<UUID, Invite> invites = pending.get(target);
        if (invites == null) {
            return null;
        }
        Invite removed = invites.remove(teamId);
        if (invites.isEmpty()) {
            pending.remove(target);
        }
        return removed;
    }

    /**
     * 清除玩家的全部邀请（入队或退服时调用）。
     */
    public void removeAll(UUID target) {
        pending.remove(target);
    }

    /**
     * 清除某支队伍发出的全部邀请（队伍解散时调用）。
     */
    public void removeByTeam(UUID teamId) {
        for (Map<UUID, Invite> invites : pending.values()) {
            invites.remove(teamId);
        }
        pending.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    /**
     * 重复邀请冷却剩余秒数，0 表示可以发送。
     */
    public int getResendCooldown(UUID teamId, UUID target, int cooldownSeconds) {
        if (cooldownSeconds <= 0) {
            return 0;
        }
        Long last = resendCooldown.get(cooldownKey(teamId, target));
        if (last == null) {
            return 0;
        }
        long elapsed = System.currentTimeMillis() - last;
        long remaining = cooldownSeconds * 1000L - elapsed;
        return remaining <= 0 ? 0 : (int) Math.ceil(remaining / 1000.0);
    }

    private String cooldownKey(UUID teamId, UUID target) {
        return teamId + ":" + target;
    }

    // ================================================================
    //  清理
    // ================================================================

    /**
     * 移除全部过期邀请，并通知在线的被邀请者。
     */
    private void cleanup() {
        long now = System.currentTimeMillis();

        Iterator<Map.Entry<UUID, Map<UUID, Invite>>> outer = pending.entrySet().iterator();
        while (outer.hasNext()) {
            Map.Entry<UUID, Map<UUID, Invite>> entry = outer.next();
            Map<UUID, Invite> invites = entry.getValue();

            Iterator<Map.Entry<UUID, Invite>> inner = invites.entrySet().iterator();
            while (inner.hasNext()) {
                Invite invite = inner.next().getValue();
                if (!invite.isExpired()) {
                    continue;
                }
                inner.remove();
                org.bukkit.entity.Player target = Bukkit.getPlayer(invite.getTarget());
                if (target != null && target.isOnline()) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("team", invite.getTeamName());
                    plugin.getMessageManager().sendList(target, "join.invite.expired", placeholders);
                }
            }
            if (invites.isEmpty()) {
                outer.remove();
            }
        }

        // 清理过期的冷却记录，避免无限增长
        resendCooldown.entrySet().removeIf(entry -> now - entry.getValue() > 600_000L);
    }
}
