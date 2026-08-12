package soys.soyslinkteam.join;

import org.bukkit.Bukkit;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.event.PlayerJoinTeamEvent;
import soys.soyslinkteam.join.impl.InviteJoinMethod;
import soys.soyslinkteam.join.impl.PasswordJoinMethod;
import soys.soyslinkteam.join.impl.PublicJoinMethod;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.team.TeamRole;
import soys.soyslinkteam.util.CooldownManager;
import soys.soyslinkteam.util.Placeholders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 入队方式注册表与统一入队执行器。
 *
 * <h3>尝试流程</h3>
 * <ol>
 *   <li>通用前置校验：是否已有队伍、退队冷却、队伍是否已满；</li>
 *   <li>按 order 升序遍历所有启用的 {@link JoinMethod}；</li>
 *   <li>某方式返回 SUCCESS 则执行实际入队并停止；返回 DENIED 则立即中断并反馈；
 *       返回 NOT_APPLICABLE 则继续下一个；</li>
 *   <li>全部不适用时返回 {@code join.no-method-available}。</li>
 * </ol>
 *
 * <h3>扩展方式</h3>
 * <pre>
 * SOYSLinkTeam.getInstance().getJoinMethodRegistry()
 *         .register(new MyItemCostJoinMethod());
 * // 并在 config.yml 的 join.methods 下补充同名配置节
 * </pre>
 */
public class JoinMethodRegistry {

    private final SOYSLinkTeam plugin;
    private final Map<String, JoinMethod> methods = new LinkedHashMap<>();

    public JoinMethodRegistry(SOYSLinkTeam plugin) {
        this.plugin = plugin;
    }

    /**
     * 注册三种内置入队方式。
     */
    public void initialize() {
        methods.clear();
        register(new InviteJoinMethod(plugin));
        register(new PublicJoinMethod(plugin));
        register(new PasswordJoinMethod(plugin));
    }

    public void register(JoinMethod method) {
        if (method == null || method.getId() == null) {
            return;
        }
        methods.put(method.getId().toLowerCase(), method);
    }

    public void unregister(String id) {
        if (id != null) {
            methods.remove(id.toLowerCase());
        }
    }

    public JoinMethod get(String id) {
        return id == null ? null : methods.get(id.toLowerCase());
    }

    /**
     * 按 order 升序排列的已启用方式。
     */
    public List<JoinMethod> getEnabledMethods() {
        List<JoinMethod> list = new ArrayList<>();
        for (JoinMethod method : methods.values()) {
            if (method.isEnabled()) {
                list.add(method);
            }
        }
        list.sort(Comparator.comparingInt(JoinMethod::getOrder));
        return list;
    }

    public List<JoinMethod> getAllMethods() {
        return Collections.unmodifiableList(new ArrayList<>(methods.values()));
    }

    public void reload() {
        for (JoinMethod method : methods.values()) {
            method.reload();
        }
    }

    // 便捷访问内置方式
    public InviteJoinMethod getInviteMethod() {
        JoinMethod method = get(InviteJoinMethod.ID);
        return method instanceof InviteJoinMethod ? (InviteJoinMethod) method : null;
    }

    public PublicJoinMethod getPublicMethod() {
        JoinMethod method = get(PublicJoinMethod.ID);
        return method instanceof PublicJoinMethod ? (PublicJoinMethod) method : null;
    }

    public PasswordJoinMethod getPasswordMethod() {
        JoinMethod method = get(PasswordJoinMethod.ID);
        return method instanceof PasswordJoinMethod ? (PasswordJoinMethod) method : null;
    }

    // ================================================================
    //  入队执行
    // ================================================================

