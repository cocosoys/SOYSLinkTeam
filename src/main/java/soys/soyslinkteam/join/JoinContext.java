package soys.soyslinkteam.join;

import org.bukkit.entity.Player;
import soys.soyslinkteam.team.Team;

/**
 * 一次入队尝试的上下文。
 * <p>封装入队所需的全部输入，便于后续扩展新的入队方式而不改动调用方签名。</p>
 */
public class JoinContext {

    private final Player player;
    private final Team team;

    /** 玩家在指令中附带的参数（如口令），可能为 null */
    private final String argument;

    /** 指令标签，用于消息中的用法提示 */
    private final String label;

    /** 是否由管理员强制执行，跳过所有条件校验 */
    private final boolean forced;

    public JoinContext(Player player, Team team, String argument, String label, boolean forced) {
        this.player = player;
        this.team = team;
        this.argument = argument;
        this.label = label;
        this.forced = forced;
    }

    public static JoinContext of(Player player, Team team, String argument, String label) {
        return new JoinContext(player, team, argument, label, false);
    }

    public Player getPlayer() {
        return player;
    }

    public Team getTeam() {
        return team;
    }

    public String getArgument() {
        return argument;
    }

    public boolean hasArgument() {
        return argument != null && !argument.isEmpty();
    }

    public String getLabel() {
        return label == null ? "team" : label;
    }

    public boolean isForced() {
        return forced;
    }
}
