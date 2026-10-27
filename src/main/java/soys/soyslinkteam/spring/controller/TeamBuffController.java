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
import soys.soyslinkteam.spring.entity.dto.BuffAddDTO;
import soys.soyslinkteam.spring.entity.dto.BuffRemoveDTO;
import soys.soyslinkteam.spring.service.TeamBuffService;

/**
 * 队伍增幅控制器：列表 / 添加 / 移除 / 可选项。
 */
@RequestMapping("/buff")
@ApiPermission("soyslinkteam.admin")
public class TeamBuffController {

    private final TeamBuffService buffService;

    public TeamBuffController(TeamBuffService buffService) {
        this.buffService = buffService;
    }

    @ApiName("增幅列表")
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(name = "teamId") String teamId) {
        return AjaxResult.success(buffService.list(teamId));
    }

    @ApiName("添加增幅")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody BuffAddDTO dto, ApiRequestContext ctx) {
        return buffService.add(dto, ctx);
    }

    @ApiName("移除增幅")
    @PostMapping("/remove")
    public AjaxResult remove(@RequestBody BuffRemoveDTO dto, ApiRequestContext ctx) {
        return buffService.remove(dto, ctx);
    }

    @ApiName("增幅可选项")
    @GetMapping("/options")
    public AjaxResult options() {
        return AjaxResult.success(buffService.options());
    }
}
