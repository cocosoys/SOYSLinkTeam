package soys.soyslinkteam.spring.service;

import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;

/**
 * 操作日志服务契约（内存环形缓冲，分页查询）。
 */
public interface TeamLogService {

    TableDataInfo logs(String level, String keyword, Integer pageNum, Integer pageSize);
}
