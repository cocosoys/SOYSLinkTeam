package soys.soyslinkteam.command.sub.admin;

import org.bukkit.command.CommandSender;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.command.SubCommand;
import soys.soyslinkteam.storage.StorageManager;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * /teamadmin backup [now] — 查看定时备份状态 / 立即生成一次快照。
 */
public class AdminBackupSub extends SubCommand {

    public AdminBackupSub(SOYSLinkTeam plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "backup";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("备份", "bak");
    }

    @Override
    public String getPermission() {
        return "soyslinkteam.admin.migrate";
    }

    @Override
    public boolean isPlayerOnly() {
        return false;
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        if (args.length >= 1 && "now".equalsIgnoreCase(args[0])) {
            runNow(sender);
            return;
        }
        showStatus(sender);
    }

    private void showStatus(CommandSender sender) {
        long minutes = plugin.getConfigManager().getBackupIntervalTicks() / 20 / 60;
        if (plugin.getConfigManager().isBackupEnabled()) {
            msg(sender, "soyslinkteam.admin.backup.status-enabled",
                    minutes,
                    plugin.getConfigManager().getBackupKeepLast(),
                    plugin.getConfigManager().getBackupDirectory());
        } else {
            msg(sender, "soyslinkteam.admin.backup.status-disabled");
        }
    }

    private void runNow(CommandSender sender) {
        StorageManager storage = plugin.getStorageManager();
        if (storage.getPrimary() == null) {
            msg(sender, "soyslinkteam.admin.backup.no-primary");
            return;
        }
        // 快照涉及 I/O，放异步线程执行，完成后回主线程反馈
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            File backup = storage.createBackup();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (backup == null) {
                    msg(sender, "soyslinkteam.admin.backup.now-failed");
                } else {
                    msg(sender, "soyslinkteam.admin.backup.now-success", backup.getName());
                }
            });
        });
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("now"), args[0]);
        }
        return null;
    }
}
