package soys.soyslinkteam.join;

import java.util.UUID;

/**
 * 一条待处理的队伍邀请。
 */
public class Invite {

    private final UUID teamId;
    private final String teamName;
    private final UUID inviter;
    private final String inviterName;
    private final UUID target;
    private final long createdAt;
    private final long expireAt;

    public Invite(UUID teamId, String teamName, UUID inviter, String inviterName,
                  UUID target, long expireMillis) {
        this.teamId = teamId;
        this.teamName = teamName;
        this.inviter = inviter;
        this.inviterName = inviterName;
        this.target = target;
        this.createdAt = System.currentTimeMillis();
        this.expireAt = createdAt + expireMillis;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public UUID getInviter() {
        return inviter;
    }

    public String getInviterName() {
        return inviterName;
    }

    public UUID getTarget() {
        return target;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getExpireAt() {
        return expireAt;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expireAt;
    }

    /**
     * 剩余有效秒数，已过期时返回 0。
     */
    public int getRemainingSeconds() {
        long remaining = expireAt - System.currentTimeMillis();
        return remaining <= 0 ? 0 : (int) Math.ceil(remaining / 1000.0);
    }
}
