# MCERP 扩展 · 后端接口实现规划

> 版本 v1 · 2026-10-23 · 状态：待确认 · 范围：**仅后端（Java 侧）**
> 目标：SOYSLinkTeam 作为 MCERP 的 ERP 模块接入，复用现有 28 个 REST 端点，**对外 URL 零变化**。

---

## 0. 结论摘要

1. **接入方式**：新建 `TeamExpansion extends McerpExpansion`，一次 `register()` 同时完成 SOYSHTTPOverMC 端点/页面登记 + MCERP ERP 菜单登记（`McerpExpansion` 继承自 `SoysExpansion`，是同一套模板方法骨架）。
2. **路径零变化**：`/api/admin/*`、`/api/user/*`、`/team-admin`、`/team-user` 全部保持原样——靠 `buildProxyControllers()`（无 `/plugins` 前缀）+ 覆写 `registerPages()` 手动代理登记页面实现。
3. **依赖变更**：`soyshttpovermc-core/-common` 1.4.0 → **1.4.1**（已验证零破坏）；新增 `MCERP:1.0-SNAPSHOT` provided。
4. **P0 修复（非新功能）**：`TeamUserApi` 的 13 个端点在当前版本（1.4.0/1.4.1 均含）被判为"默认拒绝 403"，必须加类级 `@ApiPublic` 恢复可用。
5. **不新增业务端点**：ERP 门户复用现有 28 个端点；模块元信息由 `ErpRegistry` 实时从扩展实例合成，无需自建元信息接口。
6. **本期工作量集中在装配层**（1 个新类 + 3 处改造），业务逻辑一行不动。

---

## 1. 现状核查（源码 / 字节码双向验证）

### F1 —— 【P0】`TeamUserApi` 13 个端点当前默认 403

| 环节 | 事实 |
|---|---|
| 权限服务注册 | `HttpOverMcPluginProxy:414/638` 在启动与每次 `/soyshttp reload` **无条件** `setPermissionService(CombinedPermissionService)` |
| 默认拒绝分支 | `ApiRegistry:448`：`permissionService != null && meta.permission.isEmpty() && !isPublicEndpoint(meta)` → 返回 403 `ajax.registry.default-denied` |
| `isPublicEndpoint` | 仅认方法级/类级 `@ApiPublic` 或 `@Anonymous`（`ApiRegistry:661`） |
| `TeamUserApi` 注解 | **类与方法均无任何权限注解** → 13 个端点全部命中默认拒绝 |
| 版本确认 | 反查本地 1.4.0 jar 的 `ApiRegistry.class` 常量池，`default-denied` / `ApiPublic` / `no-permission` 均存在 → **该逻辑 1.4.0 就有，不是升级引入** |

**结论**：`/team-user` 用户端在当前版本下全功能不可用（所有 `/api/user/*` 请求 403）。
**修复**：`TeamUserApi` 加类级 `@ApiPublic`，语义与 MCERP `ErpAuthController` 完全一致（"网关 auth 策略已保护 `/api/*`，此处仅解除注册表默认拒绝 → 已登录即可"）。

> 注：若你的测试服上用户端目前是正常的，说明跑的是更早的 SOYSHTTPOverMC 构建（1.3.x 或更早）。升级到 1.4.1 后该修复是**必须**项。

### F2 —— 网关鉴权 = Bukkit 权限节点精确匹配

`PlayerPermissionService.hasPermission` → `online.hasPermission(permission)`，无通配符兜底；`@ApiPermission` 的值就是 Bukkit 节点。
`TeamAdminApi` 现用类级 `@ApiPermission("soyslinkteam.admin")`（plugin.yml `default: op`）→ **当前有效，本期保持不动**。

### F3 —— ERP 前端菜单/按钮权限来源

