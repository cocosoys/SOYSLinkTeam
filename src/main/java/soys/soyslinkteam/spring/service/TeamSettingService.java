package soys.soyslinkteam.spring.service;

import soys.soyslinkteam.spring.entity.vo.SettingsVO;

/**
 * 系统设置服务契约（只读展示，写回配置文件仍建议在服务器侧编辑后 /teamadmin reload）。
 */
public interface TeamSettingService {

    SettingsVO settings();
}
