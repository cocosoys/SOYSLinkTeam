package soys.soyslinkteam.application;

import java.util.UUID;

/**
 * 一条玩家提交的入队申请。
 * <p>申请是纯内存状态（与邀请一致），具有时效性，重启后自动失效。</p>
 */
public class JoinApplication {

    /** 待审批 / 已批准 / 已拒绝 */
    public enum Status {
        PENDING, APPROVED, DENIED
    }

    private final UUID teamId;
    private final String teamName;
    private final UUID applicant;
    private final String applicantName;
    private final String message;
    private final long createdAt;
    private final long expireAt;
    private volatile Status status;

    public JoinApplication(UUID teamId, String teamName, UUID applicant, String applicantName,
                           String message, long expireMillis) {
        this.teamId = teamId;
        this.teamName = teamName;
        this.applicant = applicant;
        this.applicantName = applicantName;
        this.message = message == null ? "" : message;
        this.createdAt = System.currentTimeMillis();
        this.expireAt = createdAt + expireMillis;
        this.status = Status.PENDING;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public UUID getApplicant() {
        return applicant;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public String getMessage() {
        return message;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expireAt;
    }

    public int getRemainingSeconds() {
        long remaining = expireAt - System.currentTimeMillis();
        return remaining <= 0 ? 0 : (int) Math.ceil(remaining / 1000.0);
    }
}