`AuthServiceImpl.permissionsOf(credential)`：遍历 `registry.getModules()` 的模块/菜单/按钮 perms，逐个经 Bukkit 权限过滤后返回前端。
即 **菜单 perms 必须是玩家真实持有的 Bukkit 节点**，否则菜单不可见。`soyslinkteam.admin`（default op）天然适配。

### F4 —— 按钮投影规则（`ControllerPermScanner`）

| 规则 | 影响 |
|---|---|
| 按钮名 = 方法级 `@ApiName` → 类级 → 方法名 | `TeamAdminApi` 15 个端点**已有方法级中文 `@ApiName`**，按钮名免费获得 |
| 按钮 perms = 方法级 `@ApiPermission` → 类级 | `TeamAdminApi` 只有类级注解 → 15 个按钮**同 perms、同 menuId**（`menuId = perms` 串）→ **冲突** |
| 无 `@ApiPermission` 且无 `@ApiPublic` 的端点不生成按钮 | `TeamUserApi` 修复后（`@ApiPublic`）会产生无 perms 按钮 → 本次不引用它 |

**结论**：本期**不使用 `permsFrom()` 自动投影**（会产生 15 个同 ID 按钮）；菜单手写。细粒度权限 + 按钮投影列入 Phase 2。

### F5 —— 版本兼容（已验证，零破坏）

| 检查项 | 结果 |
|---|---|
| `registerProxyController(Object, Plugin)` 在 1.4.1 | ✓ `ApiRegistrationApi:43` |
| `registerProxyPage(Plugin, String, byte[], String)` 在 1.4.1 | ✓ `WebPageApi:152` |
| `HttpOverMcPlugin.getInstance().getApi()` | ✓ Lombok `@Getter` 生成 |
| 1.4.1 jar 是否含 `SoysExpansion` | ✓ 已比对 jar 条目 |
| MCERP jar（对 1.4.0 编译）跑在 1.4.1 运行时 | 二进制兼容：`register()/onRegister()/getOwner()/getApi()/pageFullPrefix()` 签名均未变 |
| 本地仓库可用性 | `soyshttpovermc-core/-common` 1.4.1 ✓、`MCERP:1.0-SNAPSHOT` ✓ |

### F6 —— 【设计约束】禁用时必须显式 `unregister()`

`ApiLifecycleListener` 只按插件名清理 `ApiRegistry` + `WebRegistry`（第 32-33 行），**不清理** `SoysExpansion.REGISTERED`，也不通知 MCERP 的 `ErpRegistry`。

后果：若插件禁用时不调用 `TeamExpansion.unregister()`：
- 端点/页面 → 被自动清理 ✓
- `SoysExpansion.REGISTERED["soyslinkteam"]` → **残留**
- MCERP `ErpRegistry.dynamic` → 残留；`getModules()` 仍从 `REGISTERED` 反查到实例并返回菜单 → **门户里留下一个点进去 404 的死菜单** ✗

**约束**：`WebIntegration.shutdown()` 必须调用 `expansion.unregister()`（幂等，内部 `registered` 标志保护）。
禁用顺序天然安全：SOYSLinkTeam 后加载 → 先禁用 → 我们的 `unregister()` 先于 MCERP 的 `onDisable` 执行。

---

## 2. 总体设计

### 2.1 类结构

```
soys.soyslinkteam.web
├── TeamExpansion.java      【新增】extends McerpExpansion
│     getIdentifier()            = "soyslinkteam"
│     displayName()              = "队伍管理"
│     permission()               = "soyslinkteam.admin"
│     homeUrl()                  = "/team-admin"
│     buildControllers()         = 空列表（不进 /plugins/<插件名> 命名空间）
│     buildProxyControllers()    = [TeamAdminApi, TeamUserApi]   ← 保持旧路径
│     resourceRoot()             = null
│     registerPages()            【覆写】手动 registerProxyPage 注册两个页面
│     menus()                    = 单入口「队伍管理后台」→ componentFullURL("/team-admin")
│
├── WebIntegration.java     【改造】hook() 委托 TeamExpansion.register()
│                                    shutdown() 显式 expansion.unregister()（F6）
│                                    保留 WebLogManager 初始化 + 插件存在性检测
│
├── TeamAdminApi.java       【不动】15 端点 / 类级 @ApiPermission("soyslinkteam.admin")
├── TeamUserApi.java        【改】类级 @ApiPublic（F1 修复）
├── TeamWebDtos.java        【不动】
└── WebLogManager.java      【不动】
```

