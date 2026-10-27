package soys.soyslinkteam.spring.entity.dto;

/**
 * 移除队伍增幅请求体。
 */
public class BuffRemoveDTO {

    private String teamId;
    private String buffId;

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getBuffId() {
        return buffId;
    }

    public void setBuffId(String buffId) {
        this.buffId = buffId;
    }
}
