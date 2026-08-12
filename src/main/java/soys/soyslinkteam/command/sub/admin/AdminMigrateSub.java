package soys.soyslinkteam.command.sub.admin;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.command.SubCommand;
import soys.soyslinkteam.storage.StorageType;
import soys.soyslinkteam.util.Placeholders;

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
            msg(sender, "admin.migrate.usage", Placeholders.of("label", label).build());
            return;
        }

        String fromArg = args[0].toLowerCase();
        String toArg = args[1].toLowerCase();
        boolean overwrite = args.length >= 3 && "overwrite".equalsIgnoreCase(args[2]);

        StorageType from = StorageType.fromId(fromArg);
        StorageType to = StorageType.fromId(toArg);

        if (from == null || to == null) {
            msg(sender, "admin.migrate.unknown-type",
                    Placeholders.of("type", from == null ? fromArg : toArg).build());
            return;
        }
        if (from == to) {
            msg(sender, "admin.migrate.same-type", null);
            return;
        }
        if (!plugin.getStorageManager().isEnabled(from)) {
            msg(sender, "admin.migrate.not-enabled",
                    Placeholders.of("type", from.getDisplayName()).build());
            return;
        }
        if (!plugin.getStorageManager().isEnabled(to)) {
            msg(sender, "admin.migrate.not-enabled",
                    Placeholders.of("type", to.getDisplayName()).build());
            return;
        }
        if (!overwrite) {
            msg(sender, "admin.migrate.warn-overwrite",
                    Placeholders.of("to", to.getDisplayName()).build());
            return;
        }

        msg(sender, "admin.migrate.started",
                Placeholders.of("from", from.getDisplayName()).and("to", to.getDisplayName()).build());

        final StorageType fFrom = from;
        final StorageType fTo = to;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                long start = System.currentTimeMillis();
                int count = plugin.getStorageManager().migrate(fFrom, fTo, true);
                long ms = System.currentTimeMillis() - start;
                Bukkit.getScheduler().runTask(plugin, () -> msg(sender, "admin.migrate.success",
                        Placeholders.of("from", fFrom.getDisplayName())
                                .and("to", fTo.getDisplayName())
                                .and("count", count)
                                .and("ms", ms).build()));
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> msg(sender, "admin.migrate.failed",
                        Placeholders.of("error",
                                e.getMessage() == null ? "未知错误" : e.getMessage()).build()));
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "迁移失败", e);
            }
        });
    }
}
