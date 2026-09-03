package soys.soyslinkteam.luckperms;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import org.bukkit.entity.Player;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.team.TeamMember;

/**
 * LuckPerms 上下文注入钩子。
 * <p>向 LuckPerms 注入两个上下文变量，使权限节点可以按队伍/角色生效：</p>
 * <ul>
 *   <li>{@code soyslinkteam_team} - 玩家所在队伍名称（无队伍时不注入）</li>
 *   <li>{@code soyslinkteam_role} - 玩家在队伍中的角色（leader / admin / member）</li>
 * </ul>
 * <p>使用示例（LuckPerms 权限）：</p>
 * <pre>
 *   lp group vip permission set essentials.fly true context=soyslinkteam_role:leader
 * </pre>
 * <p>LuckPerms 未安装时本钩子自动停用，不影响插件其它功能。</p>
 */
public class LuckPermsContextHook {

    private final SOYSLinkTeam plugin;
    private LuckPerms luckPerms;
    private ContextCalculator<Player> calculator;
    private boolean registered = false;

    public LuckPermsContextHook(SOYSLinkTeam plugin) {
        this.plugin = plugin;
    }

    /**
     * 检测 LuckPerms 并注册上下文计算器。
     * @return true 表示成功注册，false 表示 LuckPerms 不可用
     */
    public boolean initialize() {
        try {
            this.luckPerms = LuckPermsProvider.get();
        } catch (Throwable t) {
            plugin.getLogger().info("未检测到 LuckPerms，上下文注入功能已停用。");
            return false;
        }

        this.calculator = new ContextCalculator<Player>() {
            @Override
            public void calculate(Player target, ContextConsumer consumer) {
                if (target == null || !target.isOnline()) {
                    return;
                }
                Team team = plugin.getTeamManager().getLoadedPlayerTeam(target.getUniqueId());
                if (team == null) {
                    return;
                }
                // 注入队伍名称上下文
                consumer.accept("soyslinkteam_team", team.getName().toLowerCase());

                // 注入角色上下文
                TeamMember member = team.getMember(target.getUniqueId());
                if (member != null && member.getRole() != null) {
                    consumer.accept("soyslinkteam_role", member.getRole().name().toLowerCase());
                }
            }
        };

        try {
            luckPerms.getContextManager().registerCalculator(calculator);
            this.registered = true;
            plugin.getLogger().info("LuckPerms 上下文注入已启用（soyslinkteam_team / soyslinkteam_role）。");
            return true;
        } catch (Throwable t) {
            plugin.getLogger().warning("注册 LuckPerms 上下文计算器失败: " + t.getMessage());
            return false;
        }
    }

    /**
     * 注销上下文计算器。
     */
    public void shutdown() {
        if (registered && luckPerms != null && calculator != null) {
            try {
                luckPerms.getContextManager().unregisterCalculator(calculator);
            } catch (Throwable ignored) {
                // 关闭时忽略异常
            }
        }
        this.registered = false;
        this.luckPerms = null;
        this.calculator = null;
    }

    public boolean isActive() {
        return registered;
    }
}
