package soys.soyslinkteam.spring.entity.vo;

import java.util.List;

/**
 * 队伍详情视图（含设置、成员、当前增幅）。
 */
public class TeamDetailVO {

    private String id;
    private String name;
    private String leader;
    private String leaderName;
    private int size;
    private int onlineCount;
    private int maxSize;
    private long createdAt;
    private String createdAtText;
    private long lastActiveAt;
    private String lastActiveText;

    /** 队伍设置（配置面板展示与回显） */
    private boolean open;
    private boolean hasPassword;
    private String notice;
    private String tag;
    private double cost;
    private boolean hasCost;

    private List<MemberVO> members;
    private List<BuffVO> buffs;

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

    public long getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(long lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }

    public String getLastActiveText() {
        return lastActiveText;
    }

    public void setLastActiveText(String lastActiveText) {
        this.lastActiveText = lastActiveText;
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

    public String getNotice() {
        return notice;
    }

    public void setNotice(String notice) {
        this.notice = notice;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public boolean isHasCost() {
        return hasCost;
    }

    public void setHasCost(boolean hasCost) {
        this.hasCost = hasCost;
    }

    public List<MemberVO> getMembers() {
        return members;
    }

    public void setMembers(List<MemberVO> members) {
        this.members = members;
    }

    public List<BuffVO> getBuffs() {
        return buffs;
    }

    public void setBuffs(List<BuffVO> buffs) {
        this.buffs = buffs;
    }
}
