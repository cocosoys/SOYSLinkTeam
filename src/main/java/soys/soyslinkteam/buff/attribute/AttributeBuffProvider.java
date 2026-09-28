package soys.soyslinkteam.buff.attribute;

import org.bukkit.entity.Player;

/**
 * 属性插件增幅提供者（SPI）。
 * <p>不同属性插件（BigAttribute、AttributePlus、Attribute、SX-Attribute、ItemAttribute）
 * 的 API 各不相同，本接口把"给予 / 移除属性"抽象为统一操作，由具体实现对接。</p>
 *
 * <p>内置 {@link CommandAttributeBuffProvider} 通过控制台命令映射适配任意属性插件，
 * 无需在编译期依赖这些插件；后续可为特定属性插件编写原生实现。</p>
 */
public interface AttributeBuffProvider {

    /**
     * 提供者 ID（如 sx-attribute / command）。
     */
    String getId();

    /**
     * 提供者显示名。
     */
    String getDisplayName();

    /**
     * 目标属性插件是否已安装 / 本提供者是否可用。
     */
    boolean isAvailable();

    /**
     * 向玩家应用一个属性增幅。
     *
     * @param player      目标玩家
     * @param attributeKey 属性键（由配置定义）
     * @param amplifier   增幅等级（0 基）
     * @param buffId      增幅实例 ID（可用于追踪 / 撤销）
     */
    void apply(Player player, String attributeKey, int amplifier, String buffId);

    /**
     * 移除玩家的一个属性增幅。
     *
     * @param player      目标玩家
     * @param attributeKey 属性键
     * @param buffId      增幅实例 ID
     */
    void remove(Player player, String attributeKey, String buffId);
}
