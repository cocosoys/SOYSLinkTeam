package soys.soyslinkteam.spring.impl;

import com.github.cocosoys.mc.soyshttpovermc.util.PageUtils;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.spring.entity.vo.LogVO;
import soys.soyslinkteam.spring.service.TeamLogService;
import soys.soyslinkteam.spring.support.TeamVoAssembler;
import soys.soyslinkteam.web.WebIntegration;
import soys.soyslinkteam.web.WebLogManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 操作日志服务实现（内存环形缓冲，分页查询）。
 */
public class TeamLogServiceImpl implements TeamLogService {

    private final WebIntegration integration;

    public TeamLogServiceImpl(WebIntegration integration) {
        this.integration = integration;
    }

    @Override
    public TableDataInfo logs(String level, String keyword, Integer pageNum, Integer pageSize) {
        WebLogManager logManager = integration.getLogManager();
        List<LogVO> all = new ArrayList<>();
        if (logManager != null) {
            for (WebLogManager.LogEntry e : logManager.query(level, keyword, 0, Integer.MAX_VALUE)) {
                all.add(TeamVoAssembler.toLog(e));
            }
        }
        return PageUtils.page(all, pageNum, pageSize);
    }
}