### 2.2 注册时序

```
Bukkit 加载顺序（softdepend 生效）
  SOYSHTTPOverMC → MCERP（depend SOYSHTTPOverMC）→ SOYSLinkTeam（softdepend 两者）
        ↓
MCERP.onEnable()
  McerpExpansion.setMcerp(this)        // 静态字段注入
        ↓
SOYSLinkTeam.onEnable()
  WebIntegration.hook()
    ├─ new WebLogManager(capacity)                     // 保留现状
    ├─ 检测 SOYSHTTPOverMC                              // 缺失 → 仅日志，静默停用
    └─ new TeamExpansion(this).register()
         ├─ registerData()      无声明 → 空成功
         ├─ registerControllers()
         │    ├─ buildControllers()     空 → 空成功
         │    └─ buildProxyControllers() → 2 个 API 代理登记（force=false）
         ├─ registerPages()     【覆写】2 个页面代理登记
         ├─ registerCors()      无声明 → 空成功
         └─ REGISTERED.put + onRegister()
              └─ McerpExpansion 默认实现 → ErpRegistry.registerModule(this)
                 （mcerp == null → 静默跳过返回 true，优雅降级）
```

### 2.3 失效路径矩阵

| 场景 | 端点 | 页面 | REGISTERED | ErpRegistry 索引 |
|---|---|---|---|---|
| SOYSLinkTeam 禁用 | 自动清理（owner） | 自动清理（owner） | **显式 `unregister()` 清理**（F6） | 由 `onUnregister()` 清理 |
| `/soyshttp reload` | 保留 | 保留 | 保留 | 保留 |
| `/mcerp reload` | 保留 | 保留 | 保留 | `reloadExpansions` 从 REGISTERED 兜底补登记 |
| MCERP 未安装 | 正常 | 正常 | 正常 | 不适用（`mcerp == null` 跳过） |
| SOYSHTTPOverMC 未安装 | 全部跳过（日志提示） | — | — | — |

### 2.4 ERP 菜单 v1

```java
@Override
protected ErpMenus menus() {
    return ErpMenus.create()
            .menu("admin", "队伍管理后台", "team")
            .componentFullURL("/team-admin")
            .perms(PERM_ADMIN);   // "soyslinkteam.admin"
}
```

**为什么用 `componentFullURL` 而不是 `component`**：
`component("xxx")` 会自动拼 `pageDirRoot()` = `/web/plugins/soyslinkteam/soyslinkteam`，而我们的页面注册在代理路径 `/team-admin`；`componentFullURL` 不做任何前缀拼接，直接使用原值。

**为什么 v1 只做单入口**：
`admin.html` 是 tab 驱动的单页 SPA，**没有 URL 路由**。若声明 6 个菜单全部指向 `/team-admin`，wujie 下会命中同一子应用实例，点击不同菜单不会切换内部 tab（体验割裂）。多入口深链需要前端配合（Phase 3）。

---

## 3. 接口清单与处置（28 端点全量）

### 3.1 `TeamAdminApi`（15）—— 全部保持代理登记，路径与鉴权不变

