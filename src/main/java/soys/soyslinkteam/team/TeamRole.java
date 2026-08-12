package soys.soyslinkteam.team;

/**
 * 队内角色。
 * <p>weight 越大权限越高，用于同级/越级操作判定。</p>
 */
public enum TeamRole {

    /** 队长，队伍唯一 */
    LEADER(100),

    /** 副队长 */
    ADMIN(50),

    /** 普通队员 */
    MEMBER(10);

    private final int weight;

    TeamRole(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }

    /**
     * 判断本角色是否严格高于目标角色。
     */
    public boolean isHigherThan(TeamRole other) {
        return other != null && this.weight > other.weight;
    }

    /**
     * 安全解析，失败时返回 MEMBER。
     */
    public static TeamRole parse(String input) {
        if (input == null) {
            return MEMBER;
        }
        try {
            return valueOf(input.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MEMBER;
        }
    }
}
