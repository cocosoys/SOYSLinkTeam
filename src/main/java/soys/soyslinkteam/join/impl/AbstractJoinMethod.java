package soys.soyslinkteam.join.impl;

import org.bukkit.configuration.ConfigurationSection;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.join.JoinMethod;

/**
 * 入队方式的公共基类：统一从 {@code join.methods.<id>} 读取 enabled 与 order。
 */
public abstract class AbstractJoinMethod implements JoinMethod {

    protected final SOYSLinkTeam plugin;

    protected AbstractJoinMethod(SOYSLinkTeam plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isEnabled() {
        return plugin.getConfigManager().isJoinMethodEnabled(getId());
    }

    @Override
    public int getOrder() {
        return plugin.getConfigManager().getJoinMethodOrder(getId());
    }

    /**
     * 本方式在 config.yml 中的配置节，可能为 null。
     */
    protected ConfigurationSection section() {
        return plugin.getConfigManager().getJoinMethodSection(getId());
    }

    protected boolean getBoolean(String path, boolean fallback) {
        ConfigurationSection section = section();
        return section == null ? fallback : section.getBoolean(path, fallback);
    }

    protected int getInt(String path, int fallback) {
        ConfigurationSection section = section();
        return section == null ? fallback : section.getInt(path, fallback);
    }

    protected String getString(String path, String fallback) {
        ConfigurationSection section = section();
        return section == null ? fallback : section.getString(path, fallback);
    }
}
