package soys.soyslinkteam.command.sub.admin;

import org.bukkit.command.CommandSender;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.command.SubCommand;
import soys.soyslinkteam.storage.DataStorage;
import soys.soyslinkteam.storage.StorageManager;
import soys.soyslinkteam.util.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * /teamadmin storage — 查看存储后端状态。
 */
public class AdminStorageSub extends SubCommand {

    public AdminStorageSub(SOYSLinkTeam plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "storage";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("存储", "stat");
    }

    @Override
    public String getPermission() {
        return "soyslinkteam.admin.inspect";
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        if (args.length >= 1 && "check".equalsIgnoreCase(args[0])) {
            runCheck(sender, label);
            return;
        }
        String trueText = plugin.getConfigManager().getPlaceholderTrue();
        String falseText = plugin.getConfigManager().getPlaceholderFalse();

        DataStorage primary = plugin.getStorageManager().getPrimary();
        List<DataStorage> secondaries = plugin.getStorageManager().getSecondaries();

        List<String> lines = new ArrayList<>();
        lines.add(plugin.getMessageManager().get("soyslinkteam.admin.storage.header"));

        if (primary != null) {
            lines.add(plugin.getMessageManager().get("soyslinkteam.admin.storage.primary",
                    primary.getType().getDisplayName(), primary.describe()));
        }

        if (secondaries.isEmpty()) {
            lines.add(plugin.getMessageManager().get("soyslinkteam.admin.storage.none-secondary"));
        } else {
            StringBuilder sb = new StringBuilder();
            for (DataStorage storage : secondaries) {
                if (sb.length() > 0) {
                    sb.append("&7, &e");
                }
                sb.append(storage.getType().getDisplayName())
                        .append("&8(&f").append(storage.describe()).append("&8)");
            }
            lines.add(plugin.getMessageManager().get("soyslinkteam.admin.storage.secondary",
                    sb.toString()));
        }

        lines.add(plugin.getMessageManager().get("soyslinkteam.admin.storage.loaded-teams",
                plugin.getTeamManager().getLoadedCount(),
                plugin.getTeamManager().getTotalCount()));

        lines.add(plugin.getMessageManager().get("soyslinkteam.admin.storage.mirror",
                plugin.getConfigManager().isMirrorEnabled() ? trueText : falseText,
                plugin.getConfigManager().isMirrorAsync() ? trueText : falseText));

        for (String line : lines) {
            Text.send(sender, line);
        }
    }

    /**
     * /teamadmin storage check — 校验主辅存储一致性。
     * <p>读取操作可能涉及 SQL，放异步线程执行，结果回主线程展示。</p>
     */
    private void runCheck(CommandSender sender, String label) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                StorageManager.ConsistencyReport report =
                        plugin.getStorageManager().checkConsistency();
                plugin.getServer().getScheduler().runTask(plugin, () -> displayReport(sender, label, report));
            } catch (Exception e) {
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        msg(sender, "soyslinkteam.admin.storage.check.failed",
                                e.getMessage() == null ? "未知错误" : e.getMessage()));
                plugin.getLogger().log(java.util.logging.Level.WARNING, "存储一致性检查失败", e);
            }
        });
    }

    private void displayReport(CommandSender sender, String label, StorageManager.ConsistencyReport report) {
        Text.send(sender, plugin.getMessageManager().get("soyslinkteam.admin.storage.check.header"));

        if (report.getStatus() == StorageManager.ConsistencyReport.Status.NO_SECONDARY) {
            msg(sender, "soyslinkteam.admin.storage.check.no-secondary", report.getPrimaryCount());
            return;
        }

        for (StorageManager.SecondaryDiff diff : report.getDiffs()) {
            String type = diff.getType().getDisplayName();
            if (diff.isConsistent()) {
                msg(sender, "soyslinkteam.admin.storage.check.ok", type, diff.getCount());
            } else {
                msg(sender, "soyslinkteam.admin.storage.check.mismatch",
                        type, diff.getCount(), report.getPrimaryCount());
                if (!diff.getMissing().isEmpty()) {
                    msg(sender, "soyslinkteam.admin.storage.check.missing",
                            diff.getMissing().size(),
                            String.join("&7, &f", diff.getMissing()));
                }
                if (!diff.getExtra().isEmpty()) {
                    msg(sender, "soyslinkteam.admin.storage.check.extra",
                            diff.getExtra().size(),
                            String.join("&7, &f", diff.getExtra()));
                }
            }
        }

        if (report.getStatus() == StorageManager.ConsistencyReport.Status.CONSISTENT) {
            msg(sender, "soyslinkteam.admin.storage.check.summary-ok");
        } else {
            msg(sender, "soyslinkteam.admin.storage.check.summary-bad", label);
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("check"), args[0]);
        }
        return null;
    }
}
