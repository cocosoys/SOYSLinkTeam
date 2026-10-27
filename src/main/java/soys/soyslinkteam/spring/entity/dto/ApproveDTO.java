package soys.soyslinkteam.spring.entity.dto;

/**
 * 批准入队申请请求体。
 */
public class ApproveDTO {

    private String teamId;
    private String playerId;
    private String playerName;

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

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
}
