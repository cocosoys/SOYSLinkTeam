package soys.soyslinkteam.spring.entity.vo;

/**
 * 队伍列表行视图。
 *
 * <p>对应原 TeamWebDtos.teamSummary；未装载队伍时 loaded=false，size/onlineCount 为 -1。</p>
 */
public class TeamRowVO {

    private String id;
    private String name;
    private String leader;
    private String leaderName;
    private int size;
    private int onlineCount;
    private int maxSize;
    private boolean open;
    private boolean hasPassword;
    private boolean hasCost;
    private double cost;
    private String tag;
    private int buffCount;
    private int applicationCount;
    private long createdAt;
    private String createdAtText;
    private boolean loaded;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLeader() {
        return leader;
    }

    public void setLeader(String leader) {
        this.leader = leader;
    }

    public String getLeaderName() {
        return leaderName;
    }

    public void setLeaderName(String leaderName) {
        this.leaderName = leaderName;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getOnlineCount() {
        return onlineCount;
    }

    public void setOnlineCount(int onlineCount) {
        this.onlineCount = onlineCount;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public boolean isHasPassword() {
        return hasPassword;
    }

    public void setHasPassword(boolean hasPassword) {
        this.hasPassword = hasPassword;
    }

    public boolean isHasCost() {
        return hasCost;
    }

    public void setHasCost(boolean hasCost) {
        this.hasCost = hasCost;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public int getBuffCount() {
        return buffCount;
    }

    public void setBuffCount(int buffCount) {
        this.buffCount = buffCount;
    }

    public int getApplicationCount() {
        return applicationCount;
    }

    public void setApplicationCount(int applicationCount) {
        this.applicationCount = applicationCount;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedAtText() {
        return createdAtText;
    }

    public void setCreatedAtText(String createdAtText) {
        this.createdAtText = createdAtText;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }
}
