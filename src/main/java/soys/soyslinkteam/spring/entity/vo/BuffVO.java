package soys.soyslinkteam.spring.entity.vo;

/**
 * 队伍增幅视图（信标式药水效果 / 属性类增幅）。
 */
public class BuffVO {

    private String id;
    private String kind;
    private String kindDisplay;
    private String effectKey;
    private String displayName;
    private int amplifier;
    private int level;
    private String levelDisplay;
    private long duration;
    private long remaining;
    private String remainingText;
    private long startedAt;
    private String grantedBy;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getKindDisplay() {
        return kindDisplay;
    }

    public void setKindDisplay(String kindDisplay) {
        this.kindDisplay = kindDisplay;
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

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getLevelDisplay() {
        return levelDisplay;
    }

    public void setLevelDisplay(String levelDisplay) {
        this.levelDisplay = levelDisplay;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getRemaining() {
        return remaining;
    }

    public void setRemaining(long remaining) {
        this.remaining = remaining;
    }

    public String getRemainingText() {
        return remainingText;
    }

    public void setRemainingText(String remainingText) {
        this.remainingText = remainingText;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }

    public String getGrantedBy() {
        return grantedBy;
    }

    public void setGrantedBy(String grantedBy) {
        this.grantedBy = grantedBy;
    }
}
