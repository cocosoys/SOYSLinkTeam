package soys.soyslinkteam.web;

import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiName;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiPermission;
import com.github.cocosoys.mc.soyshttpovermc.annotations.GetMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PostMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestBody;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestParam;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.util.JsonReader;
import com.github.cocosoys.mc.soyshttpovermc.web.ApiRequestContext;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffectType;
import soys.soyslinkteam.application.JoinApplication;
import soys.soyslinkteam.buff.BuffKind;
import soys.soyslinkteam.buff.TeamBuff;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.util.PasswordUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ERP 管理后台 REST API（供管理员网页调用）。
 *
 * <p>覆盖：队伍列表 / 详情、队伍增幅、申请审批、更换队长、解散、队伍配置、
 * 系统设置、操作日志。写操作需要管理员权限并记录操作日志。</p>
 */
@ApiName("队伍ERP管理")
@ApiPermission("soyslinkteam.admin")
public class TeamAdminApi {

    private final WebIntegration integration;

    public TeamAdminApi(WebIntegration integration) {
        this.integration = integration;
    }

    private soys.soyslinkteam.SOYSLinkTeam plugin() {
        return integration.getPlugin();
    }

    // ================================================================
    //  概览
    // ================================================================

    @ApiName("管理概览")
    @GetMapping("/admin/overview")
    public AjaxResult overview() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalTeams", plugin().getTeamManager().getTotalCount());
        data.put("loadedTeams", plugin().getTeamManager().getLoadedCount());

        int onlinePlayers = plugin().getServer().getOnlinePlayers().size();
        data.put("onlinePlayers", onlinePlayers);

