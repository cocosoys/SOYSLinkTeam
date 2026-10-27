package soys.soyslinkteam.spring.impl;

import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.spring.entity.vo.OverviewVO;
import soys.soyslinkteam.spring.service.TeamOverviewService;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.web.WebIntegration;

/**
 * 概览服务实现。
 */
public class TeamOverviewServiceImpl implements TeamOverviewService {

    private final SOYSLinkTeam plugin;
    private final WebIntegration integration;

    public TeamOverviewServiceImpl(SOYSLinkTeam plugin, WebIntegration integration) {
        this.plugin = plugin;
        this.integration = integration;
    }

    @Override
    public OverviewVO overview() {
        OverviewVO v = new OverviewVO();
        v.setTotalTeams(plugin.getTeamManager().getTotalCount());
        v.setLoadedTeams(plugin.getTeamManager().getLoadedCount());
        v.setOnlinePlayers(plugin.getServer().getOnlinePlayers().size());

        int pending = 0;
        int buffs = 0;
        for (Team team : plugin.getTeamManager().getLoadedTeams()) {
            pending += plugin.getApplicationManager().countApplications(team.getId());
            buffs += plugin.getTeamBuffManager().getBuffs(team.getId()).size();
        }
        v.setPendingApplications(pending);
        v.setActiveBuffs(buffs);
        v.setLogEntries(integration.getLogManager() == null ? 0 : integration.getLogManager().size());
        return v;
    }
}
