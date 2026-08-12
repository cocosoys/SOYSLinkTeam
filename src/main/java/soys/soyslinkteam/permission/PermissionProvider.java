package soys.soyslinkteam.permission;

import org.bukkit.entity.Player;
import soys.soyslinkteam.team.Team;

/**
 * 权限判定提供者。
 * <p>
 * 权限体系被抽象为一条责任链：{@link PermissionManager} 按 config.yml 中
 * {@code permission.providers} 的顺序依次询问每个 Provider，
 * 第一个返回 {@link TriState#ALLOW} 或 {@link TriState#DENY} 的结果即为最终判定，
 * 返回 {@link TriState#PASS} 则继续询问下一个。
 * </p>
 * <p>
 * 第三方插件可实现本接口并通过
 * {@link PermissionManager#register(PermissionProvider)} 注册自定义权限体系
 * （如 VIP 等级、赛季资格、副本进度限制），无需修改插件本体。
 * </p>
 */
public interface PermissionProvider {

    /**
     * Provider 唯一标识，与 config.yml 中 {@code permission.providers} 的条目对应。
     */
    String getId();

    /**
     * 判定玩家能否在指定队伍中执行某个动作。
     *
     * @param player 操作者
     * @param team   目标队伍，可能为 null（如尚未入队时的全局动作）
     * @param action 动作
     * @return 三态结果
     */
    TriState check(Player player, Team team, TeamAction action);

    /**
     * 配置重载时的回调，用于刷新 Provider 内部缓存。默认无操作。
     */
    default void reload() {
    }
}
