package soys.soyslinkteam.command.sub.admin;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.command.SubCommand;
import soys.soyslinkteam.storage.DataStorage;
import soys.soyslinkteam.util.Placeholders;

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
            msg(sender, "admin.sync.no-secondary", null);
            return;
        }

        msg(sender, "admin.sync.started", null);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                long start = System.currentTimeMillis();
                int count = plugin.getStorageManager().syncToSecondaries();
                long ms = System.currentTimeMillis() - start;
                String targets = buildTargets();
                Bukkit.getScheduler().runTask(plugin, () -> msg(sender, "admin.sync.success",
                        Placeholders.of("count", count)
                                .and("targets", targets)
                                .and("ms", ms).build()));
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> msg(sender, "admin.sync.failed",
                        Placeholders.of("error",
                                e.getMessage() == null ? "未知错误" : e.getMessage()).build()));
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "同步失败", e);
            }
        });
    }

    private String buildTargets() {
        StringBuilder sb = new StringBuilder();
        for (DataStorage storage : plugin.getStorageManager().getSecondaries()) {
            if (sb.length() > 0) {
                sb.append("&7, &e");
            }
            sb.append(storage.getType().getDisplayName());
        }
        return sb.toString();
    }
}
