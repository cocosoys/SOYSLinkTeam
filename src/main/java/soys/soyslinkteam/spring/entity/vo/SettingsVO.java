package soys.soyslinkteam.spring.entity.vo;

/**
 * 系统设置只读视图（来自 ConfigManager）。
 *
 * <p>结构性存储项（storage.backends 启停）不在 ERP 编辑范围，避免误重建连接。</p>
 */
public class SettingsVO {

    private String language;
    private boolean debug;
    private int defaultMaxSize;
    private int hardMaxSize;
    private boolean mirrorEnabled;
    private boolean mirrorAsync;
    private boolean teamChatEnabled;
    private boolean nametagEnabled;
    private int buffDefaultDuration;
    private int buffMaxDuration;
    private int applicationExpireSeconds;
    private int webLogCapacity;

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public int getDefaultMaxSize() {
        return defaultMaxSize;
    }

    public void setDefaultMaxSize(int defaultMaxSize) {
        this.defaultMaxSize = defaultMaxSize;
    }

    public int getHardMaxSize() {
        return hardMaxSize;
    }

    public void setHardMaxSize(int hardMaxSize) {
        this.hardMaxSize = hardMaxSize;
    }

    public boolean isMirrorEnabled() {
        return mirrorEnabled;
    }

    public void setMirrorEnabled(boolean mirrorEnabled) {
        this.mirrorEnabled = mirrorEnabled;
    }

    public boolean isMirrorAsync() {
        return mirrorAsync;
    }

    public void setMirrorAsync(boolean mirrorAsync) {
        this.mirrorAsync = mirrorAsync;
    }

    public boolean isTeamChatEnabled() {
        return teamChatEnabled;
    }

    public void setTeamChatEnabled(boolean teamChatEnabled) {
        this.teamChatEnabled = teamChatEnabled;
    }

    public boolean isNametagEnabled() {
        return nametagEnabled;
    }

    public void setNametagEnabled(boolean nametagEnabled) {
        this.nametagEnabled = nametagEnabled;
    }

    public int getBuffDefaultDuration() {
        return buffDefaultDuration;
    }

    public void setBuffDefaultDuration(int buffDefaultDuration) {
        this.buffDefaultDuration = buffDefaultDuration;
    }

    public int getBuffMaxDuration() {
        return buffMaxDuration;
    }

    public void setBuffMaxDuration(int buffMaxDuration) {
        this.buffMaxDuration = buffMaxDuration;
    }

    public int getApplicationExpireSeconds() {
        return applicationExpireSeconds;
    }

    public void setApplicationExpireSeconds(int applicationExpireSeconds) {
        this.applicationExpireSeconds = applicationExpireSeconds;
    }

    public int getWebLogCapacity() {
        return webLogCapacity;
    }

    public void setWebLogCapacity(int webLogCapacity) {
        this.webLogCapacity = webLogCapacity;
    }
}
