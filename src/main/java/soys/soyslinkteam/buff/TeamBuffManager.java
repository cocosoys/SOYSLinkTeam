package soys.soyslinkteam.buff;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.buff.attribute.AttributeBuffProvider;
import soys.soyslinkteam.buff.attribute.CommandAttributeBuffProvider;
import soys.soyslinkteam.team.Team;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 队伍增幅管理器。
 *
 * <p>职责：</p>
 * <ul>
 *   <li>维护各队伍的活跃增幅（药水 / 属性）；</li>
 *   <li>每秒在主线程刷新持续药水（类似信标），过期自动收回；</li>
 *   <li>属性增幅通过 {@link AttributeBuffProvider} 对接第三方属性插件；</li>
 *   <li>玩家上线 / 入队时自动补发其队伍的全部增幅；</li>
 *   <li>持久化到独立的 buffs.yml，重启后按 startedAt 恢复剩余时间。</li>
 * </ul>
 */
public class TeamBuffManager implements Listener {

    /** 药水刷新缓冲（tick），保证每秒刷新时效果不断档 */
    private static final int REFRESH_BUFFER_TICKS = 40;

    private final SOYSLinkTeam plugin;

    /** 队伍 ID -> 活跃增幅列表 */
    private final Map<UUID, List<TeamBuff>> activeBuffs = new ConcurrentHashMap<>();

    /** 待首次应用的新增增幅（瞬间药水 / 属性） */
    private final List<TeamBuff> newlyAdded = new CopyOnWriteArrayList<>();

    /** 待移除的属性增幅（过期 / 手动移除，保证在主线程执行） */
    private final List<TeamBuff> newlyRemoved = new CopyOnWriteArrayList<>();

    /** 上线后需要补发属性增幅的玩家 */
    private final List<UUID> pendingAttributeRefresh = new CopyOnWriteArrayList<>();

    private final Map<String, AttributeBuffProvider> providers = new ConcurrentHashMap<>();

    private File storageFile;
    private BukkitTask tickTask;
    private BukkitTask saveTask;
    private int tickCounter;

    public TeamBuffManager(SOYSLinkTeam plugin) {
        this.plugin = plugin;
    }

    // ================================================================
    //  生命周期
    // ================================================================

    public void initialize() {
        shutdown();
        this.storageFile = new File(plugin.getDataFolder(), "buffs.yml");

        registerProvider(new CommandAttributeBuffProvider(plugin));

        load();

        this.tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
        this.saveTask = Bukkit.getScheduler().runTaskTimer(plugin, this::save, 600L, 600L);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("队伍增幅系统已启用，当前 " + countActiveBuffs() + " 条活跃增幅。");
    }

