package soys.soyslinkteam.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.util.Placeholders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 子指令分发器基类。
 * <p>统一处理子指令查找、权限校验、玩家限定校验与 Tab 补全。</p>
 */
public abstract class CommandDispatcher implements CommandExecutor, TabCompleter {

    protected final SOYSLinkTeam plugin;

    /** 名称与别名 -> 子指令 */
    private final Map<String, SubCommand> lookup = new LinkedHashMap<>();

    /** 注册顺序的子指令列表 */
    private final List<SubCommand> ordered = new ArrayList<>();

    protected CommandDispatcher(SOYSLinkTeam plugin) {
        this.plugin = plugin;
    }

    /**
     * 注册子指令。
     */
    protected void register(SubCommand sub) {
        ordered.add(sub);
        lookup.put(sub.getName().toLowerCase(), sub);
        for (String alias : sub.getAliases()) {
            lookup.put(alias.toLowerCase(), sub);
        }
    }

    public SubCommand getSubCommand(String name) {
        return name == null ? null : lookup.get(name.toLowerCase());
    }

    public List<SubCommand> getSubCommands() {
        return Collections.unmodifiableList(ordered);
    }

    /**
     * 无参数时的默认行为（通常是显示帮助）。
     */
    protected abstract void showDefault(CommandSender sender, String label);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showDefault(sender, label);
            return true;
        }

        SubCommand sub = getSubCommand(args[0]);
        if (sub == null) {
            msg(sender, "general.unknown-command", Placeholders
                    .of("input", args[0]).and("label", label).build());
            return true;
        }

        if (sub.isPlayerOnly() && !(sender instanceof Player)) {
            msg(sender, "general.player-only", null);
            return true;
        }

        String permission = sub.getPermission();
        if (permission != null && !sender.hasPermission(permission)) {
            msg(sender, "general.no-permission", null);
            return true;
        }

        String[] subArgs = args.length > 1
                ? Arrays.copyOfRange(args, 1, args.length)
                : new String[0];
        try {
            sub.execute(sender, label, subArgs);
        } catch (Throwable t) {
            plugin.getLogger().severe("执行子指令 " + sub.getName() + " 时出错: " + t.getMessage());
            t.printStackTrace();
            msg(sender, "general.storage-error", null);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (!plugin.getConfigManager().isTabComplete()) {
            return Collections.emptyList();
        }

        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase();
            List<String> names = new ArrayList<>();
            for (SubCommand sub : ordered) {
                if (sub.isPlayerOnly() && !(sender instanceof Player)) {
                    continue;
                }
                String permission = sub.getPermission();
                if (permission != null && !sender.hasPermission(permission)) {
                    continue;
                }
                if (sub.getName().toLowerCase().startsWith(prefix)) {
                    names.add(sub.getName());
                }
            }
            return names;
        }

        SubCommand sub = getSubCommand(args[0]);
        if (sub == null) {
            return Collections.emptyList();
        }
        String permission = sub.getPermission();
        if (permission != null && !sender.hasPermission(permission)) {
            return Collections.emptyList();
        }
        List<String> result = sub.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
        return result == null ? Collections.emptyList() : result;
    }

    private void msg(CommandSender sender, String key, Map<String, String> placeholders) {
        plugin.getMessageManager().send(sender, key, placeholders);
    }
}
