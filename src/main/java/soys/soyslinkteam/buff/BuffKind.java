package soys.soyslinkteam.buff;

/**
 * 队伍增幅类型。
 */
public enum BuffKind {

    /** 原版药水效果（信标效果），经 Bukkit PotionEffect 应用 */
    POTION("potion", "药水效果"),

    /** 第三方属性插件加成（BigAttribute / AttributePlus / SX-Attribute 等） */
    ATTRIBUTE("attribute", "属性加成");

    private final String id;
    private final String displayName;

    BuffKind(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 按配置文本解析类型，无法识别时返回 null。
     */
    public static BuffKind fromId(String text) {
        if (text == null) {
            return null;
        }
        String lower = text.trim().toLowerCase();
        for (BuffKind kind : values()) {
            if (kind.id.equals(lower) || kind.name().equalsIgnoreCase(lower)) {
                return kind;
            }
        }
        return null;
    }
}
