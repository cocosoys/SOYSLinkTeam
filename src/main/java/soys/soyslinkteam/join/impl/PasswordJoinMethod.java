package soys.soyslinkteam.join.impl;

import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.join.JoinContext;
import soys.soyslinkteam.join.JoinResult;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.util.PasswordUtil;
import soys.soyslinkteam.util.Placeholders;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 口令加入。
 * <p>队长设置口令后，玩家使用 {@code /team join <队伍> <口令>} 加入。</p>
 * <p>内置错误次数限制与冷却，防止暴力猜测。</p>
 */
public class    PasswordJoinMethod extends AbstractJoinMethod {

    public static final String ID = "password";

    /** 「玩家 + 队伍」-> 失败记录 */
    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    public PasswordJoinMethod(SOYSLinkTeam plugin) {
        super(plugin);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "口令加入";
    }

    public int getMinLength() {
        return getInt("min-length", 3);
    }

    public int getMaxLength() {
        return getInt("max-length", 16);
    }

    public int getMaxAttempts() {
        return getInt("max-attempts", 5);
    }

    public int getAttemptCooldown() {
        return getInt("attempt-cooldown", 60);
    }

    public boolean isCaseSensitive() {
        return getBoolean("case-sensitive", true);
    }

    @Override
    public JoinResult attempt(JoinContext context) {
        Team team = context.getTeam();

        if (!team.getSettings().hasPassword()) {
            // 队伍未设口令，交由其它方式尝试
            return JoinResult.notApplicable();
        }

        String key = key(context.getPlayer().getUniqueId(), team.getId());
        Attempt record = attempts.get(key);

        // 处于冷却中
        if (record != null) {
            int remaining = record.getCooldownRemaining(getMaxAttempts(), getAttemptCooldown());
            if (remaining > 0) {
                return JoinResult.denied("join.password.locked",
                        Placeholders.of("seconds", remaining).and("team", team.getName()).build());
            }
            if (record.isCooldownFinished(getMaxAttempts(), getAttemptCooldown())) {
                attempts.remove(key);
                record = null;
            }
        }

        // 未提供口令
        if (!context.hasArgument()) {
            return JoinResult.denied("join.password.required",
                    Placeholders.of("team", team.getName())
                            .and("label", context.getLabel()).build());
        }

        String expected = team.getSettings().getPassword();
        String provided = context.getArgument();
        // 使用 PasswordUtil 比对：优先哈希比对，回退明文比对（兼容旧数据）
        boolean matched = PasswordUtil.matches(provided, expected, isCaseSensitive());

        if (matched) {
            attempts.remove(key);
            return JoinResult.success();
        }

        // 记录失败
        if (record == null) {
            record = new Attempt();
            attempts.put(key, record);
        }
        record.fail();

        int remaining = Math.max(0, getMaxAttempts() - record.getCount());
        if (remaining <= 0) {
            return JoinResult.denied("join.password.locked",
                    Placeholders.of("seconds", getAttemptCooldown())
                            .and("team", team.getName()).build());
        }
        return JoinResult.denied("join.password.incorrect",
                Placeholders.of("remaining", remaining).and("team", team.getName()).build());
    }

    /**
     * 清除玩家的全部失败记录（入队成功或退服时调用）。
     */
    public void clearAttempts(UUID playerId) {
        String prefix = playerId.toString() + ":";
        attempts.keySet().removeIf(key -> key.startsWith(prefix));
    }

    @Override
    public void onJoinSuccess(JoinContext context) {
        clearAttempts(context.getPlayer().getUniqueId());
    }

    private String key(UUID playerId, UUID teamId) {
        return playerId + ":" + teamId;
    }

    /**
     * 单个「玩家 + 队伍」的口令失败记录。
     */
    private static class Attempt {

        private int count = 0;
        private long lastFailAt = 0L;

        void fail() {
            count++;
            lastFailAt = System.currentTimeMillis();
        }

        int getCount() {
            return count;
        }

        /**
         * 冷却剩余秒数，未触发上限时返回 0。
         */
        int getCooldownRemaining(int maxAttempts, int cooldownSeconds) {
            if (count < maxAttempts || cooldownSeconds <= 0) {
                return 0;
            }
            long remaining = lastFailAt + cooldownSeconds * 1000L - System.currentTimeMillis();
            return remaining <= 0 ? 0 : (int) Math.ceil(remaining / 1000.0);
        }

        boolean isCooldownFinished(int maxAttempts, int cooldownSeconds) {
            return count >= maxAttempts && getCooldownRemaining(maxAttempts, cooldownSeconds) == 0;
        }
    }
}
