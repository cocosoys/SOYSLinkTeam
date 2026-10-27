package soys.soyslinkteam.spring.entity.vo;

/**
 * ERP 概览统计视图。
 *
 * <p>对应原 /admin/overview 返回结构，强类型化替代 Map 手撕。</p>
 */
public class OverviewVO {

    private int totalTeams;
    private int loadedTeams;
    private int onlinePlayers;
    private int pendingApplications;
    private int activeBuffs;
    private int logEntries;

    public int getTotalTeams() {
        return totalTeams;
    }

    public void setTotalTeams(int totalTeams) {
        this.totalTeams = totalTeams;
    }

    public int getLoadedTeams() {
        return loadedTeams;
    }

    public void setLoadedTeams(int loadedTeams) {
        this.loadedTeams = loadedTeams;
    }

    public int getOnlinePlayers() {
        return onlinePlayers;
    }

    public void setOnlinePlayers(int onlinePlayers) {
        this.onlinePlayers = onlinePlayers;
    }

    public int getPendingApplications() {
        return pendingApplications;
    }

    public void setPendingApplications(int pendingApplications) {
        this.pendingApplications = pendingApplications;
    }

    public int getActiveBuffs() {
        return activeBuffs;
    }

    public void setActiveBuffs(int activeBuffs) {
        this.activeBuffs = activeBuffs;
    }

    public int getLogEntries() {
        return logEntries;
    }

    public void setLogEntries(int logEntries) {
        this.logEntries = logEntries;
    }
}
