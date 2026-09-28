package soys.soyslinkteam.web;

import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiName;
import com.github.cocosoys.mc.soyshttpovermc.annotations.GetMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PostMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestBody;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestParam;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.util.JsonReader;
import com.github.cocosoys.mc.soyshttpovermc.web.ApiRequestContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import soys.soyslinkteam.application.JoinApplication;
import soys.soyslinkteam.buff.TeamBuff;
import soys.soyslinkteam.join.JoinContext;
import soys.soyslinkteam.join.JoinResult;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.team.TeamMember;
import soys.soyslinkteam.util.CooldownManager;
import soys.soyslinkteam.util.Text;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/**
 * 用户侧 REST API（供 TIM 风格聊天窗口调用）。
 *
 * <p>覆盖：我的信息、我的队伍、浏览队伍、申请 / 加入 / 离开、队伍聊天。
 * 仅需登录（框架 401 底线），所有 Bukkit 写操作切回主线程执行。</p>
 */
@ApiName("队伍用户端")
public class TeamUserApi {

    private final WebIntegration integration;

    public TeamUserApi(WebIntegration integration) {
        this.integration = integration;
    }

    private soys.soyslinkteam.SOYSLinkTeam plugin() {
        return integration.getPlugin();
    }

    // ================================================================
    //  我 / 我的队伍
    // ================================================================

