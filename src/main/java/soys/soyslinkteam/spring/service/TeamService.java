package soys.soyslinkteam.spring.service;

import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.web.ApiRequestContext;
import soys.soyslinkteam.spring.entity.dto.DisbandDTO;
import soys.soyslinkteam.spring.entity.dto.TeamConfigDTO;
import soys.soyslinkteam.spring.entity.dto.TransferDTO;

/**
 * 队伍管理服务契约：列表 / 详情 / 更换队长 / 解散 / 队伍配置。
 */
public interface TeamService {

    TableDataInfo list(String keyword, Integer pageNum, Integer pageSize);

    AjaxResult detail(String id);

    AjaxResult transfer(TransferDTO dto, ApiRequestContext ctx);

    AjaxResult disband(DisbandDTO dto, ApiRequestContext ctx);

    AjaxResult updateConfig(TeamConfigDTO dto, ApiRequestContext ctx);
}
