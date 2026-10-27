package soys.soyslinkteam.spring.entity.dto;

/**
 * 添加队伍增幅请求体。
 */
public class BuffAddDTO {

    private String teamId;
    private String kind;
    private String effectKey;
    private String displayName;
    private int amplifier;
    private Long duration;

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getEffectKey() {
        return effectKey;
    }

    public void setEffectKey(String effectKey) {
        this.effectKey = effectKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getAmplifier() {
        return amplifier;
    }

    public void setAmplifier(int amplifier) {
        this.amplifier = amplifier;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }
}
