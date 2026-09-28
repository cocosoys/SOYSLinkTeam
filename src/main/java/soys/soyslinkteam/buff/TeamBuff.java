package soys.soyslinkteam.buff;

import java.util.UUID;

/**
 * 一条队伍增幅效果。
 * <p>药水效果与属性加成统一建模；通过 {@code startedAt + duration} 计算剩余时间，
 * 即使插件重启也能正确恢复剩余时长。</p>
 */
public class TeamBuff {

    private final String id;
    private final BuffKind kind;
    /** 药水效果类型名（PotionEffectType 枚举名）或属性键 */
    private final String effectKey;
    /** 显示名称（可选，用于网页展示） */
    private final String displayName;
    /** 效果等级（0 = 一级，与 Bukkit amplifier 一致） */
    private final int amplifier;
    /** 总持续秒数 */
    private final long durationSeconds;
    /** 开始时间戳（毫秒） */
    private final long startedAt;
    /** 授予者名称（管理员 / 系统），用于日志 */
    private final String grantedBy;

    public TeamBuff(String id, BuffKind kind, String effectKey, String displayName,
                    int amplifier, long durationSeconds, long startedAt, String grantedBy) {
        this.id = id;
        this.kind = kind;
        this.effectKey = effectKey;
        this.displayName = displayName;
        this.amplifier = Math.max(0, amplifier);
        this.durationSeconds = Math.max(1, durationSeconds);
        this.startedAt = startedAt;
        this.grantedBy = grantedBy;
    }

    /**
     * 创建一条新的增幅（startedAt = 当前时间）。
     */
    public static TeamBuff create(BuffKind kind, String effectKey, String displayName,
                                  int amplifier, long durationSeconds, String grantedBy) {
        return new TeamBuff(UUID.randomUUID().toString().substring(0, 8),
                kind, effectKey, displayName, amplifier, durationSeconds,
                System.currentTimeMillis(), grantedBy);
    }

    /**
     * 剩余秒数，已过期返回 0。
     */
    public long getRemainingSeconds() {
        long elapsed = (System.currentTimeMillis() - startedAt) / 1000L;
        long remaining = durationSeconds - elapsed;
        return Math.max(0, remaining);
    }

    /**
     * 是否已过期。
     */
    public boolean isExpired() {
        return getRemainingSeconds() <= 0;
    }

    /**
     * 效果等级（1 基，用于展示，如 amplifier=0 → 等级 I）。
     */
    public int getLevel() {
        return amplifier + 1;
    }

    public String getId() {
        return id;
    }

    public BuffKind getKind() {
        return kind;
    }

    public String getEffectKey() {
        return effectKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getAmplifier() {
        return amplifier;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public String getGrantedBy() {
        return grantedBy;
    }
}
