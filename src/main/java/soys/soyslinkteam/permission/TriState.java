package soys.soyslinkteam.permission;

/**
 * 三态判定结果，用于权限责任链。
 */
public enum TriState {

    /** 明确放行，立即中断判定链 */
    ALLOW,

    /** 明确拒绝，立即中断判定链 */
    DENY,

    /** 不表态，交给下一个 Provider */
    PASS;

    public boolean isDecisive() {
        return this != PASS;
    }

    public static TriState of(boolean value) {
        return value ? ALLOW : DENY;
    }

    /**
     * 安全解析，未匹配时返回 PASS。
     */
    public static TriState parse(String input) {
        if (input == null) {
            return PASS;
        }
        try {
            return valueOf(input.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PASS;
        }
    }
}
