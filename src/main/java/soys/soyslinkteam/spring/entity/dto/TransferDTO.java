package soys.soyslinkteam.spring.entity.dto;

/**
 * 更换队长请求体。
 */
public class TransferDTO {

    private String teamId;
    private String playerId;

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
}