        int pendingApplications = 0;
        int activeBuffs = 0;
        for (Team team : plugin().getTeamManager().getLoadedTeams()) {
            pendingApplications += plugin().getApplicationManager().countApplications(team.getId());
            activeBuffs += plugin().getTeamBuffManager().getBuffs(team.getId()).size();
        }
        data.put("pendingApplications", pendingApplications);
        data.put("activeBuffs", activeBuffs);
        data.put("logEntries", integration.getLogManager().size());
        return AjaxResult.success(data);
    }

    // ================================================================
    //  队伍列表 / 详情
    // ================================================================

    @ApiName("队伍列表")
    @GetMapping("/admin/teams")
    public AjaxResult teams(@RequestParam(name = "keyword", required = false) String keyword) {
        List<Map<String, Object>> list = new ArrayList<>();
        String kw = keyword == null ? null : keyword.trim().toLowerCase();

        for (Map.Entry<UUID, String> entry : plugin().getTeamManager().getTeamNameIndex().entrySet()) {
            UUID id = entry.getKey();
            String name = entry.getValue();
            if (kw != null && !kw.isEmpty()
                    && (name == null || !name.toLowerCase().contains(kw))) {
                continue;
            }
            Team loaded = plugin().getTeamManager().getLoadedTeam(id);
            int maxSize = plugin().getTeamManager().getMaxSize(loaded);
            int buffCount = plugin().getTeamBuffManager().getBuffs(id).size();
            int appCount = plugin().getApplicationManager().countApplications(id);
            if (loaded != null) {
                list.add(TeamWebDtos.teamSummary(loaded, maxSize, buffCount, appCount));
            } else {
                // 未装载队伍：仅返回名称等轻量信息
                Map<String, Object> placeholder = new LinkedHashMap<>();
                placeholder.put("id", id.toString());
                placeholder.put("name", name);
                placeholder.put("size", -1);
                placeholder.put("onlineCount", -1);
                placeholder.put("maxSize", maxSize);
                placeholder.put("buffCount", buffCount);
                placeholder.put("applicationCount", appCount);
                placeholder.put("loaded", false);
                list.add(placeholder);
            }
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", list);
        data.put("total", list.size());
        return AjaxResult.success(data);
    }

    @ApiName("队伍详情")
    @GetMapping("/admin/team")
    public AjaxResult team(@RequestParam(name = "id") String id) {
        Team team = loadTeam(id);
        if (team == null) {
            return AjaxResult.notFound("队伍不存在");
        }
        int maxSize = plugin().getTeamManager().getMaxSize(team);
        List<TeamBuff> buffs = plugin().getTeamBuffManager().getBuffs(team.getId());
        return AjaxResult.success(TeamWebDtos.teamDetail(team, maxSize, buffs));
    }

    // ================================================================
    //  队伍增幅
    // ================================================================

    @ApiName("增幅列表")
    @GetMapping("/admin/buffs")
    public AjaxResult buffs(@RequestParam(name = "teamId") String teamId) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (TeamBuff buff : plugin().getTeamBuffManager().getBuffs(parseId(teamId))) {
            list.add(TeamWebDtos.buff(buff));
        }
        return AjaxResult.success(list);
    }

    @ApiName("添加增幅")
    @PostMapping("/admin/buff/add")
    public AjaxResult buffAdd(@RequestBody String body, ApiRequestContext ctx) {
        Map<String, Object> req = JsonReader.parseObject(body);
        UUID teamId = parseId(str(req, "teamId"));
        if (teamId == null) {
            return AjaxResult.error("缺少队伍 ID");
        }
        BuffKind kind = BuffKind.fromId(str(req, "kind"));
        if (kind == null) {
            kind = BuffKind.POTION;
        }
        String effectKey = str(req, "effectKey");
        if (effectKey == null || effectKey.isEmpty()) {
            return AjaxResult.error("缺少效果类型");
        }
        int amplifier = intVal(req, "amplifier", 0);
        long duration = longVal(req, "duration", plugin().getConfigManager().getBuffDefaultDuration());

        int maxDuration = plugin().getConfigManager().getBuffMaxDuration();
        if (maxDuration > 0 && duration > maxDuration) {
            return AjaxResult.error("持续时间超过上限（" + maxDuration + " 秒）");
        }

        String displayName = str(req, "displayName");
        TeamBuff buff = plugin().getTeamBuffManager()
                .addBuff(teamId, kind, effectKey, displayName, amplifier, duration, ctx.getPlayerName());
        if (buff == null) {
            return AjaxResult.error("添加失败：药水效果类型不存在");
        }
        integration.getLogManager().info(ctx.getPlayerName(), "队伍增幅",
                "为 " + teamName(teamId) + " 添加 " + TeamWebDtos.potionDisplayName(effectKey)
                        + " " + TeamWebDtos.romanLevel(buff.getLevel())
                        + "，持续 " + TeamWebDtos.formatDuration(duration), ctx.getIp());
        return AjaxResult.success("增幅已添加", TeamWebDtos.buff(buff));
    }

    @ApiName("移除增幅")
    @PostMapping("/admin/buff/remove")
    public AjaxResult buffRemove(@RequestBody String body, ApiRequestContext ctx) {
        Map<String, Object> req = JsonReader.parseObject(body);
        UUID teamId = parseId(str(req, "teamId"));
        String buffId = str(req, "buffId");
        boolean ok = plugin().getTeamBuffManager().removeBuff(teamId, buffId);
        if (!ok) {
            return AjaxResult.error("增幅不存在或已过期");
        }
        integration.getLogManager().info(ctx.getPlayerName(), "队伍增幅",
                "移除 " + teamName(teamId) + " 的增幅 " + buffId, ctx.getIp());
        return AjaxResult.success("增幅已移除");
    }

    // ================================================================
    //  申请审批
    // ================================================================

    @ApiName("申请列表")
    @GetMapping("/admin/applications")
    public AjaxResult applications(@RequestParam(name = "teamId") String teamId) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (JoinApplication app : plugin().getApplicationManager().getApplications(parseId(teamId))) {
            list.add(TeamWebDtos.application(app));
        }
        return AjaxResult.success(list);
    }

    @ApiName("批准申请")
    @PostMapping("/admin/application/approve")
    public AjaxResult applicationApprove(@RequestBody String body, ApiRequestContext ctx) {
        Map<String, Object> req = JsonReader.parseObject(body);
        UUID teamId = parseId(str(req, "teamId"));
        UUID playerId = parseId(str(req, "playerId"));
        soys.soyslinkteam.application.ApplicationManager.ApproveResult result = plugin().getApplicationManager().approve(teamId, playerId);
        if (result != soys.soyslinkteam.application.ApplicationManager.ApproveResult.SUCCESS) {
            return AjaxResult.error(result.getMessage());
        }
        integration.getLogManager().info(ctx.getPlayerName(), "申请审批",
                "批准 " + str(req, "playerName") + " 加入 " + teamName(teamId), ctx.getIp());
        return AjaxResult.success(result.getMessage());
    }

    @ApiName("拒绝申请")
    @PostMapping("/admin/application/deny")
    public AjaxResult applicationDeny(@RequestBody String body, ApiRequestContext ctx) {
        Map<String, Object> req = JsonReader.parseObject(body);
        UUID teamId = parseId(str(req, "teamId"));
        UUID playerId = parseId(str(req, "playerId"));
        String reason = str(req, "reason");
        soys.soyslinkteam.application.ApplicationManager.ApproveResult result = plugin().getApplicationManager().deny(teamId, playerId, reason);
        if (result != soys.soyslinkteam.application.ApplicationManager.ApproveResult.SUCCESS) {
            return AjaxResult.error(result.getMessage());
        }
        integration.getLogManager().info(ctx.getPlayerName(), "申请审批",
                "拒绝 " + str(req, "playerName") + " 加入 " + teamName(teamId)
                        + (reason == null ? "" : "：" + reason), ctx.getIp());
        return AjaxResult.success("已拒绝");
    }

    // ================================================================
    //  更换队长 / 解散
    // ================================================================

    @ApiName("更换队长")
    @PostMapping("/admin/transfer")
    public AjaxResult transfer(@RequestBody String body, ApiRequestContext ctx) {
        Map<String, Object> req = JsonReader.parseObject(body);
        Team team = loadTeam(str(req, "teamId"));
        UUID newLeaderId = parseId(str(req, "playerId"));
        if (team == null || newLeaderId == null) {
            return AjaxResult.error("参数不完整");
        }
        if (team.getMember(newLeaderId) == null) {
            return AjaxResult.error("该玩家不在队伍中");
        }
        String oldLeader = team.getLeaderName();
        boolean ok = team.transferLeader(newLeaderId);
        if (!ok) {
            return AjaxResult.error("更换队长失败");
        }
        plugin().getTeamManager().save(team);
        integration.getLogManager().info(ctx.getPlayerName(), "更换队长",
                team.getName() + " 队长由 " + oldLeader + " 变更为 " + team.getLeaderName(), ctx.getIp());
        return AjaxResult.success("队长已更换");
    }

    @ApiName("解散队伍")
    @PostMapping("/admin/disband")
    public AjaxResult disband(@RequestBody String body, ApiRequestContext ctx) {
        Map<String, Object> req = JsonReader.parseObject(body);
        Team team = loadTeam(str(req, "teamId"));
        if (team == null) {
            return AjaxResult.error("队伍不存在");
        }
        String name = team.getName();
        plugin().getTeamBuffManager().clearTeam(team.getId());
        plugin().getApplicationManager().removeByTeam(team.getId());
        boolean ok = plugin().getTeamManager().disbandTeam(team, ctx.getSyncPlayer());
        if (!ok) {
            return AjaxResult.error("解散被取消（可能被其他插件拦截）");
        }
        integration.getLogManager().warn(ctx.getPlayerName(), "解散队伍", "解散了队伍 " + name, ctx.getIp());
        return AjaxResult.success("队伍已解散");
    }

    // ================================================================
    //  队伍配置
    // ================================================================

    @ApiName("更新队伍配置")
    @PostMapping("/admin/config")
    public AjaxResult config(@RequestBody String body, ApiRequestContext ctx) {
        Map<String, Object> req = JsonReader.parseObject(body);
        Team team = loadTeam(str(req, "teamId"));
        if (team == null) {
            return AjaxResult.error("队伍不存在");
        }
        List<String> changed = new ArrayList<>();

        // 队伍名
        if (req.containsKey("name")) {
            String newName = str(req, "name");
            if (newName != null && !newName.equals(team.getName())) {
                soys.soyslinkteam.util.NameValidator.Result result = plugin().getNameValidator().validateTeamName(newName, team.getId());
                if (!result.isValid()) {
                    return AjaxResult.error("队伍名不合法");
                }
                String oldName = team.getName();
                plugin().getTeamManager().renameTeam(team, newName);
                changed.add("名称：" + oldName + " → " + newName);
            }
        }

        // 是否无条件加入（公开）
        if (req.containsKey("open")) {
            boolean open = Boolean.TRUE.equals(req.get("open"));
            team.getSettings().setOpen(open);
            changed.add("无条件加入：" + (open ? "开启" : "关闭"));
        }

        // 口令
        if (req.containsKey("password")) {
            String password = str(req, "password");
            if (password == null || password.isEmpty() || password.equalsIgnoreCase("off")) {
                team.getSettings().setPassword(null);
                changed.add("口令：已清除");
            } else {
                boolean cs = plugin().getConfigManager().isPasswordCaseSensitive();
                team.getSettings().setPassword(PasswordUtil.hash(cs ? password : password.toLowerCase()));
                changed.add("口令：已更新");
            }
        }

        // 入队费用
        if (req.containsKey("cost")) {
            Object costVal = req.get("cost");
            if (costVal == null || String.valueOf(costVal).equalsIgnoreCase("off")) {
                team.getSettings().setEconomyCost(0);
                changed.add("入队费用：已清除");
            } else {
                double cost = Double.parseDouble(String.valueOf(costVal));
                double maxCost = plugin().getConfigManager().getEconomyMaxCost();
                if (maxCost > 0 && cost > maxCost) {
                    return AjaxResult.error("费用超过上限");
                }
                team.getSettings().setEconomyCost(cost);
                changed.add("入队费用：" + cost);
            }
        }

        // 公告
        if (req.containsKey("notice")) {
            String notice = str(req, "notice");
            team.getSettings().setNotice(notice == null ? "" : notice);
            changed.add("公告已更新");
        }

        plugin().getTeamManager().save(team);
        integration.getLogManager().info(ctx.getPlayerName(), "队伍配置",
                team.getName() + "：" + String.join("；", changed), ctx.getIp());
        return AjaxResult.success("配置已保存", String.join("；", changed));
    }

    // ================================================================
    //  系统设置
    // ================================================================

    @ApiName("系统设置")
    @GetMapping("/admin/settings")
    public AjaxResult settings() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("language", plugin().getConfigManager().getLanguage());
        data.put("debug", plugin().getConfigManager().isDebug());
        data.put("defaultMaxSize", plugin().getConfigManager().getDefaultMaxSize());
        data.put("hardMaxSize", plugin().getConfigManager().getHardMaxSize());
        data.put("mirrorEnabled", plugin().getConfigManager().isMirrorEnabled());
        data.put("mirrorAsync", plugin().getConfigManager().isMirrorAsync());
        data.put("teamChatEnabled", plugin().getConfigManager().isTeamChatEnabled());
        data.put("nametagEnabled", plugin().getConfigManager().isNametagEnabled());
        return AjaxResult.success(data);
    }

    // ================================================================
    //  操作日志
    // ================================================================

    @ApiName("操作日志")
    @GetMapping("/admin/logs")
    public AjaxResult logs(@RequestParam(name = "level", required = false) String level,
                           @RequestParam(name = "keyword", required = false) String keyword,
                           @RequestParam(name = "page", required = false, defaultValue = "0") int page,
                           @RequestParam(name = "pageSize", required = false, defaultValue = "20") int pageSize) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (WebLogManager.LogEntry entry : integration.getLogManager().query(level, keyword, page, pageSize)) {
            list.add(TeamWebDtos.log(entry));
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", list);
        data.put("total", integration.getLogManager().count(level, keyword));
        data.put("page", page);
        data.put("pageSize", pageSize);
        return AjaxResult.success(data);
    }

    // ================================================================
    //  增幅可选项（药水效果 + 属性键）
    // ================================================================

    @ApiName("增幅可选项")
    @GetMapping("/admin/buff-options")
    public AjaxResult buffOptions() {
        Map<String, Object> data = new LinkedHashMap<>();

        // 药水效果（信标常用 + 全部）
        List<Map<String, Object>> potions = new ArrayList<>();
        for (PotionEffectType type : PotionEffectType.values()) {
            if (type == null) {
                continue;
            }
            Map<String, Object> option = new LinkedHashMap<>();
            option.put("key", type.getName());
            option.put("name", TeamWebDtos.potionDisplayName(type.getName()));
            potions.add(option);
        }
        data.put("potions", potions);

        // 属性键（从 buff.yml attributes 配置读取）
        List<Map<String, Object>> attributes = new ArrayList<>();
        ConfigurationSection root = plugin().getConfigManager().getBuffSection();
        ConfigurationSection attrs = root == null ? null : root.getConfigurationSection("attributes");
        if (attrs != null) {
            for (String key : attrs.getKeys(false)) {
                Map<String, Object> option = new LinkedHashMap<>();
                option.put("key", key);
                option.put("name", attrs.getConfigurationSection(key).getString("display-name", key));
                attributes.add(option);
            }
        }
        data.put("attributes", attributes);

        // 支持的属性插件提示
        data.put("attributePlugins", Arrays("BigAttribute", "AttributePlus", "Attribute",
                "SX-Attribute", "ItemAttribute"));
        return AjaxResult.success(data);
    }

    // ================================================================
    //  辅助
    // ================================================================

    private Team loadTeam(String idText) {
        UUID id = parseId(idText);
        if (id == null) {
            return null;
        }
        Team loaded = plugin().getTeamManager().getLoadedTeam(id);
        if (loaded != null) {
            return loaded;
        }
        try {
            Team team = plugin().getStorageManager().loadTeam(id);
            if (team != null) {
                plugin().getTeamManager().activate(team);
            }
            return team;
        } catch (Exception e) {
            return null;
        }
    }

    private String teamName(UUID teamId) {
        Team team = plugin().getTeamManager().getLoadedTeam(teamId);
        if (team != null) {
            return team.getName();
        }
        Map<UUID, String> index = plugin().getTeamManager().getTeamNameIndex();
        return index.getOrDefault(teamId, String.valueOf(teamId));
    }

    private static UUID parseId(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(text.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private static int intVal(Map<String, Object> map, String key, int def) {
        Object v = map.get(key);
        return v instanceof Number ? ((Number) v).intValue() : def;
    }

    private static long longVal(Map<String, Object> map, String key, long def) {
        Object v = map.get(key);
        return v instanceof Number ? ((Number) v).longValue() : def;
    }

    private static List<String> Arrays(String... values) {
        List<String> list = new ArrayList<>();
        Collections.addAll(list, values);
        return list;
    }
}
