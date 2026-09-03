package soys.soyslinkteam.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import soys.soyslinkteam.SOYSLinkTeam;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 主配置文件访问器。
 * <p>集中收敛所有配置读取，避免各模块散落硬编码的配置路径。</p>
 * <p>配置被拆分到两个文件：
 *   - config.yml  : 通用设置 / 数据存储 / PAPI / 指令
 *   - teams.yml   : 队伍规则(team) / 入队方式(join) / 权限体系(permission)</p>
 */
public class ConfigManager {

    private final SOYSLinkTeam plugin;
    /** 主配置文件（config.yml） */
    private FileConfiguration config;
    /** 队伍规则/入队方式/权限体系（teams.yml） */
    private FileConfiguration rulesConfig;

    public ConfigManager(SOYSLinkTeam plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        // ---- config.yml ----
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        // ---- teams.yml ----
        File rulesFile = new File(plugin.getDataFolder(), "teams.yml");
        if (!rulesFile.exists()) {
            plugin.saveResource("teams.yml", false);
        }
        this.rulesConfig = YamlConfiguration.loadConfiguration(rulesFile);
        FileConfiguration rulesDefaults = loadRulesDefaults();
        if (rulesDefaults != null) {
            rulesConfig.setDefaults(rulesDefaults);
        }
    }

    /** 从 jar 内资源读取 teams.yml 默认值（缺失键时回退）。 */
    private FileConfiguration loadRulesDefaults() {
        try (InputStream stream = plugin.getResource("teams.yml")) {
            if (stream == null) {
                return null;
            }
            return YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return null;
        }
    }

    public FileConfiguration raw() {
        return config;
    }

    /** teams.yml 的原始配置对象（队伍规则/入队方式/权限体系）。 */
    public FileConfiguration rawRules() {
        return rulesConfig;
    }

    // ================================================================
    //  通用
    // ================================================================

    public String getLanguage() {
        return config.getString("general.language", "zh_CN");
    }

    public boolean isDebug() {
        return config.getBoolean("general.debug", false);
    }

    // ================================================================
    //  内存激活策略
    // ================================================================

    /** 队伍活跃超时（毫秒） */
    public long getActiveTimeoutMillis() {
        return Math.max(1L, config.getLong("storage.memory.active-timeout", 30L)) * 60L * 1000L;
    }

    /** 活跃检测间隔（tick） */
    public long getCheckIntervalTicks() {
        return Math.max(5L, config.getLong("storage.memory.check-interval", 60L)) * 20L;
    }

    /** 自动保存间隔（tick），0 表示关闭 */
    public long getAutoSaveIntervalTicks() {
        long seconds = config.getLong("storage.memory.auto-save-interval", 300L);
        return seconds <= 0 ? 0L : seconds * 20L;
    }

    public boolean isLoadOnJoin() {
        return config.getBoolean("storage.memory.load-on-join", true);
    }

    public boolean isKeepLoadedWhileOnline() {
        return config.getBoolean("storage.memory.keep-loaded-while-online", true);
    }

    // ================================================================
    //  存储后端
    // ================================================================

    public boolean isBackendEnabled(String backendId) {
        return config.getBoolean("storage.backends." + backendId + ".enabled", false);
    }

    public ConfigurationSection getBackendSection(String backendId) {
        return config.getConfigurationSection("storage.backends." + backendId);
    }

    public boolean isMirrorEnabled() {
        return config.getBoolean("storage.mirror.enabled", true);
    }

    public boolean isMirrorAsync() {
        return config.getBoolean("storage.mirror.async", true);
    }

    public boolean isSyncOnStartup() {
        return config.getBoolean("storage.mirror.sync-on-startup", false);
    }

    // ================================================================
    //  队伍规则 - 人数（来源: teams.yml）
    // ================================================================

    public int getDefaultMaxSize() {
        return Math.max(1, rulesConfig.getInt("team.size.default-max", 5));
    }

    public int getHardMaxSize() {
        return Math.max(getDefaultMaxSize(), rulesConfig.getInt("team.size.hard-max", 30));
    }

    public boolean isFollowLeaderPermission() {
        return rulesConfig.getBoolean("team.size.follow-leader-permission", true);
    }

    // ================================================================
    //  队伍规则 - 名称（来源: teams.yml）
    // ================================================================

    public int getNameMinLength() {
        return rulesConfig.getInt("team.name.min-length", 2);
    }

    public int getNameMaxLength() {
        return rulesConfig.getInt("team.name.max-length", 16);
    }

    public String getNamePattern() {
        return rulesConfig.getString("team.name.pattern", "^[\\u4e00-\\u9fa5A-Za-z0-9_]+$");
    }

