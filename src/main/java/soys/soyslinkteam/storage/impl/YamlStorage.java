package soys.soyslinkteam.storage.impl;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.storage.DataStorage;
import soys.soyslinkteam.storage.StorageType;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.team.TeamMember;
import soys.soyslinkteam.team.TeamRole;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * YAML 文件存储后端。
 * <p>
 * 零外部依赖，默认启用。所有队伍写在同一个 teams.yml 中，
 * 通过整对象加锁保证并发安全。适用于中小型服务器。
 * </p>
 */
public class YamlStorage implements DataStorage {

    private static final String ROOT = "teams";

    private final SOYSLinkTeam plugin;
    private final Object lock = new Object();

    private File file;
    private YamlConfiguration config;
    private boolean available = false;
    private boolean backupOnSave = false;

    public YamlStorage(SOYSLinkTeam plugin) {
        this.plugin = plugin;
    }

    @Override
    public StorageType getType() {
        return StorageType.YAML;
    }

    @Override
    public void initialize() throws Exception {
        ConfigurationSection section = plugin.getConfigManager().getBackendSection("yaml");
        String path = section == null ? "data/teams.yml" : section.getString("file", "data/teams.yml");
        this.backupOnSave = section != null && section.getBoolean("backup-on-save", false);

        this.file = new File(plugin.getDataFolder(), path);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("无法创建数据目录: " + parent.getAbsolutePath());
        }
        if (!file.exists() && !file.createNewFile()) {
            throw new IOException("无法创建数据文件: " + file.getAbsolutePath());
        }
        synchronized (lock) {
            this.config = YamlConfiguration.loadConfiguration(file);
            if (!config.isConfigurationSection(ROOT)) {
                config.createSection(ROOT);
            }
        }
        this.available = true;
    }

    @Override
    public void shutdown() {
        synchronized (lock) {
            try {
                if (config != null && file != null) {
                    config.save(file);
                }
            } catch (IOException e) {
                plugin.getLogger().warning("[YAML] 关闭时保存失败: " + e.getMessage());
            }
            available = false;
        }
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public String describe() {
        return file == null ? "未初始化" : file.getPath().replace('\\', '/');
    }

    // ================================================================
    //  读
    // ================================================================

    @Override
    public Team loadTeam(UUID teamId) {
        synchronized (lock) {
            ConfigurationSection section = config.getConfigurationSection(ROOT + "." + teamId);
            return section == null ? null : deserialize(teamId, section);
        }
    }

    @Override
    public Collection<Team> loadAllTeams() {
        synchronized (lock) {
            Collection<Team> teams = new ArrayList<>();
            ConfigurationSection root = config.getConfigurationSection(ROOT);
            if (root == null) {
                return teams;
            }
            for (String key : root.getKeys(false)) {
                UUID id = parseUuid(key);
                if (id == null) {
                    continue;
                }
                ConfigurationSection section = root.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }
                Team team = deserialize(id, section);
                if (team != null) {
                    teams.add(team);
                }
            }
            return teams;
        }
    }

    @Override
    public Map<UUID, UUID> loadPlayerIndex() {
        synchronized (lock) {
            Map<UUID, UUID> index = new HashMap<>();
            ConfigurationSection root = config.getConfigurationSection(ROOT);
            if (root == null) {
                return index;
            }
            for (String key : root.getKeys(false)) {
                UUID teamId = parseUuid(key);
                if (teamId == null) {
                    continue;
                }
                ConfigurationSection members = root.getConfigurationSection(key + ".members");
                if (members == null) {
                    continue;
                }
                for (String memberKey : members.getKeys(false)) {
                    UUID playerId = parseUuid(memberKey);
                    if (playerId != null) {
                        index.put(playerId, teamId);
                    }
                }
            }
            return index;
        }
    }

    @Override
    public Map<UUID, String> loadTeamNames() {
        synchronized (lock) {
            Map<UUID, String> names = new HashMap<>();
            ConfigurationSection root = config.getConfigurationSection(ROOT);
            if (root == null) {
                return names;
            }
            for (String key : root.getKeys(false)) {
                UUID id = parseUuid(key);
                if (id == null) {
                    continue;
                }
                String name = root.getString(key + ".name");
                if (name != null) {
                    names.put(id, name);
                }
            }
            return names;
        }
    }

    @Override
    public Set<String> loadLowerCaseNames() {
        Set<String> set = new HashSet<>();
        for (String name : loadTeamNames().values()) {
            set.add(name.toLowerCase());
        }
        return set;
    }

    @Override
    public int countTeams() {
        synchronized (lock) {
            ConfigurationSection root = config.getConfigurationSection(ROOT);
            return root == null ? 0 : root.getKeys(false).size();
        }
    }

    // ================================================================
    //  写
    // ================================================================

    @Override
    public void saveTeam(Team team) throws Exception {
        synchronized (lock) {
            serialize(team);
            flush();
        }
    }

    @Override
    public void saveTeams(Collection<Team> teams) throws Exception {
        synchronized (lock) {
            for (Team team : teams) {
                serialize(team);
            }
            flush();
        }
    }

    @Override
    public void deleteTeam(UUID teamId) throws Exception {
        synchronized (lock) {
            config.set(ROOT + "." + teamId, null);
            flush();
        }
    }

    @Override
    public void clear() throws Exception {
        synchronized (lock) {
            config.set(ROOT, null);
            config.createSection(ROOT);
            flush();
        }
    }

    // ================================================================
    //  内部
    // ================================================================

    private void flush() throws IOException {
        if (backupOnSave && file.exists()) {
            File backup = new File(file.getParentFile(), file.getName() + ".bak");
            try {
                Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                plugin.getLogger().warning("[YAML] 备份失败: " + e.getMessage());
            }
        }
        config.save(file);
    }

    private void serialize(Team team) {
        String base = ROOT + "." + team.getId();
        config.set(base + ".name", team.getName());
        config.set(base + ".leader", team.getLeader() == null ? null : team.getLeader().toString());
        config.set(base + ".created-at", team.getCreatedAt());
        config.set(base + ".settings.open", team.getSettings().isOpen());
        config.set(base + ".settings.password", team.getSettings().getPassword());
        config.set(base + ".settings.notice", team.getSettings().getNotice());
        config.set(base + ".settings.tag", team.getSettings().getTag());
        config.set(base + ".settings.economy-cost", team.getSettings().getEconomyCost());

        // 整体重写成员节点，避免残留已退出的成员
        config.set(base + ".members", null);
        for (TeamMember member : team.getMembers()) {
            String path = base + ".members." + member.getUuid();
            config.set(path + ".name", member.getName());
            config.set(path + ".role", member.getRole().name());
            config.set(path + ".joined-at", member.getJoinedAt());
            config.set(path + ".last-seen", member.getLastSeen());
            config.set(path + ".chat-channel", member.isChatChannel());
        }
    }

    private Team deserialize(UUID id, ConfigurationSection section) {
        String name = section.getString("name");
        if (name == null) {
            return null;
        }
        UUID leader = parseUuid(section.getString("leader"));
        long createdAt = section.getLong("created-at", System.currentTimeMillis());

        Team team = new Team(id, name, leader, createdAt);
        team.getSettings().setOpen(section.getBoolean("settings.open", false));
        team.getSettings().setPassword(section.getString("settings.password"));
        team.getSettings().setNotice(section.getString("settings.notice", ""));
        team.getSettings().setTag(section.getString("settings.tag", ""));
        team.getSettings().setEconomyCost(section.getDouble("settings.economy-cost", 0.0));

        ConfigurationSection members = section.getConfigurationSection("members");
        if (members != null) {
            for (String key : members.getKeys(false)) {
                UUID memberId = parseUuid(key);
                if (memberId == null) {
                    continue;
                }
                team.addMember(new TeamMember(
                        memberId,
                        members.getString(key + ".name", "未知"),
                        TeamRole.parse(members.getString(key + ".role")),
                        members.getLong(key + ".joined-at", createdAt),
                        members.getLong(key + ".last-seen", createdAt),
                        members.getBoolean(key + ".chat-channel", false)));
            }
        }
        team.clearDirty();
        return team;
    }

    private UUID parseUuid(String input) {
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
