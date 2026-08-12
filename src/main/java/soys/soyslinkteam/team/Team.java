package soys.soyslinkteam.team;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 队伍实体。
 * <p>
 * 该对象可能被主线程与异步存储线程同时访问，因此成员集合使用并发容器，
 * 可变标量字段使用 volatile。所有会改变持久化内容的写操作都会置 dirty 标记。
 * </p>
 */
public class Team {

    /** 队伍唯一 ID，创建后不可变，作为存储主键 */
    private final UUID id;

    private volatile String name;
    private volatile UUID leader;

    private final Map<UUID, TeamMember> members = new ConcurrentHashMap<>();
    private final TeamSettings settings = new TeamSettings();

    private final long createdAt;

    /** 队伍最后一次活动时间，用于内存卸载判定，不参与持久化对比 */
    private volatile long lastActiveAt;

    /** 是否存在未落盘的修改 */
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    /** 队伍是否已被解散（防止卸载任务把已删除队伍写回） */
    private volatile boolean disbanded = false;

    public Team(UUID id, String name, UUID leader, long createdAt) {
        this.id = id;
        this.name = name;
        this.leader = leader;
        this.createdAt = createdAt;
        this.lastActiveAt = System.currentTimeMillis();
    }

    /**
     * 创建一支新队伍，创建者自动成为队长。
     */
    public static Team create(String name, Player leader) {
        long now = System.currentTimeMillis();
        Team team = new Team(UUID.randomUUID(), name, leader.getUniqueId(), now);
        team.members.put(leader.getUniqueId(), TeamMember.of(leader, TeamRole.LEADER));
        team.markDirty();
        return team;
    }

    // ================================================================
    //  基础属性
    // ================================================================

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        markDirty();
    }

    public UUID getLeader() {
        return leader;
    }

    public TeamMember getLeaderMember() {
        return members.get(leader);
    }

    public String getLeaderName() {
        TeamMember member = getLeaderMember();
        return member == null ? "未知" : member.getName();
    }

    public boolean isLeader(UUID uuid) {
        return leader != null && leader.equals(uuid);
    }

    public TeamSettings getSettings() {
        return settings;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    // ================================================================
    //  成员管理
    // ================================================================

    public Collection<TeamMember> getMembers() {
        return Collections.unmodifiableCollection(members.values());
    }

    /**
     * 获取按角色权重降序、入队时间升序排序的成员列表。
     */
    public List<TeamMember> getSortedMembers() {
        List<TeamMember> list = new ArrayList<>(members.values());
        list.sort(Comparator
                .comparingInt((TeamMember m) -> -m.getRole().getWeight())
                .thenComparingLong(TeamMember::getJoinedAt));
        return list;
    }

    public TeamMember getMember(UUID uuid) {
        return members.get(uuid);
    }

    /**
     * 按名称查找成员（忽略大小写）。
     */
    public TeamMember getMemberByName(String name) {
        if (name == null) {
            return null;
        }
        for (TeamMember member : members.values()) {
            if (name.equalsIgnoreCase(member.getName())) {
                return member;
            }
        }
        return null;
    }

    public boolean hasMember(UUID uuid) {
        return members.containsKey(uuid);
    }

    public int getSize() {
        return members.size();
    }

    public int getOnlineCount() {
        int count = 0;
        for (TeamMember member : members.values()) {
            if (member.isOnline()) {
                count++;
            }
        }
        return count;
    }

    public List<Player> getOnlinePlayers() {
        List<Player> players = new ArrayList<>();
        for (TeamMember member : members.values()) {
            Player player = member.getPlayer();
            if (player != null && player.isOnline()) {
                players.add(player);
            }
        }
        return players;
    }

    public boolean hasOnlineMember() {
        for (TeamMember member : members.values()) {
            if (member.isOnline()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 直接添加成员（不做任何规则校验，校验由上层服务完成）。
     */
    public void addMember(TeamMember member) {
        members.put(member.getUuid(), member);
        touch();
        markDirty();
    }

    /**
     * 移除成员。
     *
     * @return 被移除的成员，不存在时返回 null
     */
    public TeamMember removeMember(UUID uuid) {
        TeamMember removed = members.remove(uuid);
        if (removed != null) {
            touch();
            markDirty();
        }
        return removed;
    }

    /**
     * 设置成员角色。
     */
    public void setRole(UUID uuid, TeamRole role) {
        TeamMember member = members.get(uuid);
        if (member != null) {
            member.setRole(role);
            markDirty();
        }
    }

    /**
     * 转让队长。原队长降级为普通队员。
     *
     * @return 转让是否成功
     */
    public boolean transferLeader(UUID newLeader) {
        TeamMember target = members.get(newLeader);
        if (target == null) {
            return false;
        }
        TeamMember old = members.get(leader);
        if (old != null) {
            old.setRole(TeamRole.MEMBER);
        }
        target.setRole(TeamRole.LEADER);
        this.leader = newLeader;
        touch();
        markDirty();
        return true;
    }

    /**
     * 统计指定角色的成员数量。
     */
    public int countRole(TeamRole role) {
        int count = 0;
        for (TeamMember member : members.values()) {
            if (member.getRole() == role) {
                count++;
            }
        }
        return count;
    }

    /**
     * 挑选一个最合适的继任队长：优先副队长，其次入队最早者。
     */
    public TeamMember pickSuccessor(UUID excluding) {
        TeamMember best = null;
        for (TeamMember member : members.values()) {
            if (member.getUuid().equals(excluding)) {
                continue;
            }
            if (best == null) {
                best = member;
                continue;
            }
            int weightDiff = member.getRole().getWeight() - best.getRole().getWeight();
            if (weightDiff > 0 || (weightDiff == 0 && member.getJoinedAt() < best.getJoinedAt())) {
                best = member;
            }
        }
        return best;
    }

    // ================================================================
    //  活跃状态
    // ================================================================

    public long getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(long lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }

    /**
     * 刷新活跃时间，延后内存卸载。
     */
    public void touch() {
        this.lastActiveAt = System.currentTimeMillis();
    }

    /**
     * 队伍是否已超过给定的空闲时长。
     */
    public boolean isIdle(long timeoutMillis) {
        return System.currentTimeMillis() - lastActiveAt > timeoutMillis;
    }

    // ================================================================
    //  脏标记与生命周期
    // ================================================================

    public boolean isDirty() {
        return dirty.get();
    }

    public void markDirty() {
        dirty.set(true);
    }

    /**
     * 清除脏标记，仅应由存储层在成功落盘后调用。
     */
    public void clearDirty() {
        dirty.set(false);
    }

    public boolean isDisbanded() {
        return disbanded;
    }

    public void markDisbanded() {
        this.disbanded = true;
    }

    // ================================================================
    //  广播
    // ================================================================

    /**
     * 向队伍内所有在线成员发送消息。
     */
    public void broadcast(String message) {
        broadcast(message, null);
    }

    /**
     * 向队伍内所有在线成员发送消息，可排除某个玩家。
     */
    public void broadcast(String message, UUID excluding) {
        if (message == null || message.isEmpty()) {
            return;
        }
        for (TeamMember member : members.values()) {
            if (excluding != null && member.getUuid().equals(excluding)) {
                continue;
            }
            Player player = member.getPlayer();
            if (player != null && player.isOnline()) {
                player.sendMessage(soys.soyslinkteam.util.Text.color(message));
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Team)) {
            return false;
        }
        return id.equals(((Team) obj).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Team{" + name + ", id=" + id + ", size=" + members.size() + "}";
    }
}
