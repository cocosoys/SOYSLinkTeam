package soys.soyslinkteam.team;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.scheduler.BukkitTask;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.config.ConfigManager;
import soys.soyslinkteam.event.TeamCreateEvent;
import soys.soyslinkteam.event.TeamDisbandEvent;
import soys.soyslinkteam.event.TeamTransferEvent;
import soys.soyslinkteam.team.TeamMember;
import soys.soyslinkteam.util.Placeholders;
import soys.soyslinkteam.util.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * 队伍管理器：内存激活池 + 常驻索引。
 *
 * <h3>内存模型</h3>
 * <ul>
 *   <li><b>激活池</b> {@code activeTeams}：只有"正在被使用"的队伍常驻内存。</li>
 *   <li><b>玩家索引</b> {@code playerIndex}：玩家 UUID -&gt; 队伍 ID 的全量轻量映射，
 *       使插件无需装载队伍即可判断玩家归属，是避免频繁查库的关键。</li>
 *   <li><b>名称索引</b> {@code teamNames}：队伍 ID -&gt; 名称，用于重名校验与队伍列表。</li>
 * </ul>
 *
 * <h3>卸载策略</h3>
 * 定时任务扫描激活池，队伍在 {@code storage.memory.active-timeout} 分钟内无任何活动时，
 * 写回存储并从内存移除。若 {@code keep-loaded-while-online} 为 true，
 * 只要还有成员在线就持续续期，避免在线玩家频繁触发装卸。
 */
public class TeamManager {

    private static final String SIZE_PERMISSION_PREFIX = "soyslinkteam.size.";

    private final SOYSLinkTeam plugin;

    /** 激活池：队伍 ID -> 队伍实例 */
    private final Map<UUID, Team> activeTeams = new ConcurrentHashMap<>();

    /** 常驻索引：玩家 UUID -> 队伍 ID */
    private final Map<UUID, UUID> playerIndex = new ConcurrentHashMap<>();

    /** 常驻索引：队伍 ID -> 队伍名称 */
    private final Map<UUID, String> teamNames = new ConcurrentHashMap<>();

    private BukkitTask unloadTask;
    private BukkitTask autoSaveTask;
    private BukkitTask autoTransferTask;

    public TeamManager(SOYSLinkTeam plugin) {
        this.plugin = plugin;
    }

    // ================================================================
    //  生命周期
    // ================================================================

    /**
     * 从主存储装载常驻索引并启动后台任务。
     */
    public void initialize() {
        reloadIndexes();
        startTasks();
    }

