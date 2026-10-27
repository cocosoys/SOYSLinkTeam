package soys.soyslinkteam.spring.impl;

import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.web.ApiRequestContext;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffectType;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.buff.BuffKind;
import soys.soyslinkteam.buff.TeamBuff;
import soys.soyslinkteam.spring.entity.dto.BuffAddDTO;
import soys.soyslinkteam.spring.entity.dto.BuffRemoveDTO;
import soys.soyslinkteam.spring.entity.vo.BuffOptionsVO;
import soys.soyslinkteam.spring.entity.vo.BuffVO;
import soys.soyslinkteam.spring.service.TeamBuffService;
import soys.soyslinkteam.spring.support.TeamVoAssembler;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.web.WebIntegration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 队伍增幅服务实现。
 */
public class TeamBuffServiceImpl implements TeamBuffService {

    private final SOYSLinkTeam plugin;
    private final WebIntegration integration;

    public TeamBuffServiceImpl(SOYSLinkTeam plugin, WebIntegration integration) {
        this.plugin = plugin;
        this.integration = integration;
    }

    @Override
    public List<BuffVO> list(String teamId) {
        UUID id = parseId(teamId);
        List<BuffVO> out = new ArrayList<>();
        if (id == null) {
            return out;
        }
        for (TeamBuff b : plugin.getTeamBuffManager().getBuffs(id)) {
            out.add(TeamVoAssembler.toBuff(b));
        }
        return out;
    }

    @Override
    public AjaxResult add(BuffAddDTO dto, ApiRequestContext ctx) {
        UUID teamId = parseId(dto.getTeamId());
        if (teamId == null) {
            return AjaxResult.error("缺少队伍 ID");
        }
        BuffKind kind = BuffKind.fromId(dto.getKind());
        if (kind == null) {
            kind = BuffKind.POTION;
        }
        String effectKey = dto.getEffectKey();
        if (effectKey == null || effectKey.isEmpty()) {
            return AjaxResult.error("缺少效果类型");
        }
        int amplifier = dto.getAmplifier();
        long duration = dto.getDuration() == null
                ? plugin.getConfigManager().getBuffDefaultDuration() : dto.getDuration();

        int maxDuration = plugin.getConfigManager().getBuffMaxDuration();
        if (maxDuration > 0 && duration > maxDuration) {
            return AjaxResult.error("持续时间超过上限（" + maxDuration + " 秒）");
        }

        TeamBuff buff = plugin.getTeamBuffManager().addBuff(teamId, kind, effectKey,
                dto.getDisplayName(), amplifier, duration, ctx.getPlayerName());
        if (buff == null) {
            return AjaxResult.error("添加失败：药水效果类型不存在");
        }
        integration.getLogManager().info(ctx.getPlayerName(), "队伍增幅",
                "为 " + teamName(teamId) + " 添加 " + TeamVoAssembler.potionDisplayName(effectKey)
                        + " " + TeamVoAssembler.romanLevel(buff.getLevel())
                        + "，持续 " + TeamVoAssembler.formatDuration(duration), ctx.getIp());
        return AjaxResult.success("增幅已添加", TeamVoAssembler.toBuff(buff));
    }

    @Override
    public AjaxResult remove(BuffRemoveDTO dto, ApiRequestContext ctx) {
        UUID teamId = parseId(dto.getTeamId());
        if (teamId == null || dto.getBuffId() == null || dto.getBuffId().isEmpty()) {
            return AjaxResult.error("参数不完整");
        }
        if (!plugin.getTeamBuffManager().removeBuff(teamId, dto.getBuffId())) {
            return AjaxResult.error("增幅不存在或已过期");
        }
        integration.getLogManager().info(ctx.getPlayerName(), "队伍增幅",
                "移除 " + teamName(teamId) + " 的增幅 " + dto.getBuffId(), ctx.getIp());
        return AjaxResult.success("增幅已移除");
    }

    @Override
    public BuffOptionsVO options() {
        BuffOptionsVO vo = new BuffOptionsVO();

        List<BuffOptionsVO.Option> potions = new ArrayList<>();
        for (PotionEffectType type : PotionEffectType.values()) {
            if (type == null) {
                continue;
            }
            potions.add(new BuffOptionsVO.Option(type.getName(),
                    TeamVoAssembler.potionDisplayName(type.getName())));
        }
        vo.setPotions(potions);

        List<BuffOptionsVO.Option> attributes = new ArrayList<>();
        ConfigurationSection root = plugin.getConfigManager().getBuffSection();
        ConfigurationSection attrs = root == null ? null : root.getConfigurationSection("attributes");
        if (attrs != null) {
            for (String key : attrs.getKeys(false)) {
                ConfigurationSection sec = attrs.getConfigurationSection(key);
                String name = sec == null ? key : sec.getString("display-name", key);
                attributes.add(new BuffOptionsVO.Option(key, name));
            }
        }
        vo.setAttributes(attributes);

        vo.setAttributePlugins(Arrays.asList("BigAttribute", "AttributePlus", "Attribute",
                "SX-Attribute", "ItemAttribute"));
        return vo;
    }

    // ===== 内部 =====

    private String teamName(UUID teamId) {
        Team team = plugin.getTeamManager().getLoadedTeam(teamId);
        if (team != null) {
            return team.getName();
        }
        return plugin.getTeamManager().getTeamNameIndex().getOrDefault(teamId, String.valueOf(teamId));
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
