package soys.soyslinkteam.join.impl;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.join.JoinContext;
import soys.soyslinkteam.join.JoinResult;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.util.Placeholders;

/**
 * 公开队伍直接加入。
 * <p>队伍被队长设为公开且未设置口令时，任意玩家可直接加入。</p>
 */
public class PublicJoinMethod extends AbstractJoinMethod {

    public static final String ID = "public";

    public PublicJoinMethod(SOYSLinkTeam plugin) {
        super(plugin);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "公开加入";
    }

    public boolean isDefaultPublic() {
        return getBoolean("default-public", false);
    }

    public boolean isRequireLeaderOnline() {
        return getBoolean("require-leader-online", false);
    }

    @Override
    public JoinResult attempt(JoinContext context) {
        Team team = context.getTeam();

        if (!team.getSettings().isOpen()) {
            // 非公开队伍，交由其它方式尝试
            return JoinResult.notApplicable();
        }

        // 设有口令的公开队伍仍需通过口令方式校验
        if (team.getSettings().hasPassword()) {
            return JoinResult.notApplicable();
        }

        if (isRequireLeaderOnline()) {
            Player leader = team.getLeader() == null ? null : Bukkit.getPlayer(team.getLeader());
            if (leader == null || !leader.isOnline()) {
                return JoinResult.denied("join.public.leader-offline",
                        Placeholders.of("team", team.getName()).build());
            }
        }

        return JoinResult.success();
    }
}
