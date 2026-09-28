package soys.soyslinkteam.web;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Web 管理操作日志器。
 *
 * <p>记录后台管理员的关键操作（增幅、审批、更换队长、解散、改配置等），
 * 采用有界环形缓冲，避免无限增长；供 ERP 后台「操作日志」页查询。</p>
 */
public class WebLogManager {

    public enum Level {
        INFO("一般"), WARN("警告"), ERROR("错误");

        private final String displayName;

        Level(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 一条日志条目（结构便于 JSON 序列化）。
     */
    public static class LogEntry {
        private final long time;
        private final String level;
        private final String operator;
        private final String action;
        private final String detail;
        private final String ip;

        public LogEntry(long time, Level level, String operator, String action, String detail, String ip) {
            this.time = time;
            this.level = level.name();
            this.operator = operator == null ? "系统" : operator;
            this.action = action == null ? "" : action;
            this.detail = detail == null ? "" : detail;
            this.ip = ip == null ? "" : ip;
        }

        public long getTime() {
            return time;
        }

        public String getTimeText() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(time));
        }

        public String getLevel() {
            return level;
        }

        public String getOperator() {
            return operator;
        }

        public String getAction() {
            return action;
        }

        public String getDetail() {
            return detail;
        }

        public String getIp() {
            return ip;
        }
    }

    /** 有界环形缓冲（最新在前） */
    private final LinkedList<LogEntry> entries = new LinkedList<>();

    private int capacity = 1000;

    public WebLogManager() {
    }

    public void setCapacity(int capacity) {
        this.capacity = Math.max(50, capacity);
        synchronized (entries) {
            while (entries.size() > this.capacity) {
                entries.removeLast();
            }
        }
    }

    /**
     * 记录一条操作日志。
     */
    public void log(Level level, String operator, String action, String detail, String ip) {
        LogEntry entry = new LogEntry(System.currentTimeMillis(), level, operator, action, detail, ip);
        synchronized (entries) {
            entries.addFirst(entry);
            while (entries.size() > capacity) {
                entries.removeLast();
            }
        }
    }

    public void info(String operator, String action, String detail, String ip) {
        log(Level.INFO, operator, action, detail, ip);
    }

    public void warn(String operator, String action, String detail, String ip) {
        log(Level.WARN, operator, action, detail, ip);
    }

    public void error(String operator, String action, String detail, String ip) {
        log(Level.ERROR, operator, action, detail, ip);
    }

    /**
     * 分页查询日志。
     *
     * @param levelFilter 级别过滤（null / 空 = 全部）
     * @param keyword     关键词（匹配动作 / 操作者 / 详情，null = 不过滤）
     * @param page        页码（0 基）
     * @param pageSize    每页条数
     * @return 当前页日志
     */
    public List<LogEntry> query(String levelFilter, String keyword, int page, int pageSize) {
        List<LogEntry> filtered = new ArrayList<>();
        String kw = keyword == null ? null : keyword.trim().toLowerCase();
        synchronized (entries) {
            for (LogEntry entry : entries) {
                if (levelFilter != null && !levelFilter.isEmpty()
                        && !entry.getLevel().equalsIgnoreCase(levelFilter)) {
                    continue;
                }
                if (kw != null && !kw.isEmpty()) {
                    String haystack = (entry.getAction() + entry.getOperator() + entry.getDetail()).toLowerCase();
                    if (!haystack.contains(kw)) {
                        continue;
                    }
                }
                filtered.add(entry);
            }
        }
        int from = Math.max(0, page) * pageSize;
        if (from >= filtered.size()) {
            return Collections.emptyList();
        }
        int to = Math.min(filtered.size(), from + pageSize);
        return new ArrayList<>(filtered.subList(from, to));
    }

    /**
     * 符合过滤条件的日志总数（用于分页）。
     */
    public int count(String levelFilter, String keyword) {
        String kw = keyword == null ? null : keyword.trim().toLowerCase();
        int count = 0;
        synchronized (entries) {
            for (LogEntry entry : entries) {
                if (levelFilter != null && !levelFilter.isEmpty()
                        && !entry.getLevel().equalsIgnoreCase(levelFilter)) {
                    continue;
                }
                if (kw != null && !kw.isEmpty()) {
                    String haystack = (entry.getAction() + entry.getOperator() + entry.getDetail()).toLowerCase();
                    if (!haystack.contains(kw)) {
                        continue;
                    }
                }
                count++;
            }
        }
        return count;
    }

    public int size() {
        synchronized (entries) {
            return entries.size();
        }
    }
}
