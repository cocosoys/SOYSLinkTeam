package soys.soyslinkteam.spring.controller;

import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiName;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiPermission;
import com.github.cocosoys.mc.soyshttpovermc.annotations.GetMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PathVariable;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PostMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestBody;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestParam;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.web.ApiRequestContext;
import soys.soyslinkteam.spring.entity.dto.DisbandDTO;
import soys.soyslinkteam.spring.entity.dto.TeamConfigDTO;
import soys.soyslinkteam.spring.entity.dto.TransferDTO;
import soys.soyslinkteam.spring.service.TeamService;

/**
 * 队伍管理控制器：列表 / 详情 / 更换队长 / 解散 / 队伍配置。
 */
@RequestMapping("/team")
@ApiPermission("soyslinkteam.admin")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @ApiName("队伍列表")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(name = "keyword", required = false) String keyword,
                              @RequestParam(name = "pageNum", required = false) Integer pageNum,
                              @RequestParam(name = "pageSize", required = false) Integer pageSize) {
        return teamService.list(keyword, pageNum, pageSize);
    }

    @ApiName("队伍详情")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable(name = "id") String id) {
        return teamService.detail(id);
    }

    @ApiName("更换队长")
    @PostMapping("/transfer")
    public AjaxResult transfer(@RequestBody TransferDTO dto, ApiRequestContext ctx) {
        return teamService.transfer(dto, ctx);
    }

    @ApiName("解散队伍")
    @PostMapping("/disband")
    public AjaxResult disband(@RequestBody DisbandDTO dto, ApiRequestContext ctx) {
        return teamService.disband(dto, ctx);
    }

    @ApiName("更新队伍配置")
    @PostMapping("/config")
    public AjaxResult updateConfig(@RequestBody TeamConfigDTO dto, ApiRequestContext ctx) {
        return teamService.updateConfig(dto, ctx);
    }
}