| # | 方法 | 路径（不变） | `@ApiName`（按钮名） | ERP v1 入口 | Phase 2 细粒度节点（暂定） |
|---|------|-------------|---------------------|------------|--------------------------|
| 1 | GET | `/api/admin/overview` | 管理概览 | — | `soyslinkteam.admin.overview` |
| 2 | GET | `/api/admin/teams` | 队伍列表 | — | `soyslinkteam.admin.team.list` |
| 3 | GET | `/api/admin/team` | 队伍详情 | — | `soyslinkteam.admin.team.query` |
| 4 | GET | `/api/admin/buffs` | 增幅列表 | — | `soyslinkteam.admin.buff.list` |
| 5 | POST | `/api/admin/buff/add` | 添加增幅 | — | `soyslinkteam.admin.buff.add` |
| 6 | POST | `/api/admin/buff/remove` | 移除增幅 | — | `soyslinkteam.admin.buff.remove` |
| 7 | GET | `/api/admin/applications` | 申请列表 | — | `soyslinkteam.admin.application.list` |
| 8 | POST | `/api/admin/application/approve` | 批准申请 | — | `soyslinkteam.admin.application.approve` |
| 9 | POST | `/api/admin/application/deny` | 拒绝申请 | — | `soyslinkteam.admin.application.deny` |
| 10 | POST | `/api/admin/transfer` | 更换队长 | — | `soyslinkteam.admin.team.transfer` |
| 11 | POST | `/api/admin/disband` | 解散队伍 | — | `soyslinkteam.admin.team.disband` |
| 12 | POST | `/api/admin/config` | 更新队伍配置 | — | `soyslinkteam.admin.team.config` |
| 13 | GET | `/api/admin/settings` | 系统设置 | — | `soyslinkteam.admin.settings.view` |
| 14 | GET | `/api/admin/logs` | 操作日志 | — | `soyslinkteam.admin.log.list` |
| 15 | GET | `/api/admin/buff-options` | 增幅可选项 | — | `soyslinkteam.admin.buff.options` |

> v1 的 ERP 入口列全部为「—」：菜单只做**门户入口**，按钮是 SPA 内部 UI，admin.html 不消费 ERP 按钮树。Phase 2 再把 15 个端点投影为按钮。

### 3.2 `TeamUserApi`（13）—— 全部加类级 `@ApiPublic`；不进 ERP 菜单

| # | 方法 | 路径（不变） | `@ApiName` |
|---|------|-------------|-----------|
| 1 | GET | `/api/user/me` | 我的信息 |
| 2 | GET | `/api/user/my-team` | 我的队伍 |
| 3 | GET | `/api/user/browse` | 浏览队伍 |
| 4 | POST | `/api/user/join` | 加入队伍 |
| 5 | POST | `/api/user/apply` | 提交申请 |
| 6 | GET | `/api/user/my-applications` | 我的申请 |
| 7 | POST | `/api/user/application/cancel` | 取消申请 |
| 8 | GET | `/api/user/team-profile` | 队伍公开详情 |
| 9 | GET | `/api/user/team-applications` | 本队申请列表 |
| 10 | POST | `/api/user/application/handle` | 队长审批申请 |
| 11 | POST | `/api/user/leave` | 离开队伍 |
| 12 | POST | `/api/user/chat` | 发送队伍聊天 |
| 13 | POST | `/api/user/chat/toggle` | 切换队伍频道 |

> 用户端保持独立门户 `/team-user`，不进 ERP 管理菜单（ERP 是管理后台）。

### 3.3 新增端点 —— 无

ERP 门户的模块/菜单/路由由 `ErpRegistry` 实时从扩展实例合成，不需要 SOYSLinkTeam 暴露"模块元信息"接口。

---

## 4. 权限设计

### 4.1 本期（v1）：粗粒度，鉴权行为零变更

| 对象 | 权限节点 | 说明 |
|---|---|---|
| `TeamAdminApi`（类级） | `soyslinkteam.admin` | plugin.yml 已声明 `default: op`，不改 |
| `TeamExpansion.permission()`（模块） | `soyslinkteam.admin` | 模块整体仅管理员可见 |
| ERP 菜单 perms | `soyslinkteam.admin` | 同上 |
| `TeamUserApi`（类级） | `@ApiPublic` | 仅需登录，与 `ErpAuthController` 一致 |

