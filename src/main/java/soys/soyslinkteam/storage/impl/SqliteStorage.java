package soys.soyslinkteam.storage.impl;

import org.bukkit.configuration.ConfigurationSection;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.storage.StorageType;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * SQLite 存储后端。
 * <p>单文件数据库，无需额外服务，适合需要 SQL 查询能力但不想部署 MySQL 的场景。</p>
 */
public class SqliteStorage extends SqlStorage {

    private File databaseFile;

    public SqliteStorage(SOYSLinkTeam plugin) {
        super(plugin);
    }

    @Override
    public StorageType getType() {
        return StorageType.SQLITE;
    }

    @Override
    public void initialize() throws Exception {
        ConfigurationSection section = plugin.getConfigManager().getBackendSection("sqlite");
        String path = section == null ? "data/teams.db" : section.getString("file", "data/teams.db");
        this.tablePrefix = section == null ? "mc_slt_" : section.getString("table-prefix", "mc_slt_");

        this.databaseFile = new File(plugin.getDataFolder(), path);
        File parent = databaseFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("无法创建数据目录: " + parent.getAbsolutePath());
        }
        super.initialize();
    }

    @Override
    public String describe() {
        return databaseFile == null ? "未初始化" : databaseFile.getPath().replace('\\', '/');
    }

    @Override
    protected String getDriverClass() {
        return "org.sqlite.JDBC";
    }

    @Override
    protected Connection createConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(
                "jdbc:sqlite:" + databaseFile.getAbsolutePath());
        // 开启外键与 WAL，提升并发读性能
        try (java.sql.Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA journal_mode = WAL");
        }
        return connection;
    }

    @Override
    protected String[] getSchemaStatements() {
        return new String[]{
                "CREATE TABLE IF NOT EXISTS " + teamsTable() + " ("
                        + "id TEXT NOT NULL PRIMARY KEY,"
                        + "name TEXT NOT NULL,"
                        + "leader TEXT,"
                        + "created_at INTEGER NOT NULL,"
                        + "is_open INTEGER NOT NULL DEFAULT 0,"
                        + "join_password TEXT,"
                        + "notice TEXT,"
                        + "tag TEXT"
                        + ")",
                "CREATE TABLE IF NOT EXISTS " + membersTable() + " ("
                        + "team_id TEXT NOT NULL,"
                        + "player_uuid TEXT NOT NULL,"
                        + "player_name TEXT,"
                        + "role TEXT NOT NULL,"
                        + "joined_at INTEGER NOT NULL,"
                        + "last_seen INTEGER NOT NULL,"
                        + "PRIMARY KEY (team_id, player_uuid)"
                        + ")",
                "CREATE INDEX IF NOT EXISTS idx_" + tablePrefix + "members_uuid"
                        + " ON " + membersTable() + " (player_uuid)",
                "CREATE INDEX IF NOT EXISTS idx_" + tablePrefix + "teams_name"
                        + " ON " + teamsTable() + " (name)"
        };
    }
}
