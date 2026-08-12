package soys.soyslinkteam.permission.impl;

import org.bukkit.entity.Player;
import soys.soyslinkteam.permission.PermissionProvider;
import soys.soyslinkteam.permission.TeamAction;
import soys.soyslinkteam.permission.TriState;
import soys.soyslinkteam.team.Team;

/**
 * 管理员权限 Provider。
 * <p>
 * 基于 Bukkit 权限节点：持有 {@code soyslinkteam.admin.manage} 或
 * {@code soyslinkteam.admin.bypass} 的玩家可以对任意队伍执行任意动作。
 * 该 Provider 通常排在判定链首位，用于让管理员越过所有队内角色限制。
 * </p>
 */
public class AdminPermissionProvider implements PermissionProvider {

    public static final String ID = "admin";

    private static final String MANAGE = "soyslinkteam.admin.manage";
    private static final String BYPASS = "soyslinkteam.admin.bypass";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public TriState check(Player player, Team team, TeamAction action) {
        if (player == null) {
            return TriState.PASS;
        }
        if (player.hasPermission(MANAGE) || player.hasPermission(BYPASS)) {
            return TriState.ALLOW;
        }
        // 不持有管理节点时不表态，交给队内角色判定
        return TriState.PASS;
    }
}
