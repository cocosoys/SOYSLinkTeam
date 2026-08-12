package soys.soyslinkteam.command.sub.admin;

import org.bukkit.command.CommandSender;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.command.SubCommand;
import soys.soyslinkteam.storage.DataStorage;
import soys.soyslinkteam.util.Placeholders;
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
        String trueText = plugin.getConfigManager().getPlaceholderTrue();
        String falseText = plugin.getConfigManager().getPlaceholderFalse();

        DataStorage primary = plugin.getStorageManager().getPrimary();
        List<DataStorage> secondaries = plugin.getStorageManager().getSecondaries();

        List<String> lines = new ArrayList<>();
        lines.add(plugin.getMessageManager().get("admin.storage.header"));

        if (primary != null) {
            lines.add(plugin.getMessageManager().get("admin.storage.primary", Placeholders
                    .of("type", primary.getType().getDisplayName())
                    .and("detail", primary.describe()).build()));
        }

        if (secondaries.isEmpty()) {
            lines.add(plugin.getMessageManager().get("admin.storage.none-secondary"));
        } else {
            StringBuilder sb = new StringBuilder();
            for (DataStorage storage : secondaries) {
                if (sb.length() > 0) {
                    sb.append("&7, &e");
                }
                sb.append(storage.getType().getDisplayName())
                        .append("&8(&f").append(storage.describe()).append("&8)");
            }
            lines.add(plugin.getMessageManager().get("admin.storage.secondary",
                    Placeholders.of("type", sb.toString()).build()));
        }

        lines.add(plugin.getMessageManager().get("admin.storage.loaded-teams", Placeholders
                .of("loaded", plugin.getTeamManager().getLoadedCount())
                .and("total", plugin.getTeamManager().getTotalCount()).build()));

        lines.add(plugin.getMessageManager().get("admin.storage.mirror", Placeholders
                .of("enabled", plugin.getConfigManager().isMirrorEnabled() ? trueText : falseText)
                .and("async", plugin.getConfigManager().isMirrorAsync() ? trueText : falseText)
                .build()));

        for (String line : lines) {
            Text.send(sender, line);
        }
    }
}
