package soys.soyslinkteam.spring.service;

import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.web.ApiRequestContext;
import soys.soyslinkteam.spring.entity.dto.ApproveDTO;
import soys.soyslinkteam.spring.entity.dto.DenyDTO;
import soys.soyslinkteam.spring.entity.vo.ApplicationVO;

import java.util.List;

/**
 * 入队申请审批服务契约：列表 / 批准 / 拒绝。
 */
public interface TeamApplicationService {

    List<ApplicationVO> list(String teamId);

    AjaxResult approve(ApproveDTO dto, ApiRequestContext ctx);

    AjaxResult deny(DenyDTO dto, ApiRequestContext ctx);
}
