package soys.soyslinkteam.team;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * 队伍成员。
 * <p>玩家名仅作缓存用于离线展示，判定一律以 UUID 为准。</p>
 */
public class TeamMember {

    private final UUID uuid;
    private volatile String name;
    private volatile TeamRole role;
    private final long joinedAt;
    private volatile long lastSeen;

    public TeamMember(UUID uuid, String name, TeamRole role, long joinedAt, long lastSeen) {
        this.uuid = uuid;
        this.name = name;
        this.role = role == null ? TeamRole.MEMBER : role;
        this.joinedAt = joinedAt;
        this.lastSeen = lastSeen;
    }

    public static TeamMember of(Player player, TeamRole role) {
        long now = System.currentTimeMillis();
        return new TeamMember(player.getUniqueId(), player.getName(), role, now, now);
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name == null ? uuid.toString().substring(0, 8) : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TeamRole getRole() {
        return role;
    }

    public void setRole(TeamRole role) {
        this.role = role == null ? TeamRole.MEMBER : role;
    }

    public long getJoinedAt() {
        return joinedAt;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public void touchLastSeen() {
        this.lastSeen = System.currentTimeMillis();
    }

    /**
     * 获取在线的 Player 实例，离线时返回 null。
     */
    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public boolean isOnline() {
        Player player = getPlayer();
        return player != null && player.isOnline();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TeamMember)) {
            return false;
        }
        return uuid.equals(((TeamMember) obj).uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
}
