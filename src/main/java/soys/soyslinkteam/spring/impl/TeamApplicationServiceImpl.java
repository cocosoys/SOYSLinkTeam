package soys.soyslinkteam.spring.impl;

import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.web.ApiRequestContext;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.application.ApplicationManager;
import soys.soyslinkteam.application.JoinApplication;
import soys.soyslinkteam.spring.entity.dto.ApproveDTO;
import soys.soyslinkteam.spring.entity.dto.DenyDTO;
import soys.soyslinkteam.spring.entity.vo.ApplicationVO;
import soys.soyslinkteam.spring.service.TeamApplicationService;
import soys.soyslinkteam.spring.support.TeamVoAssembler;
import soys.soyslinkteam.web.WebIntegration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 入队申请审批服务实现。
 */
public class TeamApplicationServiceImpl implements TeamApplicationService {

    private final SOYSLinkTeam plugin;
    private final WebIntegration integration;

    public TeamApplicationServiceImpl(SOYSLinkTeam plugin, WebIntegration integration) {
        this.plugin = plugin;
        this.integration = integration;
    }

    @Override
    public List<ApplicationVO> list(String teamId) {
        UUID id = parseId(teamId);
        List<ApplicationVO> out = new ArrayList<>();
        if (id == null) {
            return out;
        }
        for (JoinApplication app : plugin.getApplicationManager().getApplications(id)) {
            out.add(TeamVoAssembler.toApplication(app));
        }
        return out;
    }

    @Override
    public AjaxResult approve(ApproveDTO dto, ApiRequestContext ctx) {
        UUID teamId = parseId(dto.getTeamId());
        UUID playerId = parseId(dto.getPlayerId());
        if (teamId == null || playerId == null) {
            return AjaxResult.error("参数不完整");
        }
        ApplicationManager.ApproveResult result =
                plugin.getApplicationManager().approve(teamId, playerId);
        if (result != ApplicationManager.ApproveResult.SUCCESS) {
            return AjaxResult.error(result.getMessage());
        }
        integration.getLogManager().info(ctx.getPlayerName(), "申请审批",
                "批准 " + dto.getPlayerName() + " 加入队伍", ctx.getIp());
        return AjaxResult.success(result.getMessage());
    }

    @Override
    public AjaxResult deny(DenyDTO dto, ApiRequestContext ctx) {
        UUID teamId = parseId(dto.getTeamId());
        UUID playerId = parseId(dto.getPlayerId());
        if (teamId == null || playerId == null) {
            return AjaxResult.error("参数不完整");
        }
        ApplicationManager.ApproveResult result =
                plugin.getApplicationManager().deny(teamId, playerId, dto.getReason());
        if (result != ApplicationManager.ApproveResult.SUCCESS) {
            return AjaxResult.error(result.getMessage());
        }
        String reason = dto.getReason() == null ? "" : "：" + dto.getReason();
        integration.getLogManager().info(ctx.getPlayerName(), "申请审批",
                "拒绝申请" + reason, ctx.getIp());
        return AjaxResult.success("已拒绝");
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
