package soys.soyslinkteam.command;

import org.bukkit.command.CommandSender;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.command.sub.admin.AdminDisbandSub;
import soys.soyslinkteam.command.sub.admin.AdminInfoSub;
import soys.soyslinkteam.command.sub.admin.AdminKickSub;
import soys.soyslinkteam.command.sub.admin.AdminMigrateSub;
import soys.soyslinkteam.command.sub.admin.AdminReloadSub;
import soys.soyslinkteam.command.sub.admin.AdminSaveSub;
import soys.soyslinkteam.command.sub.admin.AdminStorageSub;
import soys.soyslinkteam.command.sub.admin.AdminSyncSub;
import soys.soyslinkteam.command.sub.admin.AdminTransferSub;

/**
 * /teamadmin 指令分发器：注册并委托所有管理侧子指令。
 */
public class AdminCommand extends CommandDispatcher {

    public AdminCommand(SOYSLinkTeam plugin) {
        super(plugin);
        register(new AdminReloadSub(plugin));
        register(new AdminInfoSub(plugin));
        register(new AdminDisbandSub(plugin));
        register(new AdminKickSub(plugin));
        register(new AdminTransferSub(plugin));
        register(new AdminStorageSub(plugin));
        register(new AdminMigrateSub(plugin));
        register(new AdminSyncSub(plugin));
        register(new AdminSaveSub(plugin));
    }

    @Override
    protected void showDefault(CommandSender sender, String label) {
        plugin.getMessageManager().sendList(sender, "admin.usage", null);
    }
}
