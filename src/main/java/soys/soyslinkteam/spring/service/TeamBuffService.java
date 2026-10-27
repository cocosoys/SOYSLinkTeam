package soys.soyslinkteam.spring.service;

import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.web.ApiRequestContext;
import soys.soyslinkteam.spring.entity.dto.BuffAddDTO;
import soys.soyslinkteam.spring.entity.dto.BuffRemoveDTO;
import soys.soyslinkteam.spring.entity.vo.BuffOptionsVO;
import soys.soyslinkteam.spring.entity.vo.BuffVO;

import java.util.List;

/**
 * 队伍增幅服务契约：列表 / 添加 / 移除 / 可选项。
 */
public interface TeamBuffService {

    List<BuffVO> list(String teamId);

    AjaxResult add(BuffAddDTO dto, ApiRequestContext ctx);

    AjaxResult remove(BuffRemoveDTO dto, ApiRequestContext ctx);

    BuffOptionsVO options();
}