    public boolean isAllowDuplicateName() {
        return rulesConfig.getBoolean("team.name.allow-duplicate", false);
    }

    public boolean isNameCaseSensitive() {
        return rulesConfig.getBoolean("team.name.case-sensitive", false);
    }

    public List<String> getNameBlacklist() {
        return rulesConfig.getStringList("team.name.blacklist");
    }

    // ================================================================
    //  队伍规则 - 简称 / 公告（来源: teams.yml）
    // ================================================================

    public boolean isTagEnabled() {
        return rulesConfig.getBoolean("team.tag.enabled", true);
    }

    public int getTagMinLength() {
        return rulesConfig.getInt("team.tag.min-length", 1);
    }

    public int getTagMaxLength() {
        return rulesConfig.getInt("team.tag.max-length", 6);
    }

    public String getTagPattern() {
        return rulesConfig.getString("team.tag.pattern", "^[\\u4e00-\\u9fa5A-Za-z0-9]+$");
    }

    public String getTagFallback() {
        return rulesConfig.getString("team.tag.fallback", "{name}");
    }

    // ================================================================
    //  队伍规则 - 头顶名称 / 计分板（来源: teams.yml，需 ProtocolLib）
    // ================================================================

    /** 头顶名称（数据包方案）是否启用 */
    public boolean isNametagEnabled() {
        return rulesConfig.getBoolean("team.tag.nametag.enabled", true);
    }

    /** 是否按角色着色玩家名（队长金 / 副队长蓝 / 队员默认） */
    public boolean isNametagUseRoleColor() {
        return rulesConfig.getBoolean("team.tag.nametag.use-role-color", true);
    }

    /** 简称与玩家名之间的分隔符 */
    public String getNametagSeparator() {
        return rulesConfig.getString("team.tag.nametag.separator", " ");
    }

    /**
     * 碰撞规则，返回 SCOREBOARD_TEAM 数据包可用的小写驼峰格式。
     * 合法值: always / pushOtherTeams / pushOwnTeam / never
     */
    public String getNametagCollisionRule() {
        String raw = rulesConfig.getString("team.tag.nametag.collision-rule", "NEVER");
        return normalizePacketEnum(raw, "never");
    }

    /**
     * 名称可见性，返回 SCOREBOARD_TEAM 数据包可用的小写驼峰格式。
     * 合法值: always / hideForOtherTeams / hideForOwnTeam / never
     */
    public String getNametagVisibility() {
        String raw = rulesConfig.getString("team.tag.nametag.visibility", "ALWAYS");
        return normalizePacketEnum(raw, "always");
    }

    /**
     * 将配置中的大写下划线/大写枚举值转换为数据包要求的小写驼峰格式。
     * 例: NEVER→never, HIDE_FOR_OTHER_TEAMS→hideForOtherTeams
     */
    private String normalizePacketEnum(String raw, String fallback) {
        if (raw == null || raw.isEmpty()) {
            return fallback;
        }
        String lower = raw.trim().toLowerCase();
        // 已经是小写驼峰，直接返回
        if (lower.equals("always") || lower.equals("never")
                || lower.equals("pushotherteams") || lower.equals("pushownteam")
                || lower.equals("hideforotherteams") || lower.equals("hideforownteam")) {
            // 还原正确的驼峰大小写
            switch (lower) {
                case "pushotherteams": return "pushOtherTeams";
                case "pushownteam": return "pushOwnTeam";
                case "hideforotherteams": return "hideForOtherTeams";
                case "hideforownteam": return "hideForOwnTeam";
                default: return lower;
            }
        }
        return fallback;
    }

    public boolean isNoticeEnabled() {
        return rulesConfig.getBoolean("team.notice.enabled", true);
    }

    public int getNoticeMaxLength() {
        return rulesConfig.getInt("team.notice.max-length", 64);
    }

    public boolean isNoticeShowOnJoin() {
        return rulesConfig.getBoolean("team.notice.show-on-join", true);
    }

    // ================================================================
    //  队伍规则 - 行为（来源: teams.yml）
    // ================================================================

    public int getCreateCooldown() {
        return rulesConfig.getInt("team.behavior.create-cooldown", 60);
    }

    public int getRejoinCooldown() {
        return rulesConfig.getInt("team.behavior.rejoin-cooldown", 30);
    }

    public boolean isDisbandConfirm() {
        return rulesConfig.getBoolean("team.behavior.disband-confirm", true);
    }

    public int getConfirmTimeout() {
        return rulesConfig.getInt("team.behavior.confirm-timeout", 15);
    }

    public String getLeaderQuitAction() {
        return rulesConfig.getString("team.behavior.leader-quit-action", "TRANSFER").toUpperCase();
    }