非 op 管理员经权限插件（LuckPerms/Vault）授予 `soyslinkteam.admin` 即可见菜单并调用全部管理端点。

### 4.2 Phase 2：细粒度（需单独验证，不在本期）

- 15 个管理端点加方法级 `@ApiPermission`（见 3.1 右列）
- `plugin.yml` 同步声明全部节点，并挂为 `soyslinkteam.admin` 的 **children**
  （Bukkit 在授权/重算时展开 children → 现有 `soyslinkteam.admin` 持有者自动获得全部子节点，向后兼容）
- ⚠️ 风险：children 展开依赖权限插件对 Bukkit 权限树的导入（LuckPerms 一般支持，需实测）
- 验证三场景：① op 全通；② 仅授 `soyslinkteam.admin` 的非 op 全通；③ 仅授单个子节点只通对应端点
- 验证失败则回退 v1 粗粒度（菜单手写 `perm`，不用 `permsFrom`）

---

## 5. 构建与依赖变更

### 5.1 `pom.xml`

```xml
<!-- 版本提升 -->
<artifactId>soyshttpovermc-core</artifactId>   <version>1.4.1</version> <scope>provided</scope>
<artifactId>soyshttpovermc-common</artifactId> <version>1.4.1</version> <scope>provided</scope>

<!-- 新增 -->
<dependency>
    <groupId>com.github.cocosoys.mc</groupId>
    <artifactId>MCERP</artifactId>
    <version>1.0-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

注意：MCERP jar 是对 1.4.0 编译的，其传递依赖按 Maven `provided` 规则收敛；若编译报缺类（`jackson-annotations` 等），按报错补 `provided` 依赖即可（预计不需要——我们只引用 `McerpExpansion`/`ErpMenus`，不直接触碰 VO/Jackson 注解）。

### 5.2 `plugin.yml`

```yaml
softdepend:
  - Essentials
  - CMI
  - ProtocolLib
  - Vault
  - LuckPerms
  - SOYSHTTPOverMC
  - MCERP          # 新增
```

### 5.3 MCERP 重绑 1.4.1（可选，推荐）

| 步骤 | 命令 / 改动 |
|---|---|
| 1 | `MCERP/pom.xml`：`<soys.version>1.4.0</soys.version>` → `1.4.1` |
| 2 | SOYSHTTPOverMC：`mvn install`（MCERP 的 systemPath 依赖 `core/target/SOYSHTTPOverMC-1_12-<ver>.jar`） |
| 3 | MCERP：`mvn install` |
| 4 | SOYSLinkTeam：`mvn clean package` |

不重绑的后果：**无功能影响**（F5 已证二进制兼容），仅版本口径不统一。

---

## 6. 实施骨架（可直接落地的代码轮廓）

### 6.1 `TeamExpansion.java`（新增）

```java
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

public class TeamExpansion extends McerpExpansion {

    /** 管理后台权限节点（与 TeamAdminApi 类级 @ApiPermission 保持一致） */
    public static final String PERM_ADMIN = "soyslinkteam.admin";

    private final WebIntegration integration;

    public TeamExpansion(WebIntegration integration) {
        this.integration = integration;
    }

    private SOYSLinkTeam plugin() {
        return integration.getPlugin();
    }

    // ===== 元信息 =====
    @Override public String getIdentifier() { return "soyslinkteam"; }
    @Override public String getAuthor() { return "SOYS"; }
    @Override public String getVersion() { return plugin().getDescription().getVersion(); }

    // ===== ERP 声明 =====
    @Override protected String displayName() { return "队伍管理"; }
    @Override protected String icon() { return "team"; }
    @Override protected int sortOrder() { return 20; }
    @Override protected String permission() { return PERM_ADMIN; }
    @Override protected String homeUrl() { return "/team-admin"; }

