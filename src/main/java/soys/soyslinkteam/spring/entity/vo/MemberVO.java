package soys.soyslinkteam.spring.entity.vo;

/**
 * 队伍成员视图。
 */
public class MemberVO {

    private String uuid;
    private String name;
    private String role;
    private String roleDisplay;
    private boolean online;
    private long joinedAt;
    private String joinedAtText;
    private long lastSeen;
    private boolean chatChannel;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getRoleDisplay() {
        return roleDisplay;
    }

    public void setRoleDisplay(String roleDisplay) {
        this.roleDisplay = roleDisplay;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public long getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(long joinedAt) {
        this.joinedAt = joinedAt;
    }

    public String getJoinedAtText() {
        return joinedAtText;
    }

    public void setJoinedAtText(String joinedAtText) {
        this.joinedAtText = joinedAtText;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public boolean isChatChannel() {
        return chatChannel;
    }

    public void setChatChannel(boolean chatChannel) {
        this.chatChannel = chatChannel;
    }
}
