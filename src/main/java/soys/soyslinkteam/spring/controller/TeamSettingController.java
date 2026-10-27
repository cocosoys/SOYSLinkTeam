package soys.soyslinkteam.spring.controller;

import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiName;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiPermission;
import com.github.cocosoys.mc.soyshttpovermc.annotations.GetMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestMapping;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import soys.soyslinkteam.spring.service.TeamSettingService;

/**
 * 系统设置控制器（只读展示）。
 */
@RequestMapping("/settings")
@ApiPermission("soyslinkteam.admin")
public class TeamSettingController {

    private final TeamSettingService settingService;

    public TeamSettingController(TeamSettingService settingService) {
        this.settingService = settingService;
    }

    @ApiName("系统设置")
    @GetMapping("")
    public AjaxResult settings() {
        return AjaxResult.success(settingService.settings());
    }
}
