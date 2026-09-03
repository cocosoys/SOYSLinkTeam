package soys.soyslinkteam;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import soys.soyslinkteam.command.AdminCommand;
import soys.soyslinkteam.command.CommandDispatcher;
import soys.soyslinkteam.command.TeamCommand;
import soys.soyslinkteam.config.ConfigManager;
import soys.soyslinkteam.config.MessageManager;
import soys.soyslinkteam.join.InviteManager;
import soys.soyslinkteam.join.JoinMethodRegistry;
import soys.soyslinkteam.chat.ChatListener;
import soys.soyslinkteam.chat.TeamChannelManager;
import soys.soyslinkteam.nametag.NametagManager;
import soys.soyslinkteam.permission.PermissionManager;
import soys.soyslinkteam.storage.StorageManager;
import soys.soyslinkteam.team.TeamManager;
import soys.soyslinkteam.util.CooldownManager;
import soys.soyslinkteam.util.NameValidator;

import java.util.logging.Level;

/**
 * SOYSLinkTeam 主类。
 * <p>负责编排各模块的生命周期：配置、存储、内存激活池、权限、入队方式、指令与 PAPI 扩展。</p>
 */
public final class SOYSLinkTeam extends JavaPlugin {

    private static SOYSLinkTeam instance;

    private ConfigManager configManager;
    private MessageManager messageManager;
    private NameValidator nameValidator;
    private CooldownManager cooldownManager;
    private StorageManager storageManager;
    private TeamManager teamManager;
    private PermissionManager permissionManager;
    private JoinMethodRegistry joinMethodRegistry;
    private InviteManager inviteManager;
    private PlaceholderHook placeholderHook;
    private TeamChannelManager teamChannelManager;
    private NametagManager nametagManager;
    private soys.soyslinkteam.luckperms.LuckPermsContextHook luckPermsContextHook;

    public static SOYSLinkTeam getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        try {
            configManager = new ConfigManager(this);
            messageManager = new MessageManager(this);
            nameValidator = new NameValidator(this);
            cooldownManager = new CooldownManager();

            storageManager = new StorageManager(this);
            storageManager.initialize();

            teamManager = new TeamManager(this);
            teamManager.initialize();

            permissionManager = new PermissionManager(this);
            permissionManager.initialize();

            joinMethodRegistry = new JoinMethodRegistry(this);
            joinMethodRegistry.initialize();

            inviteManager = new InviteManager(this);
            inviteManager.start();

            teamChannelManager = new TeamChannelManager(this);
            nametagManager = new NametagManager(this);
            nametagManager.initialize();

            registerCommands();
            registerListeners();
            registerPlaceholders();

            // LuckPerms 上下文注入（软依赖，不可用时自动停用）
            luckPermsContextHook = new soys.soyslinkteam.luckperms.LuckPermsContextHook(this);
            luckPermsContextHook.initialize();

            getLogger().info("SOYSLinkTeam 已启用，共加载 "
                    + teamManager.getTotalCount() + " 支队伍。");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "SOYSLinkTeam 启用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void onDisable() {
        if (nametagManager != null) {
            nametagManager.shutdown();
        }
        if (teamManager != null) {
            teamManager.shutdown();
        }
        if (inviteManager != null) {
            inviteManager.stop();
        }
        if (storageManager != null) {
            storageManager.shutdown();
        }
        if (placeholderHook != null) {
            try {
                placeholderHook.unregister();
            } catch (Throwable ignored) {
                // 卸载阶段忽略异常
            }
        }
        if (luckPermsContextHook != null) {
            luckPermsContextHook.shutdown();
        }
        getLogger().info("SOYSLinkTeam 已停用。");
    }

    private void registerCommands() {
        registerCommand("steam", new TeamCommand(this));
        registerCommand("teamadmin", new AdminCommand(this));
    }

    private void registerCommand(String name, CommandDispatcher dispatcher) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("未在 plugin.yml 中找到指令定义: " + name);
            return;
        }
        command.setExecutor(dispatcher);
        command.setTabCompleter(dispatcher);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(nametagManager, this);
    }

    private void registerPlaceholders() {
        try {
            placeholderHook = new PlaceholderHook(this);
            placeholderHook.register();
            getLogger().info("PlaceholderAPI 变量已注册。");
        } catch (Throwable t) {
            getLogger().warning("PlaceholderAPI 变量注册失败: " + t.getMessage());
        }
    }

    /**
     * 热重载配置与全部模块，供 /teamadmin reload 调用。
     * <p>会重建存储连接并刷新内存索引，建议在无人在线时执行。</p>
     */
    public void reloadConfiguration() {
        configManager.reload();
        messageManager.reload();
        permissionManager.reload();
        joinMethodRegistry.reload();
        storageManager.initialize();
        teamManager.reloadIndexes();
        if (nametagManager != null) {
            nametagManager.refreshAll();
        }
    }

    // ================================================================
    //  模块访问器
    // ================================================================

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public NameValidator getNameValidator() {
        return nameValidator;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    public JoinMethodRegistry getJoinMethodRegistry() {
        return joinMethodRegistry;
    }

    public InviteManager getInviteManager() {
        return inviteManager;
    }

    public TeamChannelManager getTeamChannelManager() {
        return teamChannelManager;
    }

    public NametagManager getNametagManager() {
        return nametagManager;
    }

    public soys.soyslinkteam.luckperms.LuckPermsContextHook getLuckPermsContextHook() {
        return luckPermsContextHook;
    }
}