    @ApiName("我的信息")
    @GetMapping("/user/me")
    public AjaxResult me(ApiRequestContext ctx) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", ctx.getPlayerName());
        Player player = ctx.getSyncPlayer();
        data.put("online", player != null);
        UUID teamId = plugin().getTeamManager().getPlayerTeamId(
                player != null ? player.getUniqueId() : lookupByName(ctx.getPlayerName()));
        data.put("hasTeam", teamId != null);
        if (teamId != null) {
            data.put("teamId", teamId.toString());
            data.put("teamName", plugin().getTeamManager().getTeamNameIndex().get(teamId));
        }
        return AjaxResult.success(data);
    }

    @ApiName("我的队伍")
    @GetMapping("/user/my-team")
    public AjaxResult myTeam(ApiRequestContext ctx) {
        Player player = requirePlayer(ctx);
        if (player == null) {
            return AjaxResult.error("你当前不在线");
        }
        Team team = plugin().getTeamManager().getLoadedPlayerTeam(player.getUniqueId());
        if (team == null) {
            UUID id = plugin().getTeamManager().getPlayerTeamId(player.getUniqueId());
            if (id != null) {
                team = loadAndActivate(id);
            }
        }
        if (team == null) {
            return AjaxResult.success(null);
        }
        int maxSize = plugin().getTeamManager().getMaxSize(team);
        List<TeamBuff> buffs = plugin().getTeamBuffManager().getBuffs(team.getId());
        Map<String, Object> detail = TeamWebDtos.teamDetail(team, maxSize, buffs);
        detail.put("myRole", team.getMember(player.getUniqueId()) == null ? null
                : team.getMember(player.getUniqueId()).getRole().name());
        detail.put("chatChannel", plugin().getTeamChannelManager().isTeamChat(player.getUniqueId()));
        return AjaxResult.success(detail);
    }

    // ================================================================
    //  浏览队伍
    // ================================================================

    @ApiName("浏览队伍")
    @GetMapping("/user/browse")
    public AjaxResult browse(@RequestParam(name = "keyword", required = false) String keyword) {
        List<Map<String, Object>> list = new ArrayList<>();
        String kw = keyword == null ? null : keyword.trim().toLowerCase();

        for (Map.Entry<UUID, String> entry : plugin().getTeamManager().getTeamNameIndex().entrySet()) {
            UUID id = entry.getKey();
            String name = entry.getValue();
            if (kw != null && !kw.isEmpty() && (name == null || !name.toLowerCase().contains(kw))) {
                continue;
            }
            Team loaded = plugin().getTeamManager().getLoadedTeam(id);
            if (loaded != null) {
                Map<String, Object> summary = TeamWebDtos.teamSummary(loaded,
                        plugin().getTeamManager().getMaxSize(loaded),
                        plugin().getTeamBuffManager().getBuffs(id).size(),
                        plugin().getApplicationManager().countApplications(id));
                summary.put("joinWay", joinWay(loaded));
                list.add(summary);
            } else {
                Map<String, Object> placeholder = new LinkedHashMap<>();
                placeholder.put("id", id.toString());
                placeholder.put("name", name);
                placeholder.put("loaded", false);
                placeholder.put("joinWay", "点击查看");
                list.add(placeholder);
            }
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", list);
        data.put("total", list.size());
        return AjaxResult.success(data);
    }

    private String joinWay(Team team) {
        if (team.getSettings().isOpen()) {
            if (team.getSettings().hasEconomyCost()) {
                return "付费加入 " + team.getSettings().getEconomyCost();
            }
            return "无条件加入";
        }
        if (team.getSettings().hasPassword()) {
            return "口令加入";
        }
        return "需申请 / 邀请";
    }

    // ================================================================
    //  加入
    // ================================================================

    @ApiName("加入队伍")
    @PostMapping("/user/join")
    public AjaxResult join(@RequestBody String body, ApiRequestContext ctx) {
        Player player = requirePlayer(ctx);
        if (player == null) {
            return AjaxResult.error("你当前不在线，无法加入队伍");
        }
        Map<String, Object> req = JsonReader.parseObject(body);
        Team team = resolveTeam(req);
        if (team == null) {
            return AjaxResult.error("队伍不存在");
        }
        String password = str(req, "password");
        JoinResult result = sync(() -> {
            JoinContext context = JoinContext.of(player, team, password, "team");
            return plugin().getJoinMethodRegistry().tryJoin(context);
        });
        if (result == null) {
            return AjaxResult.error("加入超时");
        }
        if (result.isSuccess()) {
            return AjaxResult.success("已加入 " + team.getName());
        }
        String text = result.getMessageKey() == null ? "无法加入该队伍"
                : plugin().getMessageManager().get(result.getMessageKey(), result.getPlaceholders());
        return AjaxResult.error(Text.strip(text));
    }

    // ================================================================
    //  申请
    // ================================================================

    @ApiName("提交申请")
    @PostMapping("/user/apply")
    public AjaxResult apply(@RequestBody String body, ApiRequestContext ctx) {
        Player player = requirePlayer(ctx);
        if (player == null) {
            return AjaxResult.error("你当前不在线，无法提交申请");
        }
        Map<String, Object> req = JsonReader.parseObject(body);
        Team team = resolveTeam(req);
        if (team == null) {
            return AjaxResult.error("队伍不存在");
        }
        if (plugin().getTeamManager().hasTeam(player.getUniqueId())) {
            return AjaxResult.error("你已经加入了队伍");
        }
        long expireMillis = plugin().getConfigManager().getApplicationExpireSeconds() * 1000L;
        JoinApplication app = plugin().getApplicationManager()
                .submit(team, player, str(req, "message"), expireMillis);
        if (app == null) {
            return AjaxResult.error("提交失败：可能已提交过申请");
        }
        return AjaxResult.success("申请已提交，等待队长审批", TeamWebDtos.application(app));
    }

    @ApiName("我的申请")
    @GetMapping("/user/my-applications")
    public AjaxResult myApplications(ApiRequestContext ctx) {
        Player player = requirePlayer(ctx);
        UUID id = player != null ? player.getUniqueId() : lookupByName(ctx.getPlayerName());
        List<Map<String, Object>> list = new ArrayList<>();
        if (id != null) {
            for (JoinApplication app : plugin().getApplicationManager().getMyApplications(id)) {
                list.add(TeamWebDtos.application(app));
            }
        }
        return AjaxResult.success(list);
    }

    @ApiName("取消申请")
    @PostMapping("/user/application/cancel")
    public AjaxResult cancelApplication(@RequestBody String body, ApiRequestContext ctx) {
        Player player = requirePlayer(ctx);
        if (player == null) {
            return AjaxResult.error("你当前不在线");
        }
        Map<String, Object> req = JsonReader.parseObject(body);
        UUID teamId = parseId(str(req, "teamId"));
        boolean ok = plugin().getApplicationManager().cancel(teamId, player.getUniqueId());
        return ok ? AjaxResult.success("申请已取消") : AjaxResult.error("申请不存在");
    }

    // ================================================================
    //  队伍公开详情（浏览 / 加入前查看）
    // ================================================================

    @ApiName("队伍公开详情")
    @GetMapping("/user/team-profile")
    public AjaxResult teamProfile(@RequestParam(name = "id", required = false) String idText,
                                  @RequestParam(name = "name", required = false) String nameText) {
        Team team = null;
        UUID id = parseId(idText);
        if (id != null) {
            team = loadAndActivate(id);
        } else if (nameText != null && !nameText.trim().isEmpty()) {
            UUID byName = plugin().getTeamManager().getTeamIdByName(nameText.trim());
            if (byName != null) {
                team = loadAndActivate(byName);
            }
        }
        if (team == null) {
            return AjaxResult.notFound("队伍不存在");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", team.getId().toString());
        data.put("name", team.getName());
        data.put("leaderName", team.getLeaderName());
        data.put("size", team.getSize());
        data.put("onlineCount", team.getOnlineCount());
        data.put("maxSize", plugin().getTeamManager().getMaxSize(team));
        data.put("open", team.getSettings().isOpen());
        data.put("hasPassword", team.getSettings().hasPassword());
        data.put("hasCost", team.getSettings().hasEconomyCost());
        data.put("cost", team.getSettings().getEconomyCost());
        data.put("notice", team.getSettings().getNotice());
        data.put("tag", team.getSettings().getTag());
        data.put("createdAtText", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm")
                .format(new java.util.Date(team.getCreatedAt())));

        List<Map<String, Object>> members = new ArrayList<>();
        for (TeamMember m : team.getSortedMembers()) {
            Map<String, Object> mm = new LinkedHashMap<>();
            mm.put("name", m.getName());
            mm.put("role", m.getRole().name());
            mm.put("online", m.isOnline());
            members.add(mm);
        }
        data.put("members", members);

        List<Map<String, Object>> buffs = new ArrayList<>();
        for (TeamBuff b : plugin().getTeamBuffManager().getBuffs(team.getId())) {
            buffs.add(TeamWebDtos.buff(b));
        }
        data.put("buffs", buffs);
        return AjaxResult.success(data);
    }

    @ApiName("本队申请列表")
    @GetMapping("/user/team-applications")
    public AjaxResult teamApplications(@RequestParam(name = "teamId") String teamIdText, ApiRequestContext ctx) {
        Player player = requirePlayer(ctx);
        if (player == null) {
            return AjaxResult.error("你当前不在线");
        }
        UUID teamId = parseId(teamIdText);
        Team team = teamId == null ? null : loadAndActivate(teamId);
        if (team == null) {
            return AjaxResult.error("队伍不存在");
        }
        TeamMember self = team.getMember(player.getUniqueId());
        if (self == null || (self.getRole() != soys.soyslinkteam.team.TeamRole.LEADER
                && self.getRole() != soys.soyslinkteam.team.TeamRole.ADMIN)) {
            return AjaxResult.forbidden("仅队长 / 副队长可查看申请");
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (JoinApplication app : plugin().getApplicationManager().getApplications(team.getId())) {
            list.add(TeamWebDtos.application(app));
        }
        return AjaxResult.success(list);
    }

    @ApiName("队长审批申请")
    @PostMapping("/user/application/handle")
    public AjaxResult handleApplication(@RequestBody String body, ApiRequestContext ctx) {
        Player player = requirePlayer(ctx);
        if (player == null) {
            return AjaxResult.error("你当前不在线");
        }
        Map<String, Object> req = JsonReader.parseObject(body);
        UUID teamId = parseId(str(req, "teamId"));
        UUID applicantId = parseId(str(req, "playerId"));
        if (teamId == null || applicantId == null) {
            return AjaxResult.error("参数不完整");
        }
        Team team = loadAndActivate(teamId);
        if (team == null) {
            return AjaxResult.error("队伍不存在");
        }
        TeamMember self = team.getMember(player.getUniqueId());
        if (self == null || (self.getRole() != soys.soyslinkteam.team.TeamRole.LEADER
                && self.getRole() != soys.soyslinkteam.team.TeamRole.ADMIN)) {
            return AjaxResult.forbidden("仅队长 / 副队长可审批申请");
        }
        boolean approve = Boolean.TRUE.equals(req.get("approve"));
        if (approve) {
            soys.soyslinkteam.application.ApplicationManager.ApproveResult result = plugin().getApplicationManager().approve(teamId, applicantId);
            return result == soys.soyslinkteam.application.ApplicationManager.ApproveResult.SUCCESS
                    ? AjaxResult.success(result.getMessage())
                    : AjaxResult.error(result.getMessage());
        }
        String reason = str(req, "reason");
        soys.soyslinkteam.application.ApplicationManager.ApproveResult result = plugin().getApplicationManager().deny(teamId, applicantId, reason);
        return result == soys.soyslinkteam.application.ApplicationManager.ApproveResult.SUCCESS
                ? AjaxResult.success("已拒绝该申请")
                : AjaxResult.error(result.getMessage());
    }

    // ================================================================
    //  离开
    // ================================================================

    @ApiName("离开队伍")
    @PostMapping("/user/leave")
    public AjaxResult leave(ApiRequestContext ctx) {
        Player player = requirePlayer(ctx);
        if (player == null) {
            return AjaxResult.error("你当前不在线");
        }
        Team team = plugin().getTeamManager().getLoadedPlayerTeam(player.getUniqueId());
        if (team == null) {
            UUID id = plugin().getTeamManager().getPlayerTeamId(player.getUniqueId());
            if (id != null) {
                team = loadAndActivate(id);
            }
        }
        if (team == null) {
            return AjaxResult.error("你当前没有队伍");
        }

        final Team target = team;
        String outcome = sync(() -> doLeave(target, player));
        if (outcome == null) {
            return AjaxResult.error("操作失败");
        }
        if (outcome.startsWith("ERR:")) {
            return AjaxResult.error(outcome.substring(4));
        }
        return AjaxResult.success(outcome);
    }

    private String doLeave(Team team, Player player) {
        boolean isLeader = team.isLeader(player.getUniqueId());
        if (isLeader) {
            String action = plugin().getConfigManager().getLeaderQuitAction();
            if ("DENY".equals(action)) {
                return "ERR:队长不能直接退出，请先转让队长或解散队伍";
            }
            if ("DISBAND".equals(action)) {
                plugin().getTeamBuffManager().clearTeam(team.getId());
                plugin().getApplicationManager().removeByTeam(team.getId());
                plugin().getTeamManager().disbandTeam(team, player);
                applyRejoinCooldown(player);
                return "队伍已解散";
            }
            // TRANSFER：转让继任者，无人则解散
            TeamMember successor = team.pickSuccessor(player.getUniqueId());
            if (successor == null) {
                plugin().getTeamBuffManager().clearTeam(team.getId());
                plugin().getApplicationManager().removeByTeam(team.getId());
                plugin().getTeamManager().disbandTeam(team, player);
                applyRejoinCooldown(player);
                return "队伍已解散";
            }
            plugin().getTeamManager().removeMember(team, player.getUniqueId());
            team.transferLeader(successor.getUuid());
            plugin().getTeamManager().save(team);
            applyRejoinCooldown(player);
            return "已离开，队长转让给 " + successor.getName();
        }
        plugin().getTeamManager().removeMember(team, player.getUniqueId());
        applyRejoinCooldown(player);
        return "已离开队伍";
    }

    private void applyRejoinCooldown(Player player) {
        if (plugin().getPermissionManager().canBypass(player)) {
            return;
        }
        plugin().getCooldownManager().set(CooldownManager.REJOIN, player.getUniqueId(),
                plugin().getConfigManager().getRejoinCooldown());
    }

    // ================================================================
    //  队伍聊天
    // ================================================================

    @ApiName("发送队伍聊天")
    @PostMapping("/user/chat")
    public AjaxResult chat(@RequestBody String body, ApiRequestContext ctx) {
        Player player = requirePlayer(ctx);
        if (player == null) {
            return AjaxResult.error("你当前不在线");
        }
        Map<String, Object> req = JsonReader.parseObject(body);
        String message = str(req, "message");
        if (message == null || message.trim().isEmpty()) {
            return AjaxResult.error("消息不能为空");
        }
        Team team = plugin().getTeamManager().getLoadedPlayerTeam(player.getUniqueId());
        if (team == null) {
            return AjaxResult.error("你当前没有队伍");
        }

        String raw = plugin().getConfigManager().getTeamChatFormat();
        String colored = Text.color(raw
                .replace("{player}", player.getDisplayName())
                .replace("{message}", message));
        for (Player member : team.getOnlinePlayers()) {
            member.sendMessage(colored);
        }
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.hasPermission("soyslinkteam.chat.spy")
                    && !team.hasMember(viewer.getUniqueId())) {
                viewer.sendMessage(Text.color("&8[spy] "
                        + raw.replace("{player}", player.getName()).replace("{message}", message)));
            }
        }

        Map<String, Object> echo = new LinkedHashMap<>();
        echo.put("from", player.getName());
        echo.put("role", team.getMember(player.getUniqueId()).getRole().name());
        echo.put("text", message);
        echo.put("time", System.currentTimeMillis());
        echo.put("mine", true);
        return AjaxResult.success(echo);
    }

    @ApiName("切换队伍频道")
    @PostMapping("/user/chat/toggle")
    public AjaxResult toggleChat(ApiRequestContext ctx) {
        Player player = requirePlayer(ctx);
        if (player == null) {
            return AjaxResult.error("你当前不在线");
        }
        boolean on = plugin().getTeamChannelManager().toggleTeamChat(player.getUniqueId());
        return AjaxResult.success(on ? "已进入队伍频道" : "已退出队伍频道");
    }

    // ================================================================
    //  辅助
    // ================================================================

    private Team resolveTeam(Map<String, Object> req) {
        UUID teamId = parseId(str(req, "teamId"));
        if (teamId != null) {
            return loadAndActivate(teamId);
        }
        String name = str(req, "name");
        if (name != null && !name.isEmpty()) {
            UUID byName = plugin().getTeamManager().getTeamIdByName(name);
            if (byName != null) {
                return loadAndActivate(byName);
            }
        }
        return null;
    }

    private Team loadAndActivate(UUID id) {
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

    private Player requirePlayer(ApiRequestContext ctx) {
        return ctx.getSyncPlayer();
    }

    private UUID lookupByName(String name) {
        if (name == null) {
            return null;
        }
        Player p = Bukkit.getPlayerExact(name);
        return p == null ? null : p.getUniqueId();
    }

    private <T> T sync(Callable<T> task) {
        if (Bukkit.isPrimaryThread()) {
            try {
                return task.call();
            } catch (Exception e) {
                return null;
            }
        }
        FutureTask<T> ft = new FutureTask<>(task);
        plugin().getServer().getScheduler().runTask(plugin(), ft);
        try {
            return ft.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            return null;
        }
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
}
