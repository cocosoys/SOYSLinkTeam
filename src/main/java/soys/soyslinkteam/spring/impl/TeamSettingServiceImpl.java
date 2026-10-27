package soys.soyslinkteam.spring.impl;

import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.spring.entity.vo.SettingsVO;
import soys.soyslinkteam.spring.service.TeamSettingService;

/**
 * 系统设置服务实现（只读展示）。
 */
public class TeamSettingServiceImpl implements TeamSettingService {

    private final SOYSLinkTeam plugin;

    public TeamSettingServiceImpl(SOYSLinkTeam plugin) {
        this.plugin = plugin;
    }

    @Override
    public SettingsVO settings() {
        SettingsVO v = new SettingsVO();
        v.setLanguage(plugin.getConfigManager().getLanguage());
        v.setDebug(plugin.getConfigManager().isDebug());
        v.setDefaultMaxSize(plugin.getConfigManager().getDefaultMaxSize());
        v.setHardMaxSize(plugin.getConfigManager().getHardMaxSize());
        v.setMirrorEnabled(plugin.getConfigManager().isMirrorEnabled());
        v.setMirrorAsync(plugin.getConfigManager().isMirrorAsync());
        v.setTeamChatEnabled(plugin.getConfigManager().isTeamChatEnabled());
        v.setNametagEnabled(plugin.getConfigManager().isNametagEnabled());
        v.setBuffDefaultDuration(plugin.getConfigManager().getBuffDefaultDuration());
        v.setBuffMaxDuration(plugin.getConfigManager().getBuffMaxDuration());
        v.setApplicationExpireSeconds(plugin.getConfigManager().getApplicationExpireSeconds());
        v.setWebLogCapacity(plugin.getConfigManager().getWebLogCapacity());
        return v;
    }
}