    public void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        if (saveTask != null) {
            saveTask.cancel();
            saveTask = null;
        }
        save();
    }

    // ================================================================
    //  Provider 注册
    // ================================================================

    public void registerProvider(AttributeBuffProvider provider) {
        if (provider != null) {
            providers.put(provider.getId(), provider);
        }
    }

    public AttributeBuffProvider getProvider(String id) {
        return providers.get(id);
    }

    public Collection<AttributeBuffProvider> getProviders() {
        return Collections.unmodifiableCollection(providers.values());
    }

    // ================================================================
    //  公共操作（线程安全，可由 Web worker 调用）
    // ================================================================

    /**
     * 为队伍添加一条增幅。
     *
     * @return 已创建的增幅；参数非法（未知药水 / 属性）时返回 null
     */
    public TeamBuff addBuff(UUID teamId, BuffKind kind, String effectKey,
                            String displayName, int amplifier, long durationSeconds, String grantedBy) {
        if (teamId == null || kind == null || effectKey == null) {
            return null;
        }
        if (kind == BuffKind.POTION && PotionEffectType.getByName(effectKey) == null) {
            return null;
        }
        TeamBuff buff = TeamBuff.create(kind, effectKey, displayName, amplifier, durationSeconds, grantedBy);
        activeBuffs.computeIfAbsent(teamId, k -> new CopyOnWriteArrayList<>()).add(buff);
        newlyAdded.add(buff);
        save();
        return buff;
    }

    /**
     * 移除队伍的指定增幅。
     */
    public boolean removeBuff(UUID teamId, String buffId) {
        List<TeamBuff> buffs = activeBuffs.get(teamId);
        if (buffs == null) {
            return false;
        }
        TeamBuff target = null;
        for (TeamBuff buff : buffs) {
            if (buff.getId().equals(buffId)) {
                target = buff;
                break;
            }
        }
        if (target == null) {
            return false;
        }
        buffs.remove(target);
        if (target.getKind() == BuffKind.ATTRIBUTE) {
            newlyRemoved.add(target);
        }
        if (buffs.isEmpty()) {
            activeBuffs.remove(teamId);
        }
        save();
        return true;
    }

    /**
     * 获取队伍的活跃增幅（只读快照）。
     */
    public List<TeamBuff> getBuffs(UUID teamId) {
        List<TeamBuff> buffs = activeBuffs.get(teamId);
        if (buffs == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(buffs);
    }

    /**
     * 队伍是否有活跃增幅。
     */
    public boolean hasBuff(UUID teamId) {
        List<TeamBuff> buffs = activeBuffs.get(teamId);
        return buffs != null && !buffs.isEmpty();
    }

    /**
     * 队伍解散时清理其全部增幅（属性需收回）。
     */
    public void clearTeam(UUID teamId) {
        List<TeamBuff> buffs = activeBuffs.remove(teamId);
        if (buffs != null) {
            for (TeamBuff buff : buffs) {
                if (buff.getKind() == BuffKind.ATTRIBUTE) {
                    newlyRemoved.add(buff);
                }
            }
        }
        save();
    }

    private int countActiveBuffs() {
        int total = 0;
        for (List<TeamBuff> buffs : activeBuffs.values()) {
            total += buffs.size();
        }
        return total;
    }

    // ================================================================
    //  每秒 tick（主线程）
    // ================================================================

    private void tick() {
        tickCounter++;

        // 1. 收回待移除的属性增幅
        while (!newlyRemoved.isEmpty()) {
            TeamBuff buff = newlyRemoved.remove(0);
            forEachOnlineMember(buff, player -> applyAttributeRemove(player, buff));
        }

        // 2. 扫描并移除过期增幅
        for (Map.Entry<UUID, List<TeamBuff>> entry : activeBuffs.entrySet()) {
            List<TeamBuff> buffs = entry.getValue();
            List<TeamBuff> expired = new ArrayList<>();
            for (TeamBuff buff : buffs) {
                if (buff.isExpired()) {
                    expired.add(buff);
                }
            }
            for (TeamBuff buff : expired) {
                buffs.remove(buff);
                if (buff.getKind() == BuffKind.ATTRIBUTE) {
                    forEachOnlineMember(buff, player -> applyAttributeRemove(player, buff));
                }
            }
            if (buffs.isEmpty()) {
                activeBuffs.remove(entry.getKey());
            }
        }

        // 3. 应用新增增幅（瞬间药水立即生效，属性立即注入）
        while (!newlyAdded.isEmpty()) {
            TeamBuff buff = newlyAdded.remove(0);
            forEachOnlineMember(buff, player -> applyFirstTime(player, buff));
        }

        // 4. 全量刷新持续药水（信标式持续覆盖，自动覆盖新上线 / 新入队成员）
        for (Map.Entry<UUID, List<TeamBuff>> entry : activeBuffs.entrySet()) {
            Team team = plugin.getTeamManager().getLoadedTeam(entry.getKey());
            if (team == null) {
                continue;
            }
            for (Player player : team.getOnlinePlayers()) {
                for (TeamBuff buff : entry.getValue()) {
                    if (buff.getKind() == BuffKind.POTION) {
                        applyContinuousPotion(player, buff);
                    }
                }
            }
        }

        // 5. 上线玩家补发属性增幅
        while (!pendingAttributeRefresh.isEmpty()) {
            UUID playerId = pendingAttributeRefresh.remove(0);
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                reapplyAttributesForPlayer(player);
            }
        }
    }

    /**
     * 对增幅所属队伍的全部在线成员执行操作。
     */
    private void forEachOnlineMember(TeamBuff buff, java.util.function.Consumer<Player> action) {
        for (Map.Entry<UUID, List<TeamBuff>> entry : activeBuffs.entrySet()) {
            if (!entry.getValue().contains(buff)) {
                continue;
            }
            Team team = plugin.getTeamManager().getLoadedTeam(entry.getKey());
            if (team != null) {
                for (Player player : team.getOnlinePlayers()) {
                    action.accept(player);
                }
            }
        }
    }

    private void applyFirstTime(Player player, TeamBuff buff) {
        if (buff.getKind() == BuffKind.POTION) {
            PotionEffectType type = PotionEffectType.getByName(buff.getEffectKey());
            if (type != null && isInstant(type)) {
                // 瞬间药水只施加一次
                player.addPotionEffect(new PotionEffect(type, 1, buff.getAmplifier(), false, false));
            }
        } else {
            applyAttributeAdd(player, buff);
        }
    }

    private void applyContinuousPotion(Player player, TeamBuff buff) {
        PotionEffectType type = PotionEffectType.getByName(buff.getEffectKey());
        if (type == null || isInstant(type)) {
            return;
        }
        long remaining = buff.getRemainingSeconds();
        if (remaining <= 0) {
            return;
        }
        int durationTicks = (int) (remaining * 20L) + REFRESH_BUFFER_TICKS;
        player.addPotionEffect(new PotionEffect(type, durationTicks, buff.getAmplifier(), false, false), true);
    }

    private void applyAttributeAdd(Player player, TeamBuff buff) {
        AttributeBuffProvider provider = resolveProvider();
        if (provider != null && provider.isAvailable()) {
            provider.apply(player, buff.getEffectKey(), buff.getAmplifier(), buff.getId());
        }
    }

    private void applyAttributeRemove(Player player, TeamBuff buff) {
        AttributeBuffProvider provider = resolveProvider();
        if (provider != null && provider.isAvailable()) {
            provider.remove(player, buff.getEffectKey(), buff.getId());
        }
    }

    private void reapplyAttributesForPlayer(Player player) {
        for (Map.Entry<UUID, List<TeamBuff>> entry : activeBuffs.entrySet()) {
            Team team = plugin.getTeamManager().getLoadedTeam(entry.getKey());
            if (team == null || !team.hasMember(player.getUniqueId())) {
                continue;
            }
            for (TeamBuff buff : entry.getValue()) {
                if (buff.getKind() == BuffKind.ATTRIBUTE && !buff.isExpired()) {
                    applyAttributeAdd(player, buff);
                }
            }
        }
    }

    private AttributeBuffProvider resolveProvider() {
        // 默认使用命令映射 provider；可扩展为按属性配置选择特定 provider
        return providers.get(CommandAttributeBuffProvider.ID);
    }

    private boolean isInstant(PotionEffectType type) {
        return type.equals(PotionEffectType.HEAL) || type.equals(PotionEffectType.HARM);
    }

    // ================================================================
    //  上线监听
    // ================================================================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        // 延迟标记，等玩家索引与队伍装载完成后补发属性
        UUID id = event.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTaskLater(plugin, () -> pendingAttributeRefresh.add(id), 40L);
    }

    // ================================================================
    //  持久化
    // ================================================================

    private void load() {
        activeBuffs.clear();
        if (storageFile == null || !storageFile.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(storageFile);
        ConfigurationSection root = yaml.getConfigurationSection("buffs");
        if (root == null) {
            return;
        }
        int restored = 0;
        for (String teamKey : root.getKeys(false)) {
            UUID teamId;
            try {
                teamId = UUID.fromString(teamKey);
            } catch (IllegalArgumentException e) {
                continue;
            }
            List<TeamBuff> buffs = new CopyOnWriteArrayList<>();
            for (Map<?, ?> raw : root.getMapList(teamKey)) {
                TeamBuff buff = deserialize(raw);
                if (buff != null && !buff.isExpired()) {
                    buffs.add(buff);
                    restored++;
                }
            }
            if (!buffs.isEmpty()) {
                activeBuffs.put(teamId, buffs);
            }
        }
        plugin.getLogger().info("已恢复 " + restored + " 条未过期增幅。");
    }

    @SuppressWarnings("unchecked")
    private TeamBuff deserialize(Map<?, ?> raw) {
        try {
            BuffKind kind = BuffKind.fromId(String.valueOf(raw.get("kind")));
            String effectKey = String.valueOf(raw.get("effect-key"));
            String displayName = raw.get("display-name") == null ? null : String.valueOf(raw.get("display-name"));
            int amplifier = ((Number) raw.get("amplifier")).intValue();
            long duration = ((Number) raw.get("duration")).longValue();
            long startedAt = ((Number) raw.get("started-at")).longValue();
            String grantedBy = raw.get("granted-by") == null ? null : String.valueOf(raw.get("granted-by"));
            String id = raw.get("id") == null ? null : String.valueOf(raw.get("id"));
            return new TeamBuff(id, kind, effectKey, displayName, amplifier, duration, startedAt, grantedBy);
        } catch (Exception e) {
            return null;
        }
    }

    public void save() {
        if (storageFile == null) {
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, List<TeamBuff>> entry : activeBuffs.entrySet()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (TeamBuff buff : entry.getValue()) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", buff.getId());
                map.put("kind", buff.getKind().getId());
                map.put("effect-key", buff.getEffectKey());
                map.put("display-name", buff.getDisplayName());
                map.put("amplifier", buff.getAmplifier());
                map.put("duration", buff.getDurationSeconds());
                map.put("started-at", buff.getStartedAt());
                map.put("granted-by", buff.getGrantedBy());
                list.add(map);
            }
            yaml.set("buffs." + entry.getKey(), list);
        }
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(storageFile);
        } catch (IOException e) {
            plugin.getLogger().warning("保存 buffs.yml 失败: " + e.getMessage());
        }
    }
}
