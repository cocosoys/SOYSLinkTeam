package soys.soyslinkteam.spring.entity.dto;

/**
 * 拒绝入队申请请求体。
 */
public class DenyDTO {

    private String teamId;
    private String playerId;
    private String reason;

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
