package soys.soyslinkteam.permission;

import org.bukkit.entity.Player;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.permission.impl.AdminPermissionProvider;
import soys.soyslinkteam.permission.impl.RolePermissionProvider;
import soys.soyslinkteam.permission.impl.VipPermissionProvider;
import soys.soyslinkteam.team.Team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 权限管理器：维护 Provider 注册表与判定链。
 *
 * <h3>判定流程</h3>
 * 按 config.yml 中 {@code permission.providers} 的顺序依次询问每个已注册的 Provider，
 * 第一个给出 ALLOW / DENY 的结果即为最终判定；全部 PASS 时使用
 * {@code permission.default-result} 兜底。
 *
 * <h3>扩展方式</h3>
 * <pre>
 * // 其它插件注册自定义权限体系
 * SOYSLinkTeam.getInstance().getPermissionManager()
 *         .register(new MyVipPermissionProvider());
 * // 然后在 config.yml 的 permission.providers 中加入该 Provider 的 id
 * </pre>
 */
public class PermissionManager {

    private final SOYSLinkTeam plugin;

    /** 已注册的全部 Provider：id -> 实例 */
    private final Map<String, PermissionProvider> registry = new LinkedHashMap<>();

    /** 按配置顺序排列的判定链 */
    private final List<PermissionProvider> chain = new ArrayList<>();

    private TriState defaultResult = TriState.DENY;

    public PermissionManager(SOYSLinkTeam plugin) {
        this.plugin = plugin;
    }

    /**
     * 注册内置 Provider 并构建判定链。
     */
    public void initialize() {
        registry.clear();
        register(new AdminPermissionProvider());
        register(new RolePermissionProvider(plugin));
        register(new VipPermissionProvider(plugin));
        rebuildChain();
    }

    /**
     * 注册一个 Provider。同 id 会被覆盖。
     * <p>注册后需调用 {@link #rebuildChain()} 或确保其 id 已在配置的 providers 列表中。</p>
     */
    public void register(PermissionProvider provider) {
        if (provider == null || provider.getId() == null) {
            return;
        }
        registry.put(provider.getId().toLowerCase(), provider);
    }

    /**
     * 注销一个 Provider。
     */
    public void unregister(String id) {
        if (id == null) {
            return;
        }
        registry.remove(id.toLowerCase());
        rebuildChain();
    }

    /**
     * 依据配置重建判定链。
     */
    public void rebuildChain() {
        chain.clear();
        for (String id : plugin.getConfigManager().getPermissionProviderOrder()) {
            PermissionProvider provider = registry.get(id.toLowerCase());
            if (provider == null) {
                plugin.getLogger().warning("config.yml permission.providers 中的 \"" + id
                        + "\" 未注册，已忽略");
                continue;
            }
            chain.add(provider);
        }
        if (chain.isEmpty()) {
            plugin.getLogger().warning("权限判定链为空，将回退到内置的 admin + role 顺序");
            PermissionProvider admin = registry.get(AdminPermissionProvider.ID);
            PermissionProvider role = registry.get(RolePermissionProvider.ID);
            if (admin != null) {
                chain.add(admin);
            }
            if (role != null) {
                chain.add(role);
            }
        }
        TriState parsed = TriState.parse(plugin.getConfigManager().getPermissionDefaultResult());
        this.defaultResult = parsed == TriState.PASS ? TriState.DENY : parsed;
    }

    /**
     * 重载所有 Provider 并重建判定链。
     */
    public void reload() {
        for (PermissionProvider provider : registry.values()) {
            provider.reload();
        }
        rebuildChain();
    }

    // ================================================================
    //  判定
    // ================================================================

    /**
     * 玩家能否在队伍中执行动作。
     */
    public boolean can(Player player, Team team, TeamAction action) {
        return resolve(player, team, action) == TriState.ALLOW;
    }

    /**
     * 完整判定，返回三态结果（已应用兜底）。
     */
    public TriState resolve(Player player, Team team, TeamAction action) {
        for (PermissionProvider provider : chain) {
            TriState result;
            try {
                result = provider.check(player, team, action);
            } catch (Throwable t) {
                plugin.getLogger().warning("权限 Provider \"" + provider.getId()
                        + "\" 判定异常，已跳过: " + t.getMessage());
                continue;
            }
            if (result != null && result.isDecisive()) {
                return result;
            }
        }
        return defaultResult;
    }

    /**
     * 玩家是否为服务器管理员（持有管理节点）。
     */
    public boolean isAdmin(Player player) {
        return player != null
                && (player.hasPermission("soyslinkteam.admin")
                || player.hasPermission("soyslinkteam.admin.manage"));
    }

    /**
     * 玩家是否可以无视人数上限、冷却等限制。
     */
    public boolean canBypass(Player player) {
        return player != null && player.hasPermission("soyslinkteam.admin.bypass");
    }

    public List<PermissionProvider> getChain() {
        return Collections.unmodifiableList(chain);
    }

    public PermissionProvider getProvider(String id) {
        return id == null ? null : registry.get(id.toLowerCase());
    }
}