    /** 不登记 /plugins/<插件名> 命名空间端点（保持既有代理路径） */
    @Override protected List<Object> buildControllers() { return Collections.emptyList(); }

    /** 代理登记：保持 /api/admin/* 与 /api/user/* */
    @Override protected List<Object> buildProxyControllers() {
        return Arrays.asList(new TeamAdminApi(integration), new TeamUserApi(integration));
    }

    /** 无目录托管（页面走手工代理登记，保持 /team-admin、/team-user） */
    @Override protected String resourceRoot() { return null; }

    /** 覆写：手工代理登记两个页面（替代原 WebIntegration 中的直接调用） */
    @Override protected boolean registerPages() {
        Plugin owner = getOwner();
        SoysHttpOverMcApi api = getApi();
        if (owner == null || api == null) return false;
        try {
            api.getWebPage().registerProxyPage(owner, "/team-admin",
                    readResource("web/admin.html"), "text/html; charset=utf-8");
            api.getWebPage().registerProxyPage(owner, "/team-user",
                    readResource("web/user.html"), "text/html; charset=utf-8");
            return true;
        } catch (Exception ex) {
            plugin().getLogger().warning("队伍网页代理登记失败: " + ex.getMessage());
            return false;   // 返回 false → 骨架整体回滚（端点一并撤销），保证原子性
        }
    }

    @Override protected ErpMenus menus() {
        return ErpMenus.create()
                .menu("admin", "队伍管理后台", "team")
                .componentFullURL("/team-admin")
                .perms(PERM_ADMIN);
    }

    private byte[] readResource(String path) throws Exception {
        try (InputStream in = plugin().getResource(path)) {
            if (in == null) throw new IllegalStateException("资源不存在: " + path);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            return out.toByteArray();
        }
    }
}
```

### 6.2 `WebIntegration.java`（改造要点）

```java
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
            return;
        }
        hooked = true;
        plugin.getLogger().info("SOYSHTTPOverMC 网页管理已挂载：/team-admin（后台） /team-user（用户端）");
    } catch (Throwable t) {
        plugin.getLogger().warning("挂载网页管理失败: " + t.getMessage());
    }
}