    public int getAutoTransferOfflineMinutes() {
        return rulesConfig.getInt("team.behavior.auto-transfer-offline", 0);
    }

    public boolean isDisbandWhenEmpty() {
        return rulesConfig.getBoolean("team.behavior.disband-when-empty", true);
    }

    public boolean isLeaderCanKickAdmin() {
        return rulesConfig.getBoolean("team.behavior.leader-can-kick-admin", true);
    }

    /** 队伍频道聊天是否开启（/steam chat） */
    public boolean isTeamChatEnabled() {
        return rulesConfig.getBoolean("team.behavior.team-chat.enabled", true);
    }

    /** 队伍频道消息格式，可用占位 {player} {message} */
    public String getTeamChatFormat() {
        return rulesConfig.getString("team.behavior.team-chat.format",
                "&8[&e队伍&8] &f{player}&8: &7{message}");
    }

    // ================================================================
    //  入队方式（来源: teams.yml）
    // ================================================================

    /**
     * 玩家同时最多可加入的队伍数量。
     * <p>当前版本数据模型固定为 1 队（playerIndex 为 UUID→UUID 一对一映射），
     * 配置值 >1 时会在启动时给出警告，多队支持将在未来版本实现。</p>
     */
    public int getMaxTeamsPerPlayer() {
        return Math.max(1, rulesConfig.getInt("join.max-teams-per-player", 1));
    }

    public boolean isJoinMethodEnabled(String methodId) {
        return rulesConfig.getBoolean("join.methods." + methodId + ".enabled", false);
    }

    public int getJoinMethodOrder(String methodId) {
        return rulesConfig.getInt("join.methods." + methodId + ".order", 100);
    }

    public ConfigurationSection getJoinMethodSection(String methodId) {
        return rulesConfig.getConfigurationSection("join.methods." + methodId);
    }

    public int getPasswordMinLength() {
        return rulesConfig.getInt("join.methods.password.min-length", 3);
    }

    public int getPasswordMaxLength() {
        return rulesConfig.getInt("join.methods.password.max-length", 16);
    }

    /**
     * 口令是否区分大小写（影响哈希计算与验证）。
     */
    public boolean isPasswordCaseSensitive() {
        return rulesConfig.getBoolean("join.methods.password.case-sensitive", true);
    }

    /**
     * 经济消耗入队的单队费用上限，0 表示不限制。
     */
    public double getEconomyMaxCost() {
        ConfigurationSection section = getJoinMethodSection("economy");
        return section == null ? 0 : section.getDouble("max-cost", 0);
    }

    // ================================================================
    //  权限（来源: teams.yml）
    // ================================================================

    public List<String> getPermissionProviderOrder() {
        List<String> list = rulesConfig.getStringList("permission.providers");
        if (list.isEmpty()) {
            list = new ArrayList<>();
            list.add("admin");
            list.add("role");
        }
        return list;
    }

    public String getPermissionDefaultResult() {
        return rulesConfig.getString("permission.default-result", "DENY").toUpperCase();
    }

    public List<String> getRoleActions(String roleName) {
        return rulesConfig.getStringList("permission.roles." + roleName);
    }

    public int getMaxTeamAdmins() {
        return rulesConfig.getInt("permission.max-team-admins", 2);
    }

    /**
     * 获取 VIP 权限配置节（permission.vip），未配置时返回 null。
     */
    public ConfigurationSection getVipSection() {
        return rulesConfig.getConfigurationSection("permission.vip");
    }

    // ================================================================
    //  PAPI（来源: config.yml）
    // ================================================================

    public String getPlaceholderNoTeam() {
        return config.getString("placeholder.no-team", "&7无队伍");
    }

    public String getPlaceholderTrue() {
        return config.getString("placeholder.boolean.true-text", "&a是");
    }

    public String getPlaceholderFalse() {
        return config.getString("placeholder.boolean.false-text", "&c否");
    }

    public String getPlaceholderListSeparator() {
        return config.getString("placeholder.list-separator", "&7, ");
    }

    public int getPlaceholderListLimit() {
        return config.getInt("placeholder.list-limit", 10);
    }

    public String getDateFormat() {
        return config.getString("placeholder.date-format", "yyyy-MM-dd HH:mm");
    }

    public String getRoleDisplay(String roleName, String fallback) {
        return config.getString("placeholder.role-display." + roleName, fallback);
    }

    // ================================================================
    //  指令（来源: config.yml）
    // ================================================================

    public int getListPageSize() {
        return Math.max(1, config.getInt("command.list-page-size", 8));
    }

    public boolean isTabComplete() {
        return config.getBoolean("command.tab-complete", true);
    }

    public boolean isTabCompletePlayers() {
        return config.getBoolean("command.tab-complete-players", true);
    }
}
