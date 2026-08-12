package soys.soyslinkteam.storage.impl;

import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.storage.DataStorage;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.team.TeamMember;
import soys.soyslinkteam.team.TeamRole;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * SQL 存储后端的公共实现。
 * <p>
 * SQLite 与 MySQL 共用同一套表结构与 CRUD 逻辑，子类只需提供：
 * 驱动类名、JDBC URL、连接创建方式与建表语句方言。
 * </p>
 * <p>
 * 连接策略：维持单个长连接并在每次使用前做有效性探测，配合对象锁串行化访问。
 * 插件的写操作本身已经被 StorageManager 收敛到异步队列，无需引入连接池。
 * </p>
 */
public abstract class SqlStorage implements DataStorage {

    protected final SOYSLinkTeam plugin;
    protected final Object lock = new Object();

    protected String tablePrefix = "slt_";
    protected volatile boolean available = false;

    private Connection connection;

    protected SqlStorage(SOYSLinkTeam plugin) {
        this.plugin = plugin;
    }

    // ================================================================
    //  子类需实现的方言部分
    // ================================================================

    /** JDBC 驱动类名 */
    protected abstract String getDriverClass();

    /** 创建一个全新的数据库连接 */
    protected abstract Connection createConnection() throws SQLException;

    /** 建表与建索引语句，按顺序执行 */
    protected abstract String[] getSchemaStatements();

    // ================================================================
    //  表名
    // ================================================================

    protected String teamsTable() {
        return tablePrefix + "teams";
    }

    protected String membersTable() {
        return tablePrefix + "members";
    }

    // ================================================================
    //  生命周期
    // ================================================================

    @Override
    public void initialize() throws Exception {
        try {
            Class.forName(getDriverClass());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("未找到 JDBC 驱动 " + getDriverClass()
                    + "，请确认服务端已提供该驱动或手动放入 libraries 目录");
        }
        synchronized (lock) {
            connection = createConnection();
            try (Statement statement = connection.createStatement()) {
                for (String sql : getSchemaStatements()) {
                    statement.execute(sql);
                }
            }
        }
        available = true;
    }

