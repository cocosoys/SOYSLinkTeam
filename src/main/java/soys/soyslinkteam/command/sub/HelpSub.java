package soys.soyslinkteam.command.sub;

import org.bukkit.command.CommandSender;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.command.SubCommand;
import soys.soyslinkteam.util.Placeholders;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * /team help — 显示玩家指令帮助。
 */
public class HelpSub extends SubCommand {

    public HelpSub(SOYSLinkTeam plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("帮助", "?");
    }

    @Override
    public String getPermission() {
        return "soyslinkteam.use";
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        msgList(sender, "help.player", Placeholders.of("label", label).build());
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
