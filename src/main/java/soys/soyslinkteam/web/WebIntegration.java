package soys.soyslinkteam.web;

import com.github.cocosoys.mc.soyshttpovermc.HttpOverMcPlugin;
import com.github.cocosoys.mc.soyshttpovermc.api.SoysHttpOverMcApi;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import soys.soyslinkteam.SOYSLinkTeam;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * SOYSHTTPOverMC 网页集成器。
 *
 * <p>负责在插件启用时检测 SOYSHTTPOverMC，并注册：
 * <ul>
 *   <li>ERP 管理后台 API（{@link TeamAdminApi}）与用户端 API（{@link TeamUserApi}）；</li>
 *   <li>两个网页：{@code /team-admin}（管理后台）与 {@code /team-user}（TIM 用户端）。</li>
 * </ul>
 * 控制器与网页均以本插件为 owner 走代理登记（路径简洁、无插件前缀），
 * 本插件被禁用 / 卸载时由 SOYSHTTPOverMC 自动清理。</p>
 *
 * <p>SOYSHTTPOverMC 未安装时仅初始化操作日志缓冲，网页功能静默停用，
 * 不影响游戏内队伍功能。</p>
 */
public class WebIntegration {

    private final SOYSLinkTeam plugin;
    private WebLogManager logManager;
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
            SoysHttpOverMcApi api = ((HttpOverMcPlugin) httpPlugin).getApi();

            // 注册控制器（代理登记：/api/admin/* 与 /api/user/*）
            api.getApiRegistration().registerProxyController(new TeamAdminApi(this), plugin);
            api.getApiRegistration().registerProxyController(new TeamUserApi(this), plugin);

            // 注册网页（代理登记：/team-admin 与 /team-user）
            byte[] adminBytes = readResource("web/admin.html");
            byte[] userBytes = readResource("web/user.html");
            api.getWebPage().registerProxyPage(plugin, "/team-admin", adminBytes,
                    "text/html; charset=utf-8");
            api.getWebPage().registerProxyPage(plugin, "/team-user", userBytes,
                    "text/html; charset=utf-8");

            hooked = true;
            plugin.getLogger().info("SOYSHTTPOverMC 网页管理已挂载：/team-admin（后台） /team-user（用户端）");
        } catch (Throwable t) {
            plugin.getLogger().warning("挂载网页管理失败: " + t.getMessage());
        }
    }

    /**
     * 关闭集成。注册项由 SOYSHTTPOverMC 按 owner 自动清理，这里仅复位状态。
     */
    public void shutdown() {
        hooked = false;
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

    private byte[] readResource(String resourcePath) throws Exception {
        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("资源不存在: " + resourcePath);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }
}
