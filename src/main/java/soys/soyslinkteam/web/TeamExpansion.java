package soys.soyslinkteam.web;

import com.github.cocosoys.mc.mcerp.ErpMenus;
import com.github.cocosoys.mc.mcerp.McerpExpansion;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.spring.controller.TeamApplicationController;
import soys.soyslinkteam.spring.controller.TeamBuffController;
import soys.soyslinkteam.spring.controller.TeamController;
import soys.soyslinkteam.spring.controller.TeamLogController;
import soys.soyslinkteam.spring.controller.TeamOverviewController;
import soys.soyslinkteam.spring.controller.TeamSettingController;
import soys.soyslinkteam.spring.impl.TeamApplicationServiceImpl;
import soys.soyslinkteam.spring.impl.TeamBuffServiceImpl;
import soys.soyslinkteam.spring.impl.TeamLogServiceImpl;
import soys.soyslinkteam.spring.impl.TeamOverviewServiceImpl;
import soys.soyslinkteam.spring.impl.TeamServiceImpl;
import soys.soyslinkteam.spring.impl.TeamSettingServiceImpl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * SOYSLinkTeam 的 MCERP ERP 模块扩展。
 *
 * <p>继承 {@link McerpExpansion}（其本身继承 {@code SoysExpansion}），一次
 * {@link #register()} 同时完成：</p>
 * <ul>
 *   <li><b>SOYSHTTPOverMC 侧</b>：标准命名空间登记 {@code /api/plugins/soyslinkteam/*}
 *       下的全部 spring 分层 controller（{@link #buildControllers()}），并托管前端 dist
 *       于 {@code /web/plugins/soyslinkteam/}；</li>
 *   <li><b>MCERP 侧</b>：{@code onRegister()} 把本模块写入 {@code ErpRegistry}，
 *       门户菜单由 {@link #menus()} 实时合成（概览/队伍/增幅/申请/设置/日志）。</li>
 * </ul>
 *
 * <p><b>降级语义</b>：MCERP 未安装/未就绪时，{@code McerpExpansion.mcerp} 为 null，
 * 门户登记静默跳过，端点与 dist 页面照常工作；后续 {@code /mcerp reload} 兜底补登记。</p>
 */
public class TeamExpansion extends McerpExpansion {

    /** 管理后台权限节点（与 controller 类级 @ApiPermission 一致）。 */
    public static final String PERM_ADMIN = "soyslinkteam.admin";

    /** 模块唯一标识：决定 /api/plugins/soyslinkteam/* 与 /web/plugins/soyslinkteam/* 前缀。 */
    private static final String IDENTIFIER = "soyslinkteam";

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

    @Override
    protected String displayName() {
        return "队伍管理";
    }

    @Override
    protected String icon() {
        return "team";
    }

    @Override
    protected int sortOrder() {
        return 20;
    }

    @Override
    protected String permission() {
        return PERM_ADMIN;
    }

    @Override
    protected String homeUrl() {
        return "/web/plugins/soyslinkteam/soyslinkteam/overview";
    }

    // ================================================================
    //  ERP 菜单（多入口，按钮权限由 controller 自动投影）
    // ================================================================

    @Override
    protected ErpMenus menus() {
        return ErpMenus.create()
                .menu("overview", "管理概览", "dashboard")
                .menu("team", "队伍管理", "peoples", TeamController.class)
                .menu("buff", "队伍增幅", "flag", TeamBuffController.class)
                .menu("application", "申请审批", "time", TeamApplicationController.class)
                .menu("settings", "系统设置", "setting", TeamSettingController.class)
                .menu("log", "操作日志", "log", TeamLogController.class);
    }

    // ================================================================
    //  端点登记（标准命名空间，薄层 controller → service → impl）
    // ================================================================

    @Override
    protected List<Object> buildControllers() {
        SOYSLinkTeam p = plugin();
        return Arrays.asList(
                new TeamOverviewController(new TeamOverviewServiceImpl(p, integration)),
                new TeamController(new TeamServiceImpl(p, integration)),
                new TeamBuffController(new TeamBuffServiceImpl(p, integration)),
                new TeamApplicationController(new TeamApplicationServiceImpl(p, integration)),
                new TeamSettingController(new TeamSettingServiceImpl(p)),
                new TeamLogController(new TeamLogServiceImpl(integration))
        );
    }

    /** 不再使用代理路径，统一走标准命名空间。 */
    @Override
    protected List<Object> buildProxyControllers() {
        return Collections.emptyList();
    }

    /** 前端 dist 目录（wujie 子应用构建产物）。 */
    @Override
    protected String resourceRoot() {
        return "dist";
    }
}
