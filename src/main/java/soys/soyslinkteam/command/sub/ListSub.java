package soys.soyslinkteam.command.sub;

import org.bukkit.command.CommandSender;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.command.SubCommand;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.util.Placeholders;
import soys.soyslinkteam.util.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * /team list [页码] — 分页查看服务器内所有队伍。
 * <p>已加载内存的队伍展示完整信息；未加载的队伍只显示名称（避免为列表触发大量磁盘读取）。</p>
 */
public class ListSub extends SubCommand {

    public ListSub(SOYSLinkTeam plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public List<String> getAliases() {
        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return "soyslinkteam.use";
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        Map<UUID, String> index = plugin.getTeamManager().getTeamNameIndex();
        if (index.isEmpty()) {
            msg(sender, "list.empty", null);
            return;
        }

        int pageSize = plugin.getConfigManager().getListPageSize();
        int totalPages = Math.max(1, (int) Math.ceil((double) index.size() / pageSize));
        int page = args.length >= 1 ? soys.soyslinkteam.util.Text.parseInt(args[0], 1) : 1;
        if (page < 1) {
            page = 1;
        }
        if (page > totalPages) {
            msg(sender, "list.invalid-page", Placeholders.of("max_page", totalPages).build());
            return;
        }

        String trueText = plugin.getConfigManager().getPlaceholderTrue();
        String falseText = plugin.getConfigManager().getPlaceholderFalse();

        List<String> lines = new ArrayList<>();
        lines.add(plugin.getMessageManager().get("list.header",
                Placeholders.of("page", page).and("max_page", totalPages).build()));

        int start = (page - 1) * pageSize;
        int i = 0;
        for (Map.Entry<UUID, String> entry : index.entrySet()) {
            if (i < start) {
                i++;
                continue;
            }
            if (i >= start + pageSize) {
                break;
            }
            i++;
            UUID id = entry.getKey();
            String name = entry.getValue();
            Team loaded = plugin.getTeamManager().getLoadedTeam(id);
            String leader = loaded != null ? loaded.getLeaderName() : "?";
            String size = loaded != null ? String.valueOf(loaded.getSize()) : "?";
            String max = loaded != null
                    ? String.valueOf(plugin.getTeamManager().getMaxSize(loaded)) : "?";
            String online = loaded != null ? String.valueOf(loaded.getOnlineCount()) : "?";
            String pub = loaded != null
                    ? (loaded.getSettings().isOpen() ? trueText : falseText) : "";
            lines.add(plugin.getMessageManager().get("list.entry", Placeholders
                    .of("name", name).and("leader", leader)
                    .and("size", size).and("max", max).and("online", online)
                    .and("public", pub).build()));
        }

        for (String line : lines) {
            Text.send(sender, line);
        }

        if (totalPages > 1) {
            msg(sender, "list.footer", Placeholders.of("label", label).build());
        }
    }
}
