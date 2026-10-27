package soys.soyslinkteam.spring.service;

import soys.soyslinkteam.spring.entity.vo.OverviewVO;

/**
 * ERP 概览服务契约。
 */
public interface TeamOverviewService {

    /** 概览统计：队伍数 / 在线人数 / 待处理申请 / 进行中增幅 / 日志条数。 */
    OverviewVO overview();
}
