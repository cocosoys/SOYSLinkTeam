package soys.soyslinkteam.join.impl;

import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.join.Invite;
import soys.soyslinkteam.join.JoinContext;
import soys.soyslinkteam.join.JoinResult;

/**
 * 队长邀请入队。
 * <p>玩家必须持有该队伍发出的、尚未过期的邀请才能加入。</p>
 */
public class InviteJoinMethod extends AbstractJoinMethod {

    public static final String ID = "invite";

    public InviteJoinMethod(SOYSLinkTeam plugin) {
        super(plugin);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "队长邀请";
    }

    /**
     * 邀请有效期（毫秒）。
     */
    public long getExpireMillis() {
        return Math.max(1, getInt("expire", 60)) * 1000L;
    }

    public int getExpireSeconds() {
        return Math.max(1, getInt("expire", 60));
    }

    public int getMaxPending() {
        return getInt("max-pending", 5);
    }

    public int getResendCooldown() {
        return getInt("resend-cooldown", 15);
    }

    public boolean isAllowOffline() {
        return getBoolean("allow-offline", false);
    }

    @Override
    public JoinResult attempt(JoinContext context) {
        Invite invite = plugin.getInviteManager()
                .get(context.getPlayer().getUniqueId(), context.getTeam().getId());
        if (invite == null) {
            // 没有邀请，交由其它方式尝试
            return JoinResult.notApplicable();
        }
        return JoinResult.success();
    }

    @Override
    public void onJoinSuccess(JoinContext context) {
        // 入队成功后清空该玩家的全部邀请
        plugin.getInviteManager().removeAll(context.getPlayer().getUniqueId());
    }
}
