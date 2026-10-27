package soys.soyslinkteam.web;

import com.github.cocosoys.mc.mcerp.ErpMenus;
import com.github.cocosoys.mc.mcerp.McerpExpansion;
import com.github.cocosoys.mc.soyshttpovermc.api.SoysHttpOverMcApi;
import org.bukkit.plugin.Plugin;
import soys.soyslinkteam.SOYSLinkTeam;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * SOYSLinkTeam 的 MCERP ERP 模块扩展。
 *
 * <p>继承 {@link McerpExpansion}（其本身继承 {@code SoysExpansion}），一次
 * {@link #register()} 同时完成两件事：</p>
 * <ul>
 *   <li><b>SOYSHTTPOverMC 侧</b>：代理登记 {@link TeamAdminApi} / {@link TeamUserApi}
 *       （{@link #buildProxyControllers()}）与两个页面 {@code /team-admin}、{@code /team-user}
 *       （覆写 {@link #registerPages()}）——<b>对外 URL 与既有部署完全一致</b>；</li>
 *   <li><b>MCERP 侧</b>：{@code onRegister()} 默认实现把本模块写入 {@code ErpRegistry}，
 *       门户菜单由 {@link #menus()} 实时合成（即安即生效）。</li>
 * </ul>
 *
 * <p>本模块<b>不</b>登记 {@code /api/plugins/SOYSLinkTeam/*} 命名空间端点
 * （{@link #buildControllers()} 返回空），以保持既有代理路径唯一、避免端点面重复。</p>
 *
 * <p><b>降级语义</b>：MCERP 未安装 / 未就绪时，{@code McerpExpansion.mcerp} 为 null，
 * {@code onRegister()} 静默跳过并返回 true，端点与页面照常工作；此时后续
 * {@code /mcerp reload} 或 {@code /soyshttp reload} 会兜底补登记。</p>
 *
 * <p><b>生命周期</b>：禁用时必须调用 {@link #unregister()}（由 {@code WebIntegration.shutdown()} 负责）。
 * 宿主的 {@code ApiLifecycleListener} 只会按 owner 清理端点与页面，<b>不会</b>清理
 * {@code SoysExpansion.REGISTERED} 静态表，也不会通知 MCERP 的 {@code ErpRegistry}——
 * 漏掉反注册会在门户里留下指向 404 页面的死菜单。</p>
 */
public class TeamExpansion extends McerpExpansion {

    /**
     * 管理后台权限节点。
     *
     * <p>必须与 {@link TeamAdminApi} 类级 {@code @ApiPermission} 保持一致：
     * 网关按该 Bukkit 节点鉴权（plugin.yml 中 {@code default: op}），
     * ERP 菜单的 perms 也使用同一节点，从而保证「菜单可见 = 接口可调」。</p>
     */
    public static final String PERM_ADMIN = "soyslinkteam.admin";

    /** 模块唯一标识：页面 tag / 冲突检测 / ERP 模块 id */
    private static final String IDENTIFIER = "soyslinkteam";

    /** 管理后台页面（代理路径，保持既有书签可用） */
    private static final String PAGE_ADMIN = "/team-admin";

    /** 用户端页面（代理路径，保持既有书签可用） */
    private static final String PAGE_USER = "/team-user";

    private final WebIntegration integration;

    public TeamExpansion(WebIntegration integration) {
        this.integration = integration;
    }

    private SOYSLinkTeam plugin() {
        return integration.getPlugin();
    }

    // ================================================================
    //  元信息
    // ================================================================

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public String getAuthor() {
        return "SOYS";
    }

    @Override
    public String getVersion() {
        return plugin().getDescription().getVersion();
    }

    // ================================================================
    //  ERP 声明
    // ================================================================

    /** 门户菜单标题 */
    @Override
    protected String displayName() {
        return "队伍管理";
    }

    /** 门户模块图标 */
    @Override
    protected String icon() {
        return "team";
    }

    /** 门户模块排序号（小在前） */
    @Override
    protected int sortOrder() {
        return 20;
    }

    /** 门户模块访问权限：非管理员不显示该模块 */
    @Override
    protected String permission() {
        return PERM_ADMIN;
    }

    /** 无子菜单时的默认页 */
    @Override
    protected String homeUrl() {
        return PAGE_ADMIN;
    }

    /**
     * 门户菜单树（v1 单入口）。
     *
     * <p>使用 {@code componentFullURL} 而非 {@code component}：后者会自动拼接
     * {@code pageDirRoot()}（{@code /web/plugins/soyslinkteam/soyslinkteam}），
     * 而本模块页面注册在代理路径 {@code /team-admin} 下，不能带该前缀。</p>
     *
     * <p>多入口深链（概览 / 队伍 / 增幅 / 申请 / 系统 / 日志）需前端 {@code admin.html}
     * 支持 hash 路由后再启用，见规划文档 Phase 3。</p>
     */
    @Override
    protected ErpMenus menus() {
        return ErpMenus.create()
                .menu("admin", "队伍管理后台", "team")
                .componentFullURL(PAGE_ADMIN)
                .perms(PERM_ADMIN);
    }

    // ================================================================
    //  端点 / 页面登记
    // ================================================================

    /** 不登记 {@code /api/plugins/SOYSLinkTeam/*} 命名空间端点，保持既有代理路径唯一。 */
    @Override
    protected List<Object> buildControllers() {
        return Collections.emptyList();
    }

    /** 代理登记两个 API 控制器：{@code /api/admin/*} 与 {@code /api/user/*}（路径不变）。 */
    @Override
    protected List<Object> buildProxyControllers() {
        return Arrays.asList(new TeamAdminApi(integration), new TeamUserApi(integration));
    }

    /** 不使用目录托管（页面以手工代理登记方式注册，路径为 {@code /team-admin}、{@code /team-user}）。 */
    @Override
    protected String resourceRoot() {
        return null;
    }

    /**
     * 覆写页面登记：手工代理登记两个 HTML 页面，保持既有 URL。
     *
     * <p>返回 false 会触发骨架整体回滚（已登记的端点一并撤销），
     * 避免出现「端点已上线但页面 404」的半注册状态——数据不完整比不可用更危险。</p>
     */
    @Override
    protected boolean registerPages() {
        Plugin owner = getOwner();
        SoysHttpOverMcApi api = getApi();
        if (owner == null || api == null) {
            return false;
        }
        try {
            api.getWebPage().registerProxyPage(owner, PAGE_ADMIN,
                    readResource("web/admin.html"), "text/html; charset=utf-8");
            api.getWebPage().registerProxyPage(owner, PAGE_USER,
                    readResource("web/user.html"), "text/html; charset=utf-8");
            return true;
        } catch (Exception ex) {
            plugin().getLogger().warning("队伍网页代理登记失败: " + ex.getMessage());
            return false;
        }
    }

    // ================================================================
    //  辅助
    // ================================================================

    /** 读取 jar 内资源为字节数组。 */
    private byte[] readResource(String path) throws Exception {
        try (InputStream in = plugin().getResource(path)) {
            if (in == null) {
                throw new IllegalStateException("资源不存在: " + path);
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
