package soys.soyslinkteam.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.command.SubCommand;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.util.CooldownManager;
import soys.soyslinkteam.util.NameValidator;
import soys.soyslinkteam.util.Placeholders;
import soys.soyslinkteam.util.Text;

import java.util.Arrays;
import java.util.List;

/**
 * /team create &lt;名称&gt; — 创建队伍。
 */
public class CreateSub extends SubCommand {

    public CreateSub(SOYSLinkTeam plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("新建", "c");
    }

    @Override
    public String getPermission() {
        return "soyslinkteam.create";
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        Player player = (Player) sender;

        if (args.length < 1) {
            msg(player, "team.create.usage", Placeholders.of("label", label).build());
            return;
        }

        // 已有队伍
        if (plugin.getTeamManager().hasTeam(player.getUniqueId())) {
            String current = currentTeamName(player);
            msg(player, "team.already-in-team", Placeholders.of("team", current).build());
            return;
        }

        // 创建冷却
        if (!plugin.getPermissionManager().canBypass(player)) {
            int remaining = plugin.getCooldownManager()
                    .getRemaining(CooldownManager.CREATE, player.getUniqueId());
            if (remaining > 0) {
                msg(player, "general.cooldown", Placeholders.of("seconds", remaining).build());
                return;
            }
        }

        // 名称校验
        String name = Text.join(args, 0).trim();
        NameValidator.Result result = plugin.getNameValidator().validateTeamName(name, null);
        if (!result.isValid()) {
            msg(player, result.getMessageKey(), result.getPlaceholders());
            return;
        }

        Team team = plugin.getTeamManager().createTeam(name, player);
        if (team == null) {
            // 被 TeamCreateEvent 监听器取消
            msg(player, "event.cancelled", Placeholders.of("reason", "").build());
            return;
        }
        plugin.getCooldownManager().set(CooldownManager.CREATE, player.getUniqueId(),
                plugin.getConfigManager().getCreateCooldown());

        msg(player, "team.create.success", Placeholders
                .of("team", team.getName()).and("label", label).build());
    }

    private String currentTeamName(Player player) {
        java.util.UUID teamId = plugin.getTeamManager().getPlayerTeamId(player.getUniqueId());
        String name = teamId == null ? null : plugin.getTeamManager().getTeamNameIndex().get(teamId);
        return name == null ? "未知" : name;
    }
}
