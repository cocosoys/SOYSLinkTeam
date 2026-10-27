package soys.soyslinkteam.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.command.SubCommand;
import soys.soyslinkteam.permission.TeamAction;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * /team disband — 解散队伍（可配置二次确认）。
 */
public class DisbandSub extends SubCommand {

    /** 玩家 UUID -> 确认到期时间戳 */
    private final Map<UUID, Long> confirmations = new ConcurrentHashMap<>();

    public DisbandSub(SOYSLinkTeam plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "disband";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("解散", "dis");
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        Player player = (Player) sender;

        withPlayerTeam(player, team -> {
            if (!checkAction(player, team, TeamAction.DISBAND)) {
                return;
            }

            // 二次确认
            if (plugin.getConfigManager().isDisbandConfirm()) {
                Long expireAt = confirmations.get(player.getUniqueId());
                long now = System.currentTimeMillis();
                if (expireAt == null || now > expireAt) {
                    int timeout = plugin.getConfigManager().getConfirmTimeout();
                    confirmations.put(player.getUniqueId(), now + timeout * 1000L);
                    msg(player, "soyslinkteam.team.disband.confirm", team.getName(), timeout);
                    msg(player, "soyslinkteam.general.confirm-required", timeout);
                    return;
                }
                confirmations.remove(player.getUniqueId());
            }

            String teamName = team.getName();

            if (!plugin.getTeamManager().disbandTeam(team, player)) {
                // 被 TeamDisbandEvent 监听器取消
                msg(player, "soyslinkteam.event.cancelled", "");
                return;
            }

            // 先广播再清理邀请，保证成员列表仍完整
            team.broadcast(plugin.getMessageManager().get("soyslinkteam.team.disband.broadcast",
                    teamName, player.getName()));

            plugin.getInviteManager().removeByTeam(team.getId());

            msg(player, "soyslinkteam.team.disband.success", teamName);
        });
    }

    /**
     * 玩家退出时清理未完成的确认。
     */
    public void clearConfirmation(UUID playerId) {
        confirmations.remove(playerId);
    }
}
