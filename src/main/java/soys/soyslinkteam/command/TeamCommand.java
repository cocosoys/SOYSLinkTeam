package soys.soyslinkteam.command;

import org.bukkit.command.CommandSender;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.command.sub.AcceptSub;
import soys.soyslinkteam.command.sub.CreateSub;
import soys.soyslinkteam.command.sub.DenySub;
import soys.soyslinkteam.command.sub.DemoteSub;
import soys.soyslinkteam.command.sub.DisbandSub;
import soys.soyslinkteam.command.sub.HelpSub;
import soys.soyslinkteam.command.sub.InfoSub;
import soys.soyslinkteam.command.sub.InviteSub;
import soys.soyslinkteam.command.sub.JoinSub;
import soys.soyslinkteam.command.sub.KickSub;
import soys.soyslinkteam.command.sub.LeaveSub;
import soys.soyslinkteam.command.sub.ListSub;
import soys.soyslinkteam.command.sub.PromoteSub;
import soys.soyslinkteam.command.sub.SetSub;
import soys.soyslinkteam.command.sub.TransferSub;
import soys.soyslinkteam.util.Placeholders;

/**
 * /team 指令分发器：注册并委托所有玩家侧子指令。
 */
public class TeamCommand extends CommandDispatcher {

    public TeamCommand(SOYSLinkTeam plugin) {
        super(plugin);
        register(new CreateSub(plugin));
        register(new DisbandSub(plugin));
        register(new InviteSub(plugin));
        register(new AcceptSub(plugin));
        register(new DenySub(plugin));
        register(new JoinSub(plugin));
        register(new LeaveSub(plugin));
        register(new KickSub(plugin));
        register(new TransferSub(plugin));
        register(new PromoteSub(plugin));
        register(new DemoteSub(plugin));
        register(new InfoSub(plugin));
        register(new ListSub(plugin));
        register(new SetSub(plugin));
        register(new HelpSub(plugin));
    }

    @Override
    protected void showDefault(CommandSender sender, String label) {
        plugin.getMessageManager().sendList(sender, "help.player",
                Placeholders.of("label", label).build());
    }
}
