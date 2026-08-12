package soys.soyslinkteam.command;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.event.TeamEvent;
import soys.soyslinkteam.permission.TeamAction;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.util.Placeholders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 子指令抽象基类。
 * <p>新增指令只需继承本类并在 {@link TeamCommand} / {@link AdminCommand} 中注册。</p>
 */
public abstract class SubCommand {

    protected final SOYSLinkTeam plugin;

    protected SubCommand(SOYSLinkTeam plugin) {
        this.plugin = plugin;
    }

    /**
     * 子指令主名称。
     */
    public abstract String getName();

    /**
     * 子指令别名。
     */
    public List<String> getAliases() {
        return Collections.emptyList();
    }

    /**
     * 所需 Bukkit 权限节点，null 表示不校验。
     */
    public String getPermission() {
        return null;
    }

    /**
     * 是否只允许玩家执行。
     */
    public boolean isPlayerOnly() {
        return true;
    }

    /**
     * 执行指令。
     *
     * @param sender 执行者
     * @param label  指令标签（用于用法提示）
     * @param args   已剔除子指令名的参数数组
     */
    public abstract void execute(CommandSender sender, String label, String[] args);

    /**
     * Tab 补全。
     *
     * @param args 已剔除子指令名的参数数组
     */
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    // ================================================================
    //  便捷方法
    // ================================================================

    protected void msg(CommandSender sender, String key) {
        plugin.getMessageManager().send(sender, key);
    }

    protected void msg(CommandSender sender, String key, Map<String, String> placeholders) {
        plugin.getMessageManager().send(sender, key, placeholders);
    }

    protected void msgList(CommandSender sender, String key, Map<String, String> placeholders) {
        plugin.getMessageManager().sendList(sender, key, placeholders);
    }

    /**
     * 获取玩家所在队伍（必要时异步装载），玩家无队伍时自动提示并中断。
     */
    protected void withPlayerTeam(Player player, Consumer<Team> action) {
        if (!plugin.getTeamManager().hasTeam(player.getUniqueId())) {
            msg(player, "team.not-in-team");
            return;
        }
        plugin.getTeamManager().getPlayerTeamAsync(player.getUniqueId(), team -> {
            if (team == null) {
                msg(player, "team.not-in-team");
                return;
            }
            action.accept(team);
        });
    }

    /**
     * 校验玩家在队伍中是否有权执行动作，无权时自动提示。
     *
     * @return 是否有权
     */
    protected boolean checkAction(Player player, Team team, TeamAction action) {
        if (plugin.getPermissionManager().can(player, team, action)) {
            return true;
        }
        msg(player, "general.no-permission");
        return false;
    }

    /**
     * 触发一个（可取消的）队伍事件，并在被取消时向 sender 反馈取消原因。
     *
     * @return true 表示事件未被取消、操作可继续；false 表示已被取消
     */
    protected boolean callEvent(TeamEvent event, CommandSender sender) {
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            String reason = event.getCancelReason();
            msg(sender, "event.cancelled",
                    Placeholders.of("reason", reason == null ? "" : " &7(" + reason + ")").build());
            return false;
        }
        return true;
    }

    /**
     * 按前缀过滤候选项，用于 Tab 补全。
     */
    protected List<String> filter(List<String> candidates, String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return candidates;
        }
        String lower = prefix.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String candidate : candidates) {
            if (candidate.toLowerCase().startsWith(lower)) {
                result.add(candidate);
            }
        }
        return result;
    }

    /**
     * 在线玩家名候选项，受 {@code command.tab-complete-players} 控制。
     */
    protected List<String> onlinePlayerNames(String prefix) {
        if (!plugin.getConfigManager().isTabCompletePlayers()) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<>();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            names.add(player.getName());
        }
        return filter(names, prefix);
    }

    /**
     * 全部队伍名候选项。
     */
    protected List<String> teamNames(String prefix) {
        List<String> names = new ArrayList<>(plugin.getTeamManager().getTeamNameIndex().values());
        return filter(names, prefix);
    }
}
