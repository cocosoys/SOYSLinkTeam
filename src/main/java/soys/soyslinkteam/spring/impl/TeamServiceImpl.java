package soys.soyslinkteam.spring.impl;

import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.util.PageUtils;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.web.ApiRequestContext;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.spring.entity.dto.DisbandDTO;
import soys.soyslinkteam.spring.entity.dto.TeamConfigDTO;
import soys.soyslinkteam.spring.entity.dto.TransferDTO;
import soys.soyslinkteam.spring.entity.vo.TeamDetailVO;
import soys.soyslinkteam.spring.entity.vo.TeamRowVO;
import soys.soyslinkteam.spring.service.TeamService;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.util.PasswordUtil;
import soys.soyslinkteam.web.WebIntegration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 队伍管理服务实现：列表 / 详情 / 更换队长 / 解散 / 队伍配置。
 *
 * <p>业务校验、持久化与操作日志集中于此，controller 仅做参数转发。</p>
 */
public class TeamServiceImpl implements TeamService {

    private final SOYSLinkTeam plugin;
    private final WebIntegration integration;

    public TeamServiceImpl(SOYSLinkTeam plugin, WebIntegration integration) {
        this.plugin = plugin;
        this.integration = integration;
    }

    @Override
    public TableDataInfo list(String keyword, Integer pageNum, Integer pageSize) {
        String kw = keyword == null ? null : keyword.trim().toLowerCase();
        List<TeamRowVO> list = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : plugin.getTeamManager().getTeamNameIndex().entrySet()) {
            UUID id = entry.getKey();
            String name = entry.getValue();
            if (kw != null && !kw.isEmpty() && (name == null || !name.toLowerCase().contains(kw))) {
                continue;
            }
            Team loaded = plugin.getTeamManager().getLoadedTeam(id);
            int maxSize = plugin.getTeamManager().getMaxSize(loaded);
            int buffCount = plugin.getTeamBuffManager().getBuffs(id).size();
            int appCount = plugin.getApplicationManager().countApplications(id);
            if (loaded != null) {
                list.add(soys.soyslinkteam.spring.support.TeamVoAssembler.toRow(loaded, maxSize, buffCount, appCount));
            } else {
                TeamRowVO row = new TeamRowVO();
                row.setId(id.toString());
                row.setName(name);
                row.setSize(-1);
                row.setOnlineCount(-1);
                row.setMaxSize(maxSize);
                row.setBuffCount(buffCount);
                row.setApplicationCount(appCount);
                row.setLoaded(false);
                list.add(row);
            }
        }
        return PageUtils.page(list, pageNum, pageSize);
    }

    @Override
    public AjaxResult detail(String id) {
        Team team = loadTeam(id);
        if (team == null) {
            return AjaxResult.error("队伍不存在");
        }
        int maxSize = plugin.getTeamManager().getMaxSize(team);
        List<soys.soyslinkteam.buff.TeamBuff> buffs =
                new ArrayList<>(plugin.getTeamBuffManager().getBuffs(team.getId()));
        TeamDetailVO vo = soys.soyslinkteam.spring.support.TeamVoAssembler.toDetail(team, maxSize, buffs);
        return AjaxResult.success(vo);
    }

    @Override
    public AjaxResult transfer(TransferDTO dto, ApiRequestContext ctx) {
        Team team = loadTeam(dto.getTeamId());
        UUID newLeader = parseId(dto.getPlayerId());
        if (team == null || newLeader == null) {
            return AjaxResult.error("参数不完整");
        }
        if (team.getMember(newLeader) == null) {
            return AjaxResult.error("该玩家不在队伍中");
        }
        String oldLeader = team.getLeaderName();
        if (!team.transferLeader(newLeader)) {
            return AjaxResult.error("更换队长失败");
        }
        plugin.getTeamManager().save(team);
        integration.getLogManager().info(ctx.getPlayerName(), "更换队长",
                team.getName() + " 队长由 " + oldLeader + " 变更为 " + team.getLeaderName(), ctx.getIp());
        return AjaxResult.success("队长已更换");
    }

    @Override
    public AjaxResult disband(DisbandDTO dto, ApiRequestContext ctx) {
        Team team = loadTeam(dto.getTeamId());
        if (team == null) {
            return AjaxResult.error("队伍不存在");
        }
        String name = team.getName();
        plugin.getTeamBuffManager().clearTeam(team.getId());
        plugin.getApplicationManager().removeByTeam(team.getId());
        if (!plugin.getTeamManager().disbandTeam(team, ctx.getSyncPlayer())) {
            return AjaxResult.error("解散被取消（可能被其他插件拦截）");
        }
        integration.getLogManager().warn(ctx.getPlayerName(), "解散队伍", "解散了队伍 " + name, ctx.getIp());
        return AjaxResult.success("队伍已解散");
    }

    @Override
    public AjaxResult updateConfig(TeamConfigDTO dto, ApiRequestContext ctx) {
        Team team = loadTeam(dto.getTeamId());
        if (team == null) {
            return AjaxResult.error("队伍不存在");
        }
        List<String> changed = new ArrayList<>();

        if (dto.getName() != null && !dto.getName().equals(team.getName())) {
            soys.soyslinkteam.util.NameValidator.Result result =
                    plugin.getNameValidator().validateTeamName(dto.getName(), team.getId());
            if (!result.isValid()) {
                return AjaxResult.error("队伍名不合法");
            }
            String oldName = team.getName();
            plugin.getTeamManager().renameTeam(team, dto.getName());
            changed.add("名称：" + oldName + " → " + dto.getName());
        }

        if (dto.getOpen() != null) {
            team.getSettings().setOpen(dto.getOpen());
            changed.add("无条件加入：" + (dto.getOpen() ? "开启" : "关闭"));
        }

        if (dto.getPassword() != null) {
            String password = dto.getPassword();
            if (password.isEmpty() || password.equalsIgnoreCase("off")) {
                team.getSettings().setPassword(null);
                changed.add("口令：已清除");
            } else {
                boolean cs = plugin.getConfigManager().isPasswordCaseSensitive();
                team.getSettings().setPassword(PasswordUtil.hash(cs ? password : password.toLowerCase()));
                changed.add("口令：已更新");
            }
        }

        if (dto.getCost() != null) {
            String raw = String.valueOf(dto.getCost());
            if (raw.isEmpty() || raw.equalsIgnoreCase("off")) {
                team.getSettings().setEconomyCost(0);
                changed.add("入队费用：已清除");
            } else {
                double cost;
                try {
                    cost = Double.parseDouble(raw);
                } catch (NumberFormatException e) {
                    return AjaxResult.error("入队费用格式不正确");
                }
                double maxCost = plugin.getConfigManager().getEconomyMaxCost();
                if (maxCost > 0 && cost > maxCost) {
                    return AjaxResult.error("费用超过上限");
                }
                team.getSettings().setEconomyCost(cost);
                changed.add("入队费用：" + cost);
            }
        }

        if (dto.getNotice() != null) {
            team.getSettings().setNotice(dto.getNotice());
            changed.add("公告已更新");
        }

        plugin.getTeamManager().save(team);
        integration.getLogManager().info(ctx.getPlayerName(), "队伍配置",
                team.getName() + "：" + String.join("；", changed), ctx.getIp());
        return AjaxResult.success("配置已保存", String.join("；", changed));
    }

    // ===== 内部 =====

    private Team loadTeam(String idText) {
        UUID id = parseId(idText);
        if (id == null) {
            return null;
        }
        Team loaded = plugin.getTeamManager().getLoadedTeam(id);
        if (loaded != null) {
            return loaded;
        }
        try {
            Team team = plugin.getStorageManager().loadTeam(id);
            if (team != null) {
                plugin.getTeamManager().activate(team);
            }
            return team;
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
}
