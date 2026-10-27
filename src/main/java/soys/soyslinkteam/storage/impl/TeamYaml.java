package soys.soyslinkteam.storage.impl;

import org.bukkit.configuration.file.YamlConfiguration;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.team.TeamMember;

/**
 * 队伍对象的 YAML 序列化工具。
 * <p>与 {@link YamlStorage} 的数据格式保持一致，供存储后端写入与快照备份共用，
 * 避免同一份序列化逻辑出现多份实现。</p>
 */
public final class TeamYaml {

    /** 快照/数据文件的根节点 */
    public static final String ROOT = "teams";

    private TeamYaml() {
    }

    /**
     * 把一支队伍序列化到指定配置节。
     *
     * @param config 目标配置对象
     * @param base   该队伍的节点路径（不含根节点，如 "teams.&lt;uuid&gt;"）
     * @param team   队伍对象
     */
    public static void write(YamlConfiguration config, String base, Team team) {
        config.set(base + ".name", team.getName());
        config.set(base + ".leader", team.getLeader() == null ? null : team.getLeader().toString());
        config.set(base + ".created-at", team.getCreatedAt());
        config.set(base + ".settings.open", team.getSettings().isOpen());
        config.set(base + ".settings.password", team.getSettings().getPassword());
        config.set(base + ".settings.notice", team.getSettings().getNotice());
        config.set(base + ".settings.tag", team.getSettings().getTag());
        config.set(base + ".settings.economy-cost", team.getSettings().getEconomyCost());
        config.set(base + ".settings.item-cost", team.getSettings().getItemCost());

        // 整体重写成员节点，避免残留已退出的成员
        config.set(base + ".members", null);
        for (TeamMember member : team.getMembers()) {
            String path = base + ".members." + member.getUuid();
            config.set(path + ".name", member.getName());
            config.set(path + ".role", member.getRole().name());
            config.set(path + ".joined-at", member.getJoinedAt());
            config.set(path + ".last-seen", member.getLastSeen());
            config.set(path + ".chat-channel", member.isChatChannel());
        }
    }
}
