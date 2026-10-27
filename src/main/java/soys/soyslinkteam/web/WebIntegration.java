package soys.soyslinkteam.web;

import com.github.cocosoys.mc.soyshttpovermc.HttpOverMcPlugin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import soys.soyslinkteam.SOYSLinkTeam;

/**
 * SOYSHTTPOverMC / MCERP 接入器。
 *
 * <p>负责在插件启用时探测宿主并挂载 {@link TeamExpansion}——由该扩展一次性完成：</p>
 * <ul>
 *   <li>SOYSHTTPOverMC 侧：标准命名空间登记 {@code /api/plugins/soyslinkteam/*} 下的
 *       spring 分层 controller（{@code soys.soyslinkteam.spring.*}），并托管前端 dist
 *       于 {@code /web/plugins/soyslinkteam/}；</li>
 *   <li>MCERP 侧：登记 ERP 门户模块与多入口菜单（MCERP 缺失时静默跳过）。</li>
 * </ul>
 *
 * <p>注册与反注册由 {@code SoysExpansion} 骨架管理：端点在插件禁用时由宿主按 owner 自动清理，
 * 但 {@code SoysExpansion.REGISTERED} 静态表与 MCERP 的 {@code ErpRegistry} 索引
 * <b>必须</b>经 {@link TeamExpansion#unregister()} 显式摘除，否则门户会残留死菜单——
 * 这正是 {@link #shutdown()} 的职责。</p>
 *
 * <p>SOYSHTTPOverMC 未安装时仅初始化操作日志缓冲，网页功能静默停用，
 * 不影响游戏内队伍功能。</p>
 */
public class WebIntegration {

    private final SOYSLinkTeam plugin;
    private WebLogManager logManager;
    private TeamExpansion expansion;
    private boolean hooked;

    public WebIntegration(SOYSLinkTeam plugin) {
        this.plugin = plugin;
    }

    /**
     * 初始化日志缓冲并尝试挂载网页管理。应在队伍 / 存储等核心管理器就绪后调用。
     */
    public void hook() {
        this.logManager = new WebLogManager();
        this.logManager.setCapacity(plugin.getConfigManager().getWebLogCapacity());

        Plugin httpPlugin = Bukkit.getPluginManager().getPlugin("SOYSHTTPOverMC");
        if (!(httpPlugin instanceof HttpOverMcPlugin) || !httpPlugin.isEnabled()) {
            plugin.getLogger().info("未检测到 SOYSHTTPOverMC，网页管理功能停用。");
            return;
        }
        try {
            this.expansion = new TeamExpansion(this);
            if (!expansion.register()) {
                plugin.getLogger().warning("队伍 ERP 扩展注册失败（identifier 冲突 / bootstrap 未就绪）");
                this.expansion = null;
                return;
            }
            hooked = true;
            plugin.getLogger().info("已挂载队伍 ERP：/web/plugins/soyslinkteam/"
                    + (isMcerpPresent() ? "，并已登记 ERP 门户模块" : "（未检测到 MCERP，跳过门户登记）"));
        } catch (Throwable t) {
            plugin.getLogger().warning("挂载网页管理失败: " + t.getMessage());
        }
    }

    /**
     * 关闭集成。
     *
     * <p>必须显式反注册：宿主的 {@code ApiLifecycleListener} 只会按 owner 清理端点与页面，
     * 不会清理 {@code SoysExpansion.REGISTERED} 与 MCERP 的 {@code ErpRegistry}——
     * 遗漏会在门户里留下指向 404 页面的死菜单。</p>
     */
    public void shutdown() {
        if (expansion != null) {
            try {
                expansion.unregister();
            } catch (Throwable t) {
                plugin.getLogger().warning("反注册队伍 ERP 扩展失败: " + t.getMessage());
            }
            expansion = null;
        }
        hooked = false;
    }

    /** 是否存在已启用的 MCERP（仅用于日志措辞，注册本身由 McerpExpansion 自主降级）。 */
    private boolean isMcerpPresent() {
        Plugin mcerp = Bukkit.getPluginManager().getPlugin("MCERP");
        return mcerp != null && mcerp.isEnabled();
    }

    public boolean isHooked() {
        return hooked;
    }

    public WebLogManager getLogManager() {
        return logManager;
    }

    public SOYSLinkTeam getPlugin() {
        return plugin;
    }

    /** 当前 ERP 扩展实例（未挂载时为 null）。 */
    public TeamExpansion getExpansion() {
        return expansion;
    }
}
