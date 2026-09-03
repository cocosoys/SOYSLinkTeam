package soys.soyslinkteam.permission;

/**
 * 队伍内可被权限体系管辖的动作。
 * <p>
 * 新增队伍功能时在此登记动作，即可自动纳入 config.yml 的
 * {@code permission.roles} 角色映射与所有 PermissionProvider 的判定范围。
 * </p>
 */
public enum TeamAction {

    /** 邀请玩家入队 */
    INVITE("邀请玩家"),

    /** 踢出成员 */
    KICK("踢出成员"),

    /** 解散队伍 */
    DISBAND("解散队伍"),

    /** 转让队长 */
    TRANSFER("转让队长"),

    /** 提升为副队长 */
    PROMOTE("提升副队长"),

    /** 降级副队长 */
    DEMOTE("降级副队长"),

    /** 修改队伍名称 */
    RENAME("修改队名"),

    /** 修改队伍简称 */
    SET_TAG("修改简称"),

    /** 修改队伍公告 */
    SET_NOTICE("修改公告"),

    /** 切换公开状态 */
    SET_PUBLIC("切换公开"),

    /** 设置或清除入队口令 */
    SET_PASSWORD("设置口令"),

    /** 设置入队费用（经济消耗入队） */
    SET_COST("设置入队费用"),

    /** 查看队伍详情 */
    VIEW_INFO("查看详情"),

    /** 退出队伍 */
    LEAVE("退出队伍");

    private final String displayName;

    TeamAction(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 安全解析，未匹配时返回 null。
     */
    public static TeamAction parse(String input) {
        if (input == null) {
            return null;
        }
        try {
            return valueOf(input.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
