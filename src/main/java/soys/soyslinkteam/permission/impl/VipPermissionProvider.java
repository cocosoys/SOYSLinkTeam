package soys.soyslinkteam.permission.impl;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.permission.PermissionProvider;
import soys.soyslinkteam.permission.TeamAction;
import soys.soyslinkteam.permission.TriState;
import soys.soyslinkteam.team.Team;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * VIP 权限提供者：基于 LuckPerms 元数据中的 vip-tier 字段判定权限。
 * <p>在 config.yml 的 {@code permission.vip} 中配置每个 VIP 等级允许的动作，
 * 玩家的 vip-tier 元数据值匹配时返回 ALLOW，否则 PASS（交由后续 Provider 判定）。</p>
 * <p>使用示例（LuckPerms 设置元数据）：</p>
 * <pre>
 *   lp user Steve meta set vip-tier svip
 * </pre>
 * <p>LuckPerms 未安装或未配置 vip 节时本 Provider 始终返回 PASS。</p>
 */
public class VipPermissionProvider implements PermissionProvider {

    public static final String ID = "vip";

    private final SOYSLinkTeam plugin;

    /** VIP 等级 -> 允许的动作集合 */
    private final Map<String, Set<TeamAction>> allowActions = new HashMap<>();

    private volatile boolean enabled = false;
    private volatile LuckPerms luckPerms;

    public VipPermissionProvider(SOYSLinkTeam plugin) {
        this.plugin = plugin;
        reload();
    }

    @Override
    public String getId() {
        return ID;
    }

    public String getDisplayName() {
        return "VIP 等级";
    }

    @Override
    public TriState check(Player player, Team team, TeamAction action) {
        if (!enabled || player == null || action == null) {
            return TriState.PASS;
        }
        String tier = getVipTier(player);
        if (tier == null || tier.isEmpty()) {
            return TriState.PASS;
        }
        Set<TeamAction> actions = allowActions.get(tier.toLowerCase());
        if (actions != null && actions.contains(action)) {
            return TriState.ALLOW;
        }
        return TriState.PASS;
    }

    @Override
    public void reload() {
        allowActions.clear();
        ConfigurationSection vipSection = plugin.getConfigManager().getVipSection();
        if (vipSection == null) {
            this.enabled = false;
            return;
        }
        this.enabled = vipSection.getBoolean("enabled", false);
        if (!enabled) {
            return;
        }
        ConfigurationSection tiers = vipSection.getConfigurationSection("tiers");
        if (tiers == null) {
            return;
        }
        for (String tier : tiers.getKeys(false)) {
            Set<TeamAction> actions = new HashSet<>();
            for (String actionName : tiers.getStringList(tier + ".allow")) {
                TeamAction action = TeamAction.parse(actionName);
                if (action != null) {
                    actions.add(action);
                }
            }
            if (!actions.isEmpty()) {
                allowActions.put(tier.toLowerCase(), actions);
            }
        }
    }

    /**
     * 从 LuckPerms 读取玩家的 vip-tier 元数据值。
     */
    private String getVipTier(Player player) {
        try {
            if (luckPerms == null) {
                luckPerms = LuckPermsProvider.get();
            }
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user == null) {
                return null;
            }
            return user.getCachedData()
                    .getMetaData(QueryOptions.defaultContextualOptions())
                    .getMetaValue("vip-tier");
        } catch (Throwable t) {
            // LuckPerms 不可用或读取失败时静默返回 null
            return null;
        }
    }
}