public void shutdown() {
    // F6：必须显式反注册，否则 SoysExpansion.REGISTERED 与 MCERP ErpRegistry 残留死菜单
    if (expansion != null) {
        expansion.unregister();
        expansion = null;
    }
    hooked = false;
}
```

### 6.3 `TeamUserApi.java`（一行修复）

```java
@ApiName("队伍用户端")
@ApiPublic   // 网关 auth 策略已保护 /api/*（无凭证 401），此处解除注册表默认拒绝 → 已登录即可
public class TeamUserApi {
```

---

## 7. 分阶段实施计划

### Phase 1 —— 装配与 P0 修复（本期）✅ 已完成（2026-10-23）

- [x] **T1** `TeamUserApi` 加类级 `@ApiPublic` + 说明注释（F1）
- [x] **T2** 新建 `TeamExpansion`（元信息 / `buildProxyControllers` / `registerPages` 覆写 / `menus`）
- [x] **T3** `WebIntegration` 改造：`hook()` 委托 `register()`；`shutdown()` 调 `unregister()`（F6）；页面资源读取逻辑平移
- [x] **T4** `pom.xml`：1.4.0 → 1.4.1；新增 `MCERP:1.0-SNAPSHOT` provided
- [x] **T5** `plugin.yml`：`softdepend` += `MCERP`
- [x] **T6** 构建验证：**Maven 真实编译通过**（103 源文件 → `target/SOYSLinkTeam-1.0.0.jar`，8s；jar 内 `TeamExpansion.class` 存在，`com/github/cocosoys` 零打包）
- [ ] **T7**（可选）MCERP 重绑 1.4.1 + 三项目重建链 —— 留待用户按本机链路执行
- [x] **T8** `TODO.md` 登记 + 工作记忆更新

### Phase 2 —— 细粒度权限与按钮投影（后续）

- [ ] **T9** 15 个端点方法级 `@ApiPermission` + `plugin.yml` 节点声明 + `soyslinkteam.admin` children 兼容
- [ ] **T10** `menus()` 扩为 6 入口（概览/队伍/增幅/申请/系统/日志）+ `permsFrom` 按钮投影
- [ ] **T11** 权限三场景实机验证（op / 粗授权 / 单节点授权）

### Phase 3 —— 门户深链与子应用化（后续）

- [ ] **T12** `admin.html` 增加 hash 路由（读 `location.hash` 定位初始 tab + `hashchange` 监听）
- [ ] **T13** `menus()` 改多入口 `componentFullURL("/team-admin#/teams")` 等 + wujie 嵌入态实测（会话 Cookie 同源流转、SPA 兜底）
- [ ] **T14** 数据层接入评估（`dataRoots()`/`schemaVersion()` 与现有 `config-version` 机制二选一，避免双轨）

---

## 8. 风险清单

| # | 风险 | 等级 | 缓解措施 |
|---|------|------|----------|
| R1 | T1 改变用户端鉴权语义（默认拒绝 → 登录即可） | 中 | **本质是修复**：现状全 403，与设计意图（仅需登录）不符；修复后与 MCERP `ErpAuthController` 语义对齐 |
| R2 | MCERP 未安装时 `register()` 行为 | 低 | `McerpExpansion.onRegister()` 对 `mcerp == null` 静默返回 `true`；端点/页面照常 |
| R3 | identifier 冲突 | 低 | `"soyslinkteam"` 与 `"MCERP"` 不冲突；骨架有冲突检测 + 拒绝注册 |
| R4 | 重复注册（`hook()` 被调两次） | 低 | `register()` 首行 `if (registered) return false`；`hook()` 仅 `onEnable` 调用一次 |
| R5 | 页面登记失败导致整体回滚 | 低 | 设计如此：`registerPages()` 返回 false → 端点一并撤销，避免"半注册"状态；日志明确提示 |
| R6 | 禁用后 MCERP 菜单残留 | 中 | **已定位并纳入设计**：`shutdown()` 显式 `unregister()`（F6） |
| R7 | MCERP provided 传递依赖缺类 | 中 | 编译期即暴露，按报错补依赖 |
| R8 | Phase 2 children 展开不被权限插件支持 | 中 | 三场景实测；失败回退粗粒度 |

---

## 9. 验证清单（Phase 1 完成后 · 测试服）

1. `mvn clean package` 通过，产出 jar
2. **不装 MCERP**：`/team-admin`、`/team-user`、`/api/admin/*`、`/api/user/*` 全部正常（回归，确认降级路径）
3. **装 MCERP**：门户出现「队伍管理」菜单；op 可见，非 op 且无 `soyslinkteam.admin` 不可见
4. 点击菜单进入 `/team-admin`，页面功能全通
5. **重点**：`/api/user/*` 13 个端点由 403 恢复 200（F1 修复生效）
6. `/mcerp reload` 后菜单仍在；`/soyshttp reload` 后端点仍注册
7. 禁用 SOYSLinkTeam → 端点 / 页面 / 菜单登记全部摘除，门户无残留死菜单（F6 验证）
8. 重启服务器 → 菜单与页面自动恢复

---

## 10. 不在本期范围

- 前端 ERP 子应用改造（按钮树消费、hash 深链）→ Phase 3
- 细粒度权限与按钮自动投影 → Phase 2
- 数据层自动运维接入（`dataRoots()/schemaVersion()`）→ Phase 3 评估
- `/team-user` 纳入 ERP 门户 → 用户端保持独立入口
- 新增业务端点 → 无需（复用现有 28 个）
