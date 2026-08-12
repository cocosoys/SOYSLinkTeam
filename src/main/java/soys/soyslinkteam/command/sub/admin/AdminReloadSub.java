package soys.soyslinkteam.command.sub.admin;

import org.bukkit.command.CommandSender;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.command.SubCommand;
import soys.soyslinkteam.util.Placeholders;

import java.util.Arrays;
import java.util.List;

/**
 * /teamadmin reload — 重载配置文件与全部模块。
 */
public class AdminReloadSub extends SubCommand {

    public AdminReloadSub(SOYSLinkTeam plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("重载", "rl");
    }

    @Override
    public String getPermission() {
        return "soyslinkteam.admin.reload";
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        long start = System.currentTimeMillis();
        try {
            plugin.reloadConfiguration();
            long ms = System.currentTimeMillis() - start;
            msg(sender, "general.reload-success", Placeholders.of("ms", ms).build());
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "重载配置失败", e);
            msg(sender, "general.reload-failed", null);
        }
    }
}
