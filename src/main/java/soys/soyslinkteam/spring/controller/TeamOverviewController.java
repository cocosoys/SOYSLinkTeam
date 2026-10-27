package soys.soyslinkteam.spring.controller;

import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiName;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiPermission;
import com.github.cocosoys.mc.soyshttpovermc.annotations.GetMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestMapping;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import soys.soyslinkteam.spring.service.TeamOverviewService;

/**
 * ERP 概览控制器。
 */
@RequestMapping("/overview")
@ApiPermission("soyslinkteam.admin")
public class TeamOverviewController {

    private final TeamOverviewService overviewService;

    public TeamOverviewController(TeamOverviewService overviewService) {
        this.overviewService = overviewService;
    }

    @ApiName("概览统计")
    @GetMapping("")
    public AjaxResult overview() {
        return AjaxResult.success(overviewService.overview());
    }
}
