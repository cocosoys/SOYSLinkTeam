package soys.soyslinkteam.spring.controller;

import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiName;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiPermission;
import com.github.cocosoys.mc.soyshttpovermc.annotations.GetMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestParam;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import soys.soyslinkteam.spring.service.TeamLogService;

/**
 * 操作日志控制器。
 */
@RequestMapping("/log")
@ApiPermission("soyslinkteam.admin")
public class TeamLogController {

    private final TeamLogService logService;

    public TeamLogController(TeamLogService logService) {
        this.logService = logService;
    }

    @ApiName("操作日志")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(name = "level", required = false) String level,
                             @RequestParam(name = "keyword", required = false) String keyword,
                             @RequestParam(name = "pageNum", required = false) Integer pageNum,
                             @RequestParam(name = "pageSize", required = false) Integer pageSize) {
        return logService.logs(level, keyword, pageNum, pageSize);
    }
}
