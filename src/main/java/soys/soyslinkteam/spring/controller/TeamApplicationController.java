package soys.soyslinkteam.spring.controller;

import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiName;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiPermission;
import com.github.cocosoys.mc.soyshttpovermc.annotations.GetMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PostMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestBody;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestParam;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.web.ApiRequestContext;
import soys.soyslinkteam.spring.entity.dto.ApproveDTO;
import soys.soyslinkteam.spring.entity.dto.DenyDTO;
import soys.soyslinkteam.spring.service.TeamApplicationService;

/**
 * 入队申请审批控制器：列表 / 批准 / 拒绝。
 */
@RequestMapping("/application")
@ApiPermission("soyslinkteam.admin")
public class TeamApplicationController {

    private final TeamApplicationService applicationService;

    public TeamApplicationController(TeamApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @ApiName("申请列表")
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(name = "teamId") String teamId) {
        return AjaxResult.success(applicationService.list(teamId));
    }

    @ApiName("批准申请")
    @PostMapping("/approve")
    public AjaxResult approve(@RequestBody ApproveDTO dto, ApiRequestContext ctx) {
        return applicationService.approve(dto, ctx);
    }

    @ApiName("拒绝申请")
    @PostMapping("/deny")
    public AjaxResult deny(@RequestBody DenyDTO dto, ApiRequestContext ctx) {
        return applicationService.deny(dto, ctx);
    }
}
