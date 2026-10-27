package soys.soyslinkteam.spring.entity.dto;

/**
 * 更新队伍配置请求体。
 *
 * <p>字段为 null 表示本次不修改该项；password 为 "off" 或空串表示清除口令；
 * cost 为 null/"off" 表示清除入队费用。</p>
 */
public class TeamConfigDTO {

    private String teamId;
    private String name;
    private Boolean open;
    private String password;
    private Object cost;
    private String notice;

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getOpen() {
        return open;
    }

    public void setOpen(Boolean open) {
        this.open = open;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Object getCost() {
        return cost;
    }

    public void setCost(Object cost) {
        this.cost = cost;
    }

    public String getNotice() {
        return notice;
    }

    public void setNotice(String notice) {
        this.notice = notice;
    }
}