    /**
     * 重新从主存储装载索引（重载配置时使用）。
     */
    public void reloadIndexes() {
        playerIndex.clear();
        teamNames.clear();
        try {
            Map<UUID, UUID> index = plugin.getStorageManager().loadPlayerIndex();
            playerIndex.putAll(index);
            Map<UUID, String> names = plugin.getStorageManager().loadTeamNames();
            teamNames.putAll(names);
            plugin.getLogger().info("已装载索引: " + names.size() + " 支队伍，"
                    + index.size() + " 名玩家");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "装载队伍索引失败: " + e.getMessage(), e);
        }
    }

    /**
     * 启动卸载与自动保存任务。
     */
    public void startTasks() {
        stopTasks();
        ConfigManager config = plugin.getConfigManager();

        long checkInterval = config.getCheckIntervalTicks();
        this.unloadTask = Bukkit.getScheduler().runTaskTimer(plugin,
                this::tickUnload, checkInterval, checkInterval);

        long autoSave = config.getAutoSaveIntervalTicks();
        if (autoSave > 0) {
            this.autoSaveTask = Bukkit.getScheduler().runTaskTimer(plugin,
                    this::saveDirtyTeams, autoSave, autoSave);
        }

        // 队长离线自动转让：复用检查间隔
        this.autoTransferTask = Bukkit.getScheduler().runTaskTimer(plugin,
                this::tickAutoTransfer, checkInterval, checkInterval);
    }

    public void stopTasks() {
        if (unloadTask != null) {
            unloadTask.cancel();
            unloadTask = null;
        }
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
        if (autoTransferTask != null) {
            autoTransferTask.cancel();
            autoTransferTask = null;
        }
    }

    /**
     * 关服流程：停止任务并把内存中的队伍全部落盘。
     */
    public void shutdown() {
        stopTasks();
        List<Team> pending = new ArrayList<>();
        for (Team team : activeTeams.values()) {
            if (!team.isDisbanded()) {
                pending.add(team);
            }
        }
        if (!pending.isEmpty()) {
            plugin.getLogger().info("正在保存 " + pending.size() + " 支内存中的队伍...");
            plugin.getStorageManager().saveTeamsBlocking(pending);
        }
        activeTeams.clear();
    }

    // ================================================================
    //  卸载与保存
    // ================================================================

    /**
     * 扫描激活池，卸载空闲队伍。
     */
    private void tickUnload() {
        long timeout = plugin.getConfigManager().getActiveTimeoutMillis();
        boolean keepOnline = plugin.getConfigManager().isKeepLoadedWhileOnline();

        List<Team> toUnload = new ArrayList<>();
        for (Team team : activeTeams.values()) {
            if (team.isDisbanded()) {
                toUnload.add(team);
                continue;
            }
            if (keepOnline && team.hasOnlineMember()) {
                team.touch();
                continue;
            }
            if (team.isIdle(timeout)) {
                toUnload.add(team);
            }
        }
        for (Team team : toUnload) {
            unload(team);
        }
    }

    /**
     * 把队伍写回存储并移出内存。
     */
    public void unload(Team team) {
        if (team == null) {
            return;
        }
        activeTeams.remove(team.getId());
        if (!team.isDisbanded() && team.isDirty()) {
            plugin.getStorageManager().saveTeamAsync(team);
        }
        debug("已卸载队伍 " + team.getName());
    }

    /**
     * 保存所有有改动的队伍。
     */
    public void saveDirtyTeams() {
        List<Team> dirty = new ArrayList<>();
        for (Team team : activeTeams.values()) {
            if (!team.isDisbanded() && team.isDirty()) {
                dirty.add(team);
            }
        }
        if (dirty.isEmpty()) {
            return;
        }
        plugin.getStorageManager().submit(
                () -> plugin.getStorageManager().saveTeamsBlocking(dirty));
    }

    /**
     * 立即保存全部内存队伍，返回队伍数量。
     */
    public int saveAll() {
        List<Team> pending = new ArrayList<>();
        for (Team team : activeTeams.values()) {
            if (!team.isDisbanded()) {
                pending.add(team);
            }
        }
        if (!pending.isEmpty()) {
            plugin.getStorageManager().submit(
                    () -> plugin.getStorageManager().saveTeamsBlocking(pending));
        }
        return pending.size();
    }

    // ================================================================
    //  查询 - 内存
    // ================================================================

    /**
     * 从激活池获取队伍，未装载时返回 null。
     */
    public Team getLoadedTeam(UUID teamId) {
        return teamId == null ? null : activeTeams.get(teamId);
    }

    /**
     * 从激活池获取玩家所在队伍，未装载时返回 null。
     */
    public Team getLoadedPlayerTeam(UUID playerId) {
        UUID teamId = playerIndex.get(playerId);
        return teamId == null ? null : activeTeams.get(teamId);
    }

    /**
     * 玩家是否属于某支队伍（无需装载队伍即可判断）。
     */
    public boolean hasTeam(UUID playerId) {
        return playerIndex.containsKey(playerId);
    }

    public UUID getPlayerTeamId(UUID playerId) {
        return playerIndex.get(playerId);
    }

    /**
     * 按名称查找队伍 ID（忽略大小写，取决于配置）。
     */
    public UUID getTeamIdByName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        boolean caseSensitive = plugin.getConfigManager().isNameCaseSensitive();
        for (Map.Entry<UUID, String> entry : teamNames.entrySet()) {
            String value = entry.getValue();
            if (value == null) {
                continue;
            }
            if (caseSensitive ? value.equals(name) : value.equalsIgnoreCase(name)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * 队伍名是否已被占用。
     */
    public boolean isNameTaken(String name) {
        return getTeamIdByName(name) != null;
    }

    public Collection<Team> getLoadedTeams() {
        return Collections.unmodifiableCollection(activeTeams.values());
    }

    public int getLoadedCount() {
        return activeTeams.size();
    }

    public int getTotalCount() {
        return teamNames.size();
    }

    /**
     * 全部队伍的「ID -&gt; 名称」快照，用于列表与 Tab 补全。
     */
    public Map<UUID, String> getTeamNameIndex() {
        return Collections.unmodifiableMap(teamNames);
    }

    // ================================================================
    //  查询 - 异步装载
    // ================================================================

    /**
     * 获取队伍，未装载时异步从存储读取。回调始终在主线程执行。
     */
    public void getTeamAsync(UUID teamId, Consumer<Team> callback) {
        if (teamId == null) {
            callback.accept(null);
            return;
        }
        Team loaded = activeTeams.get(teamId);
        if (loaded != null) {
            loaded.touch();
            callback.accept(loaded);
            return;
        }
        if (!teamNames.containsKey(teamId)) {
            callback.accept(null);
            return;
        }
        plugin.getStorageManager().loadTeamAsync(teamId, team -> {
            if (team == null) {
                // 索引与存储不一致，清理脏索引
                teamNames.remove(teamId);
                callback.accept(null);
                return;
            }
            Team existing = activeTeams.putIfAbsent(teamId, team);
            Team result = existing != null ? existing : team;
            result.touch();
            debug("已装载队伍 " + result.getName());
            callback.accept(result);
        });
    }

    /**
     * 获取玩家所在队伍，未装载时异步读取。
     */
    public void getPlayerTeamAsync(UUID playerId, Consumer<Team> callback) {
        UUID teamId = playerIndex.get(playerId);
        if (teamId == null) {
            callback.accept(null);
            return;
        }
        getTeamAsync(teamId, callback);
    }

    /**
     * 按名称获取队伍，未装载时异步读取。
     */
    public void getTeamByNameAsync(String name, Consumer<Team> callback) {
        getTeamAsync(getTeamIdByName(name), callback);
    }

    /**
     * 直接把一支队伍放入激活池（用于新建队伍）。
     */
    public void activate(Team team) {
        activeTeams.put(team.getId(), team);
        team.touch();
    }

    // ================================================================
    //  业务操作
    // ================================================================

    /**
     * 创建队伍。调用前应已完成名称与冷却校验。
     */
    public Team createTeam(String name, Player leader) {
        Team team = Team.create(name, leader);
        if (plugin.getConfigManager().isJoinMethodEnabled("public")) {
            boolean defaultPublic = plugin.getConfigManager()
                    .getJoinMethodSection("public") != null
                    && plugin.getConfigManager().getJoinMethodSection("public")
                    .getBoolean("default-public", false);
            team.getSettings().setOpen(defaultPublic);
        }
        TeamCreateEvent createEvent = new TeamCreateEvent(team, leader);
        Bukkit.getPluginManager().callEvent(createEvent);
        if (createEvent.isCancelled()) {
            return null;
        }

        activate(team);
        teamNames.put(team.getId(), name);
        playerIndex.put(leader.getUniqueId(), team.getId());
        plugin.getStorageManager().saveTeamAsync(team);
        return team;
    }

    /**
     * 解散队伍：清索引、从内存移除、从存储删除。
     *
     * @param actor 触发解散的操作者（玩家 / 管理员），系统行为为 null
     * @return 是否真正执行了解散（被监听器取消时返回 false）
     */
    public boolean disbandTeam(Team team, Player actor) {
        if (team == null) {
            return false;
        }
        TeamDisbandEvent event = new TeamDisbandEvent(team, actor);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        team.markDisbanded();
        for (TeamMember member : team.getMembers()) {
            playerIndex.remove(member.getUuid());
        }
        teamNames.remove(team.getId());
        activeTeams.remove(team.getId());
        plugin.getStorageManager().deleteTeamAsync(team.getId());
        debug("已解散队伍 " + team.getName());
        return true;
    }

    /**
     * 将玩家加入队伍并维护索引。
     */
    public void addMember(Team team, Player player, TeamRole role) {
        team.addMember(TeamMember.of(player, role));
        playerIndex.put(player.getUniqueId(), team.getId());
        plugin.getStorageManager().saveTeamAsync(team);
    }

    /**
     * 将成员移出队伍并维护索引。
     *
     * @return 被移出的成员，不存在时返回 null
     */
    public TeamMember removeMember(Team team, UUID playerId) {
        TeamMember removed = team.removeMember(playerId);
        if (removed != null) {
            playerIndex.remove(playerId);
            // 离开队伍即关闭其队伍频道开关，避免残留定向频道
            removed.setChatChannel(false);
            if (team.getSize() == 0 && plugin.getConfigManager().isDisbandWhenEmpty()) {
                // 最后一名成员离开且开启了自动解散：解散空队伍
                autoDisbandEmpty(team, removed);
            } else {
                plugin.getStorageManager().saveTeamAsync(team);
            }
        }
        return removed;
    }

    /**
     * 空队伍自动解散：最后一名成员离开且 {@code team.behavior.disband-when-empty} 开启时调用。
     * 触发 {@link TeamDisbandEvent}（系统行为，actor 为 null），并通知被移除的最后成员。
     */
    private void autoDisbandEmpty(Team team, TeamMember removed) {
        TeamDisbandEvent event = new TeamDisbandEvent(team, null);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            // 监听器拒绝解散：保留空队伍
            plugin.getStorageManager().saveTeamAsync(team);
            return;
        }
        String name = team.getName();
        team.markDisbanded();
        for (TeamMember member : team.getMembers()) {
            playerIndex.remove(member.getUuid());
        }
        teamNames.remove(team.getId());
        activeTeams.remove(team.getId());
        plugin.getStorageManager().deleteTeamAsync(team.getId());

        Player removedPlayer = removed.getPlayer();
        if (removedPlayer != null && removedPlayer.isOnline()) {
            plugin.getMessageManager().send(removedPlayer, "team.disband.empty",
                    Placeholders.of("team", name).build());
        }
        debug("空队伍 " + name + " 因最后一名成员离开而自动解散");
    }

    /**
     * 队长离线自动转让：当队长离线超过 {@code team.behavior.auto-transfer-offline} 分钟时，
     * 将队长自动转让给继任者（pickSuccessor）。本方法幂等，可在周期任务与玩家上线时安全调用。
     */
    public void maybeAutoTransfer(Team team) {
        int minutes = plugin.getConfigManager().getAutoTransferOfflineMinutes();
        if (minutes <= 0 || team == null || team.isDisbanded()) {
            return;
        }
        TeamMember leader = team.getLeaderMember();
        if (leader == null || leader.isOnline()) {
            return;
        }
        long offlineMillis = System.currentTimeMillis() - leader.getLastSeen();
        if (offlineMillis < (long) minutes * 60_000L) {
            return;
        }
        TeamMember successor = team.pickSuccessor(leader.getUuid());
        if (successor == null) {
            // 无人可继任：保留有成员的队伍，避免误解散
            return;
        }
        TeamTransferEvent transferEvent = new TeamTransferEvent(
                team, leader.getPlayer(), successor.getPlayer(), null);
        Bukkit.getPluginManager().callEvent(transferEvent);
        if (transferEvent.isCancelled()) {
            return;
        }
        team.transferLeader(successor.getUuid());
        this.save(team);

        team.broadcast(plugin.getMessageManager().get("member.transfer.broadcast",
                Placeholders.of("player", successor.getName()).build()));
        team.broadcast(plugin.getMessageManager().get("team.disband.autotransfer.notice",
                Placeholders.of("old", leader.getName()).and("player", successor.getName()).build()));

        Player newLeader = successor.getPlayer();
        if (newLeader != null && newLeader.isOnline()) {
            plugin.getMessageManager().send(newLeader, "member.transfer.received",
                    Placeholders.of("team", team.getName()).build());
        }
        debug("队长 " + leader.getName() + " 离线超过 " + minutes + " 分钟，队伍 " + team.getName()
                + " 已自动转让队长给 " + successor.getName());
    }

    /**
     * 周期扫描激活池，对队长离线超时的队伍执行自动转让。
     */
    private void tickAutoTransfer() {
        if (plugin.getConfigManager().getAutoTransferOfflineMinutes() <= 0) {
            return;
        }
        for (Team team : activeTeams.values()) {
            maybeAutoTransfer(team);
        }
    }

    /**
     * 修改队伍名称并同步名称索引。
     */
    public void renameTeam(Team team, String newName) {
        team.setName(newName);
        teamNames.put(team.getId(), newName);
        plugin.getStorageManager().saveTeamAsync(team);
    }

    /**
     * 标记队伍已变更并异步落盘。
     */
    public void save(Team team) {
        if (team == null || team.isDisbanded()) {
            return;
        }
        team.markDirty();
        team.touch();
        plugin.getStorageManager().saveTeamAsync(team);
    }

    // ================================================================
    //  人数上限
    // ================================================================

    /**
     * 计算队伍的人数上限。
     * <p>
     * 取 {@code team.size.default-max} 与队长的 {@code soyslinkteam.size.<n>} 权限中的较大值，
     * 并受 {@code team.size.hard-max} 硬上限约束。队长离线时退化为默认值。
     * </p>
     */
    public int getMaxSize(Team team) {
        ConfigManager config = plugin.getConfigManager();
        int max = config.getDefaultMaxSize();
        if (config.isFollowLeaderPermission() && team != null && team.getLeader() != null) {
            Player leader = Bukkit.getPlayer(team.getLeader());
            if (leader != null && leader.isOnline()) {
                max = Math.max(max, getPermissionSize(leader));
            }
        }
        return Math.min(max, config.getHardMaxSize());
    }

    /**
     * 读取玩家 {@code soyslinkteam.size.<n>} 权限中的最大值，无匹配时返回 0。
     */
    public int getPermissionSize(Player player) {
        int best = 0;
        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            if (!info.getValue()) {
                continue;
            }
            String permission = info.getPermission().toLowerCase();
            if (!permission.startsWith(SIZE_PERMISSION_PREFIX)) {
                continue;
            }
            int value = Text.parseInt(permission.substring(SIZE_PERMISSION_PREFIX.length()), -1);
            if (value > best) {
                best = value;
            }
        }
        return best;
    }

    /**
     * 队伍是否已满。
     */
    public boolean isFull(Team team) {
        return team.getSize() >= getMaxSize(team);
    }

    private void debug(String message) {
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("[队伍] " + message);
        }
    }
}
