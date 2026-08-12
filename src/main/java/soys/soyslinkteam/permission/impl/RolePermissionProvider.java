package soys.soyslinkteam.permission.impl;

import org.bukkit.entity.Player;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.permission.PermissionProvider;
import soys.soyslinkteam.permission.TeamAction;
import soys.soyslinkteam.permission.TriState;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.team.TeamMember;
import soys.soyslinkteam.team.TeamRole;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 队内角色权限 Provider。
 * <p>
 * 依据 config.yml 中 {@code permission.roles} 的角色到动作映射进行判定：
 * 玩家在队伍中的角色（LEADER / ADMIN / MEMBER）允许该动作则放行，否则拒绝。
 * 玩家不在该队伍中时不表态。
 * </p>
 */
public class RolePermissionProvider implements PermissionProvider {

    public static final String ID = "role";

    private static final String WILDCARD = "*";

    private final SOYSLinkTeam plugin;
    private final Map<TeamRole, Set<TeamAction>> roleActions = new EnumMap<>(TeamRole.class);

    public RolePermissionProvider(SOYSLinkTeam plugin) {
        this.plugin = plugin;
        reload();
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void reload() {
        roleActions.clear();
        for (TeamRole role : TeamRole.values()) {
            Set<TeamAction> actions = EnumSet.noneOf(TeamAction.class);
            List<String> configured = plugin.getConfigManager().getRoleActions(role.name());

            if (configured.isEmpty()) {
                // 配置缺失时使用安全默认值
                actions.addAll(defaultActions(role));
            } else {
                for (String entry : configured) {
                    if (WILDCARD.equals(entry.trim())) {
                        actions.addAll(EnumSet.allOf(TeamAction.class));
                        break;
                    }
                    TeamAction action = TeamAction.parse(entry);
                    if (action == null) {
                        plugin.getLogger().warning("config.yml permission.roles." + role.name()
                                + " 中存在未知动作: " + entry);
                        continue;
                    }
                    actions.add(action);
                }
            }
            roleActions.put(role, actions);
        }
    }

    /**
     * 配置缺失时的兜底权限，保证插件在配置损坏时仍可运行。
     */
    private Set<TeamAction> defaultActions(TeamRole role) {
        switch (role) {
            case LEADER:
                return EnumSet.allOf(TeamAction.class);
            case ADMIN:
                return EnumSet.of(TeamAction.INVITE, TeamAction.KICK, TeamAction.SET_NOTICE,
                        TeamAction.SET_PUBLIC, TeamAction.SET_PASSWORD,
                        TeamAction.VIEW_INFO, TeamAction.LEAVE);
            default:
                return EnumSet.of(TeamAction.VIEW_INFO, TeamAction.LEAVE);
        }
    }

    @Override
    public TriState check(Player player, Team team, TeamAction action) {
        if (player == null || team == null) {
            return TriState.PASS;
        }
        TeamMember member = team.getMember(player.getUniqueId());
        if (member == null) {
            // 不是队伍成员，本 Provider 不表态
            return TriState.PASS;
        }
        Set<TeamAction> allowed = roleActions.get(member.getRole());
        if (allowed == null) {
            return TriState.DENY;
        }
        return allowed.contains(action) ? TriState.ALLOW : TriState.DENY;
    }

    /**
     * 查询某个角色被允许的动作集合，供帮助界面与调试使用。
     */
    public Set<TeamAction> getAllowedActions(TeamRole role) {
        Set<TeamAction> actions = roleActions.get(role);
        return actions == null ? EnumSet.noneOf(TeamAction.class) : EnumSet.copyOf(actions);
    }
}
