package soys.soyslinkteam.buff.attribute;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import soys.soyslinkteam.SOYSLinkTeam;

/**
 * 基于控制台命令映射的属性增幅提供者。
 * <p>不直接依赖任何属性插件，而是由服主在 buff.yml 中为每个属性键配置
 * "给予命令"与"移除命令"，从而适配 BigAttribute、AttributePlus、Attribute、
 * SX-Attribute、ItemAttribute 等一切支持控制台指令的属性插件。</p>
 *
 * <p>命令模板占位符：{@code %player%}（玩家名）、{@code %level%}（等级）、
 * {@code %buffid%}（增幅实例 ID）。</p>
 *
 * <pre>
 * attributes:
 *   crit:
 *     display-name: "暴击率"
 *     apply-command: "sx attribute %player% add 暴击率 %level%"
 *     remove-command: "sx attribute %player% remove 暴击率"
 * </pre>
 */
public class CommandAttributeBuffProvider implements AttributeBuffProvider {

    public static final String ID = "command";

    private final SOYSLinkTeam plugin;

    public CommandAttributeBuffProvider(SOYSLinkTeam plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "命令映射属性";
    }

    /**
     * 命令映射方式始终"可用"（只要配置了对应属性命令即可执行）。
     */
    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void apply(Player player, String attributeKey, int amplifier, String buffId) {
        ConfigurationSection attr = getAttributeSection(attributeKey);
        if (attr == null) {
            plugin.getLogger().warning("[增幅] 属性 " + attributeKey + " 未在 buff.yml 中配置 apply-command");
            return;
        }
        String command = attr.getString("apply-command");
        if (command == null || command.isEmpty()) {
            return;
        }
        dispatch(command, player, amplifier, buffId);
    }

    @Override
    public void remove(Player player, String attributeKey, String buffId) {
        ConfigurationSection attr = getAttributeSection(attributeKey);
        if (attr == null) {
            return;
        }
        String command = attr.getString("remove-command");
        if (command == null || command.isEmpty()) {
            return;
        }
        dispatch(command, player, amplifierOf(attr), buffId);
    }

    private void dispatch(String template, Player player, int amplifier, String buffId) {
        String command = template
                .replace("%player%", player.getName())
                .replace("%level%", String.valueOf(amplifier + 1))
                .replace("%amplifier%", String.valueOf(amplifier))
                .replace("%buffid%", buffId == null ? "" : buffId);
        // 以控制台身份同步执行属性插件命令
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    private int amplifierOf(ConfigurationSection attr) {
        return attr.getInt("last-amplifier", 0);
    }

    private ConfigurationSection getAttributeSection(String attributeKey) {
        ConfigurationSection root = plugin.getConfigManager().getBuffSection();
        if (root == null) {
            return null;
        }
        return root.getConfigurationSection("attributes." + attributeKey);
    }
}
