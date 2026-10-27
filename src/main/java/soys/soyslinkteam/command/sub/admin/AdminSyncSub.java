package soys.soyslinkteam.command.sub.admin;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.command.SubCommand;

import java.util.Arrays;
import java.util.List;

/**
 * /teamadmin sync — 将主存储数据覆盖同步到所有辅助存储。
 */
public class AdminSyncSub extends SubCommand {

    public AdminSyncSub(SOYSLinkTeam plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "sync";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("同步");
    }

    @Override
    public String getPermission() {
        return "soyslinkteam.admin.migrate";
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        if (plugin.getStorageManager().getSecondaries().isEmpty()) {
            msg(sender, "soyslinkteam.admin.sync.no-secondary");
            return;
        }

        msg(sender, "soyslinkteam.admin.sync.started");

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                long start = System.currentTimeMillis();
                int count = plugin.getStorageManager().syncToSecondaries();
                long ms = System.currentTimeMillis() - start;
                int secondaries = plugin.getStorageManager().getSecondaries().size();
                Bukkit.getScheduler().runTask(plugin, () -> msg(sender, "soyslinkteam.admin.sync.success",
                        count, secondaries, ms));
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> msg(sender, "soyslinkteam.admin.sync.failed",
                        e.getMessage() == null ? "未知错误" : e.getMessage()));
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "同步失败", e);
            }
        });
    }
}
