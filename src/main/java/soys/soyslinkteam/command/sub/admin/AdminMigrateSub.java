package soys.soyslinkteam.command.sub.admin;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.command.SubCommand;
import soys.soyslinkteam.storage.StorageType;

import java.util.Arrays;
import java.util.List;

/**
 * /teamadmin migrate &lt;来源&gt; &lt;目标&gt; [overwrite] — 在任意两个后端之间迁移数据。
 */
public class AdminMigrateSub extends SubCommand {

    public AdminMigrateSub(SOYSLinkTeam plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "migrate";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("迁移", "mg");
    }

    @Override
    public String getPermission() {
        return "soyslinkteam.admin.migrate";
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            msg(sender, "soyslinkteam.admin.migrate.usage", label);
            return;
        }

        String fromArg = args[0].toLowerCase();
        String toArg = args[1].toLowerCase();
        boolean overwrite = args.length >= 3 && "overwrite".equalsIgnoreCase(args[2]);

        StorageType from = StorageType.fromId(fromArg);
        StorageType to = StorageType.fromId(toArg);

        if (from == null || to == null) {
            msg(sender, "soyslinkteam.admin.migrate.unknown-type",
                    from == null ? fromArg : toArg);
            return;
        }
        if (from == to) {
            msg(sender, "soyslinkteam.admin.migrate.same-type");
            return;
        }
        if (!plugin.getStorageManager().isEnabled(from)) {
            msg(sender, "soyslinkteam.admin.migrate.not-enabled", from.getDisplayName());
            return;
        }
        if (!plugin.getStorageManager().isEnabled(to)) {
            msg(sender, "soyslinkteam.admin.migrate.not-enabled", to.getDisplayName());
            return;
        }
        if (!overwrite) {
            msg(sender, "soyslinkteam.admin.migrate.warn-overwrite", to.getDisplayName());
            return;
        }

        msg(sender, "soyslinkteam.admin.migrate.started",
                from.getDisplayName(), to.getDisplayName());

        final StorageType fFrom = from;
        final StorageType fTo = to;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                long start = System.currentTimeMillis();
                // 进度节流：每 500 支或收尾时反馈一次，避免大迁移时刷屏
                final int[] lastReported = {0};
                int count = plugin.getStorageManager().migrate(fFrom, fTo, true, (done, total) -> {
                    if (done != total && done - lastReported[0] < 500) {
                        return;
                    }
                    lastReported[0] = done;
                    int percent = total > 0 ? (int) Math.round(done * 100.0 / total) : 100;
                    Bukkit.getScheduler().runTask(plugin, () -> msg(sender, "soyslinkteam.admin.migrate.progress",
                            percent, done, total, renderBar(done, total)));
                });
                long ms = System.currentTimeMillis() - start;
                Bukkit.getScheduler().runTask(plugin, () -> msg(sender, "soyslinkteam.admin.migrate.success",
                        count, fFrom.getDisplayName(), fTo.getDisplayName(), ms));
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> msg(sender, "soyslinkteam.admin.migrate.failed",
                        e.getMessage() == null ? "未知错误" : e.getMessage()));
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "迁移失败", e);
            }
        });
    }

    /** 渲染 10 段进度条。 */
    private String renderBar(int done, int total) {
        int segments = 10;
        int filled = total <= 0 ? segments
                : (int) Math.round(done * (double) segments / total);
        filled = Math.max(0, Math.min(segments, filled));
        StringBuilder builder = new StringBuilder("&a");
        for (int i = 0; i < segments; i++) {
            builder.append(i < filled ? "■" : "□");
        }
        return builder.append("&r").toString();
    }
}