    @Override
    public void shutdown() {
        synchronized (lock) {
            available = false;
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ignored) {
                    // 关闭失败无需处理
                }
                connection = null;
            }
        }
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    /**
     * 获取一个可用连接，失效时自动重建。调用方必须持有 {@link #lock}。
     */
    protected Connection connection() throws SQLException {
        if (connection == null || connection.isClosed() || !connection.isValid(3)) {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ignored) {
                    // 旧连接关闭失败可忽略
                }
            }
            connection = createConnection();
        }
        return connection;
    }

    /**
     * 主动探测连接（保活任务使用）。
     */
    public void keepAlive() {
        synchronized (lock) {
            try {
                connection().isValid(3);
            } catch (SQLException e) {
                plugin.getLogger().warning("[" + getType().getId() + "] 保活探测失败: " + e.getMessage());
            }
        }
    }

    // ================================================================
    //  读
    // ================================================================

    @Override
    public Team loadTeam(UUID teamId) throws Exception {
        synchronized (lock) {
            Connection conn = connection();
            Team team = null;
            String sql = "SELECT * FROM " + teamsTable() + " WHERE id = ?";
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, teamId.toString());
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        team = readTeam(rs);
                    }
                }
            }
            if (team == null) {
                return null;
            }
            loadMembersInto(conn, team);
            team.clearDirty();
            return team;
        }
    }

    @Override
    public Collection<Team> loadAllTeams() throws Exception {
        synchronized (lock) {
            Connection conn = connection();
            Map<UUID, Team> teams = new HashMap<>();

            try (Statement statement = conn.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT * FROM " + teamsTable())) {
                while (rs.next()) {
                    Team team = readTeam(rs);
                    if (team != null) {
                        teams.put(team.getId(), team);
                    }
                }
            }

            try (Statement statement = conn.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT * FROM " + membersTable())) {
                while (rs.next()) {
                    UUID teamId = parseUuid(rs.getString("team_id"));
                    Team team = teamId == null ? null : teams.get(teamId);
                    if (team != null) {
                        TeamMember member = readMember(rs);
                        if (member != null) {
                            team.addMember(member);
                        }
                    }
                }
            }

            for (Team team : teams.values()) {
                team.clearDirty();
            }
            return new ArrayList<>(teams.values());
        }
    }

    @Override
    public Map<UUID, UUID> loadPlayerIndex() throws Exception {
        synchronized (lock) {
            Map<UUID, UUID> index = new HashMap<>();
            String sql = "SELECT player_uuid, team_id FROM " + membersTable();
            try (Statement statement = connection().createStatement();
                 ResultSet rs = statement.executeQuery(sql)) {
                while (rs.next()) {
                    UUID player = parseUuid(rs.getString("player_uuid"));
                    UUID team = parseUuid(rs.getString("team_id"));
                    if (player != null && team != null) {
                        index.put(player, team);
                    }
                }
            }
            return index;
        }
    }

    @Override
    public Map<UUID, String> loadTeamNames() throws Exception {
        synchronized (lock) {
            Map<UUID, String> names = new HashMap<>();
            String sql = "SELECT id, name FROM " + teamsTable();
            try (Statement statement = connection().createStatement();
                 ResultSet rs = statement.executeQuery(sql)) {
                while (rs.next()) {
                    UUID id = parseUuid(rs.getString("id"));
                    if (id != null) {
                        names.put(id, rs.getString("name"));
                    }
                }
            }
            return names;
        }
    }

    @Override
    public Set<String> loadLowerCaseNames() throws Exception {
        Set<String> set = new HashSet<>();
        for (String name : loadTeamNames().values()) {
            if (name != null) {
                set.add(name.toLowerCase());
            }
        }
        return set;
    }

    @Override
    public int countTeams() throws Exception {
        synchronized (lock) {
            try (Statement statement = connection().createStatement();
                 ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM " + teamsTable())) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // ================================================================
    //  写
    // ================================================================

    @Override
    public void saveTeam(Team team) throws Exception {
        synchronized (lock) {
            Connection conn = connection();
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                writeTeam(conn, team);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        }
    }

    @Override
    public void saveTeams(Collection<Team> teams) throws Exception {
        if (teams.isEmpty()) {
            return;
        }
        synchronized (lock) {
            Connection conn = connection();
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                for (Team team : teams) {
                    writeTeam(conn, team);
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        }
    }

    @Override
    public void deleteTeam(UUID teamId) throws Exception {
        synchronized (lock) {
            Connection conn = connection();
            try (PreparedStatement statement =
                         conn.prepareStatement("DELETE FROM " + membersTable() + " WHERE team_id = ?")) {
                statement.setString(1, teamId.toString());
                statement.executeUpdate();
            }
            try (PreparedStatement statement =
                         conn.prepareStatement("DELETE FROM " + teamsTable() + " WHERE id = ?")) {
                statement.setString(1, teamId.toString());
                statement.executeUpdate();
            }
        }
    }

    @Override
    public void clear() throws Exception {
        synchronized (lock) {
            try (Statement statement = connection().createStatement()) {
                statement.executeUpdate("DELETE FROM " + membersTable());
                statement.executeUpdate("DELETE FROM " + teamsTable());
            }
        }
    }

    // ================================================================
    //  内部
    // ================================================================

    private void writeTeam(Connection conn, Team team) throws SQLException {
        String teamSql = "REPLACE INTO " + teamsTable()
                + " (id, name, leader, created_at, is_open, join_password, notice, tag)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = conn.prepareStatement(teamSql)) {
            statement.setString(1, team.getId().toString());
            statement.setString(2, team.getName());
            statement.setString(3, team.getLeader() == null ? null : team.getLeader().toString());
            statement.setLong(4, team.getCreatedAt());
            statement.setInt(5, team.getSettings().isOpen() ? 1 : 0);
            statement.setString(6, team.getSettings().getPassword());
            statement.setString(7, team.getSettings().getNotice());
            statement.setString(8, team.getSettings().getTag());
            statement.executeUpdate();
        }

        // 整体重写成员，保证已退出的成员被清除
        try (PreparedStatement statement =
                     conn.prepareStatement("DELETE FROM " + membersTable() + " WHERE team_id = ?")) {
            statement.setString(1, team.getId().toString());
            statement.executeUpdate();
        }

        String memberSql = "REPLACE INTO " + membersTable()
                + " (team_id, player_uuid, player_name, role, joined_at, last_seen)"
                + " VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = conn.prepareStatement(memberSql)) {
            for (TeamMember member : team.getMembers()) {
                statement.setString(1, team.getId().toString());
                statement.setString(2, member.getUuid().toString());
                statement.setString(3, member.getName());
                statement.setString(4, member.getRole().name());
                statement.setLong(5, member.getJoinedAt());
                statement.setLong(6, member.getLastSeen());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void loadMembersInto(Connection conn, Team team) throws SQLException {
        String sql = "SELECT * FROM " + membersTable() + " WHERE team_id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, team.getId().toString());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    TeamMember member = readMember(rs);
                    if (member != null) {
                        team.addMember(member);
                    }
                }
            }
        }
    }

    private Team readTeam(ResultSet rs) throws SQLException {
        UUID id = parseUuid(rs.getString("id"));
        if (id == null) {
            return null;
        }
        Team team = new Team(
                id,
                rs.getString("name"),
                parseUuid(rs.getString("leader")),
                rs.getLong("created_at"));
        team.getSettings().setOpen(rs.getInt("is_open") == 1);
        team.getSettings().setPassword(rs.getString("join_password"));
        team.getSettings().setNotice(rs.getString("notice"));
        team.getSettings().setTag(rs.getString("tag"));
        return team;
    }

    private TeamMember readMember(ResultSet rs) throws SQLException {
        UUID uuid = parseUuid(rs.getString("player_uuid"));
        if (uuid == null) {
            return null;
        }
        return new TeamMember(
                uuid,
                rs.getString("player_name"),
                TeamRole.parse(rs.getString("role")),
                rs.getLong("joined_at"),
                rs.getLong("last_seen"));
    }

    protected UUID parseUuid(String input) {
        if (input == null) {
            return null;
        }
        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
