package soys.soyslinkteam.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通用冷却管理器。
 * <p>按「冷却类型 + 玩家」维度记录到期时间戳，纯内存，重启后清空。</p>
 */
public class CooldownManager {

    /** 冷却类型 -> (玩家 UUID -> 到期时间戳) */
    private final Map<String, Map<UUID, Long>> cooldowns = new ConcurrentHashMap<>();

    /** 创建队伍冷却 */
    public static final String CREATE = "create";

    /** 退队后再次入队冷却 */
    public static final String REJOIN = "rejoin";

    /**
     * 设置冷却。
     *
     * @param seconds 冷却秒数，&lt;= 0 时不生效
     */
    public void set(String type, UUID playerId, int seconds) {
        if (seconds <= 0) {
            return;
        }
        cooldowns.computeIfAbsent(type, key -> new ConcurrentHashMap<>())
                .put(playerId, System.currentTimeMillis() + seconds * 1000L);
    }

    /**
     * 剩余冷却秒数，无冷却时返回 0。
     */
    public int getRemaining(String type, UUID playerId) {
        Map<UUID, Long> map = cooldowns.get(type);
        if (map == null) {
            return 0;
        }
        Long expireAt = map.get(playerId);
        if (expireAt == null) {
            return 0;
        }
        long remaining = expireAt - System.currentTimeMillis();
        if (remaining <= 0) {
            map.remove(playerId);
            return 0;
        }
        return (int) Math.ceil(remaining / 1000.0);
    }

    public boolean isOnCooldown(String type, UUID playerId) {
        return getRemaining(type, playerId) > 0;
    }

    public void clear(String type, UUID playerId) {
        Map<UUID, Long> map = cooldowns.get(type);
        if (map != null) {
            map.remove(playerId);
        }
    }

    public void clearAll(UUID playerId) {
        for (Map<UUID, Long> map : cooldowns.values()) {
            map.remove(playerId);
        }
    }

    public void clearAll() {
        cooldowns.clear();
    }
}
