package soys.soyslinkteam.team;

/**
 * 队伍可变设置项。
 * <p>与 {@link Team} 分离，便于后续版本扩展新设置而不污染队伍主体。</p>
 */
public class TeamSettings {

    /** 是否公开（允许任意玩家直接加入） */
    private volatile boolean open = false;

    /** 入队口令，null 或空字符串表示未设置 */
    private volatile String password = null;

    /** 队伍公告 */
    private volatile String notice = "";

    /** 队伍简称 */
    private volatile String tag = "";

    /** 入队费用（经济消耗入队），0 表示不收费 */
    private volatile double economyCost = 0.0;

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = (password == null || password.isEmpty()) ? null : password;
    }

    public boolean hasPassword() {
        return password != null && !password.isEmpty();
    }

    public String getNotice() {
        return notice == null ? "" : notice;
    }

    public void setNotice(String notice) {
        this.notice = notice == null ? "" : notice;
    }

    public boolean hasNotice() {
        return notice != null && !notice.isEmpty();
    }

    public String getTag() {
        return tag == null ? "" : tag;
    }

    public void setTag(String tag) {
        this.tag = tag == null ? "" : tag;
    }

    public boolean hasTag() {
        return tag != null && !tag.isEmpty();
    }

    public double getEconomyCost() {
        return economyCost;
    }

    public void setEconomyCost(double economyCost) {
        this.economyCost = Math.max(0, economyCost);
    }

    public boolean hasEconomyCost() {
        return economyCost > 0;
    }
}