    /**
     * 执行一次完整的入队尝试。成功时玩家已被真正加入队伍。
     *
     * @return 最终结果，调用方据此向玩家反馈
     */
    public JoinResult tryJoin(JoinContext context) {
        Team team = context.getTeam();

        // ---------- 通用前置校验（管理员强制入队时跳过） ----------
        if (!context.isForced()) {
            JoinResult precheck = precheck(context);
            if (precheck != null) {
                return precheck;
            }
        }

        // ---------- 遍历入队方式 ----------
        JoinMethod matched = null;
        if (context.isForced()) {
            matched = null;
        } else {
            for (JoinMethod method : getEnabledMethods()) {
                JoinResult result;
                try {
                    result = method.attempt(context);
                } catch (Throwable t) {
                    plugin.getLogger().warning("入队方式 \"" + method.getId()
                            + "\" 判定异常，已跳过: " + t.getMessage());
                    continue;
                }
                if (result == null || result.isNotApplicable()) {
                    continue;
                }
                if (result.isDenied()) {
                    return result;
                }
                matched = method;
                break;
            }
            if (matched == null) {
                return JoinResult.denied("join.no-method-available",
                        Placeholders.of("team", team.getName()).build());
            }
        }

        // ---------- 执行实际入队 ----------
        boolean joined = performJoin(context, matched);
        return joined ? JoinResult.success()
                : JoinResult.denied("event.cancelled", Placeholders.of("reason", "").build());
    }

    /**
     * 通用前置校验。
     *
     * @return 校验失败的结果，通过时返回 null
     */
    private JoinResult precheck(JoinContext context) {
        Team team = context.getTeam();

        if (plugin.getTeamManager().hasTeam(context.getPlayer().getUniqueId())) {
            return JoinResult.denied("team.already-in-team",
                    Placeholders.of("team", currentTeamName(context)).build());
        }

        if (!plugin.getPermissionManager().canBypass(context.getPlayer())) {
            int remaining = plugin.getCooldownManager()
                    .getRemaining(CooldownManager.REJOIN, context.getPlayer().getUniqueId());
            if (remaining > 0) {
                return JoinResult.denied("member.rejoin-cooldown",
                        Placeholders.of("seconds", remaining).build());
            }

            if (plugin.getTeamManager().isFull(team)) {
                return JoinResult.denied("team.team-full", Placeholders
                        .of("team", team.getName())
                        .and("size", team.getSize())
                        .and("max", plugin.getTeamManager().getMaxSize(team))
                        .build());
            }
        }

        return null;
    }

    private String currentTeamName(JoinContext context) {
        java.util.UUID teamId = plugin.getTeamManager()
                .getPlayerTeamId(context.getPlayer().getUniqueId());
        String name = teamId == null ? null : plugin.getTeamManager().getTeamNameIndex().get(teamId);
        return name == null ? "未知" : name;
    }

    /**
     * 真正把玩家加入队伍，并处理广播、公告与方式回调。
     *
     * @return 是否成功加入（被监听器取消时返回 false）
     */
    private boolean performJoin(JoinContext context, JoinMethod method) {
        Team team = context.getTeam();

        PlayerJoinTeamEvent event = new PlayerJoinTeamEvent(team, context.getPlayer(), null);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        plugin.getTeamManager().addMember(team, context.getPlayer(), TeamRole.MEMBER);

        // 清理该玩家在所有队伍的邀请与口令失败记录
        plugin.getInviteManager().removeAll(context.getPlayer().getUniqueId());
        PasswordJoinMethod password = getPasswordMethod();
        if (password != null) {
            password.clearAttempts(context.getPlayer().getUniqueId());
        }

        if (method != null) {
            try {
                method.onJoinSuccess(context);
            } catch (Throwable t) {
                plugin.getLogger().warning("入队方式 \"" + method.getId()
                        + "\" 的成功回调异常: " + t.getMessage());
            }
        }

        // 向队伍广播
        Map<String, String> placeholders = Placeholders
                .of("player", context.getPlayer().getName())
                .and("team", team.getName())
                .and("size", team.getSize())
                .and("max", plugin.getTeamManager().getMaxSize(team))
                .build();
        team.broadcast(plugin.getMessageManager().get("member.join-broadcast", placeholders),
                context.getPlayer().getUniqueId());

        // 向入队者反馈
        plugin.getMessageManager().send(context.getPlayer(), "join.success", placeholders);

        // 推送队伍公告
        if (plugin.getConfigManager().isNoticeEnabled()
                && plugin.getConfigManager().isNoticeShowOnJoin()
                && team.getSettings().hasNotice()) {
            plugin.getMessageManager().send(context.getPlayer(), "settings.notice.on-join",
                    Placeholders.of("notice", team.getSettings().getNotice()).build());
        }
        return true;
    }
}
