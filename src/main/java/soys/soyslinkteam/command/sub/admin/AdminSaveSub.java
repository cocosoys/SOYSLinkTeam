package soys.soyslinkteam.command.sub.admin;

import org.bukkit.command.CommandSender;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.command.SubCommand;
import soys.soyslinkteam.util.Placeholders;

import java.util.Arrays;
import java.util.List;

/**
 * /teamadmin save — 立即保存所有内存中的队伍。
 */
public class AdminSaveSub extends SubCommand {

    public AdminSaveSub(SOYSLinkTeam plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "save";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("保存");
    }

    @Override
    public String getPermission() {
        return "soyslinkteam.admin.manage";
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        int count = plugin.getTeamManager().saveAll();
        msg(sender, "admin.save.success", Placeholders.of("count", count).build());
    }
}
