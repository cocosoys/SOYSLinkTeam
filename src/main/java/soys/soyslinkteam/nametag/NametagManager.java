package soys.soyslinkteam.nametag;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.event.PlayerJoinTeamEvent;
import soys.soyslinkteam.event.PlayerLeaveTeamEvent;
import soys.soyslinkteam.event.TeamRoleChangeEvent;
import soys.soyslinkteam.event.TeamSettingChangeEvent;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.team.TeamMember;
import soys.soyslinkteam.team.TeamRole;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 头顶名称 / 计分板替换（数据包方案，需 ProtocolLib）。
 * <p>
 * 通过向每个观察者发送 {@code SCOREBOARD_TEAM} 虚拟队伍数据包，把队伍简称作为玩家
 * 头顶/ Tab 列表前缀。该方案不触碰玩家真实计分板，因此与 EssentialsX / CMI 自行管理的
 * 计分板/昵称完全共存 —— 这是将本插件声明为二者 softdepend 前提下唯一自洽的实现路线。
 * </p>
 * <p>
 * 说明（Spigot 1.12.2 限制）：1.12.2 的 SCOREBOARD_TEAM 数据包不含 collision / visibility /
 * 独立的 name-color 字段，故碰撞规则与名称可见性沿用服务端默认值，角色着色通过前缀尾部
 * 颜色码向下延续到玩家名（受 16 字符前缀上限约束，超长时自动截断以保证客户端不报警）。
 * </p>
 */
public class NametagManager implements Listener {

    private final SOYSLinkTeam plugin;
    private ProtocolManager protocolManager;
    private boolean enabled = false;

    /** 成员 UUID → 虚拟队伍名（稳定映射，避免每次重建） */
    private final Map<UUID, String> teamNames = new ConcurrentHashMap<>();
    /** 当前持有虚拟队伍的成员 */
    private final Map<UUID, Boolean> active = new ConcurrentHashMap<>();

    /** 1.12.2 计分板队伍名上限 16 字符 */
    private static final int TEAM_NAME_LIMIT = 16;
    /** 1.12.2 前缀/后缀上限 16 字符（颜色码计 2） */
    private static final int PREFIX_LIMIT = 16;

    public NametagManager(SOYSLinkTeam plugin) {
        this.plugin = plugin;
    }

    /**
     * 初始化：检测配置与 ProtocolLib 可用性。
     */
    public void initialize() {
        if (!plugin.getConfigManager().isNametagEnabled()) {
            plugin.getLogger().info("头顶名称功能已在配置中关闭。");
            return;
        }
        if (!Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
            plugin.getLogger().warning("未检测到 ProtocolLib，头顶名称功能已停用（不影响其它功能）。");
            return;
        }
        try {
            protocolManager = ProtocolLibrary.getProtocolManager();
        } catch (Throwable t) {
            plugin.getLogger().warning("获取 ProtocolLib 失败，头顶名称功能已停用: " + t.getMessage());
            return;
        }
        enabled = true;
        // 为当前在线且已有队伍的玩家建立虚拟队伍
        for (Player player : Bukkit.getOnlinePlayers()) {
            refreshForViewer(player);
            plugin.getTeamManager().getPlayerTeamAsync(player.getUniqueId(), team -> {
                if (team != null) {
                    updatePlayer(player.getUniqueId());
                }
            });
        }
        plugin.getLogger().info("头顶名称（数据包方案）已启用。");
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 插件停用：清除所有虚拟队伍，恢复玩家默认显示。
     */
    public void shutdown() {
        if (!enabled) {
            return;
        }
        for (UUID uuid : active.keySet()) {
            removePlayer(uuid);
        }
        active.clear();
        teamNames.clear();
    }

    /**
     * 配置热重载后全量刷新。
     */
    public void refreshAll() {
        if (!enabled) {
            initialize();
            return;
        }
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            refreshForViewer(viewer);
        }
    }

    // ================================================================
    //  观察者维度：让某个玩家看到所有人的虚拟队伍
    // ================================================================

    private void refreshForViewer(Player viewer) {
        if (!enabled || protocolManager == null) {
            return;
        }
        for (Player target : Bukkit.getOnlinePlayers()) {
            Team team = plugin.getTeamManager().getLoadedPlayerTeam(target.getUniqueId());
            if (team == null) {
                continue;
            }
            TeamMember member = team.getMember(target.getUniqueId());
            if (member == null || !target.isOnline()) {
                continue;
            }
            sendTeamPacket(viewer, buildPacket(team, member, target, 0));
        }
    }

    // ================================================================
    //  成员维度：建立 / 更新 / 移除某个玩家的虚拟队伍
    // ================================================================

    private void updatePlayer(UUID uuid) {
        if (!enabled || protocolManager == null) {
            return;
        }
        Team team = plugin.getTeamManager().getLoadedPlayerTeam(uuid);
        if (team == null) {
            return;
        }
        TeamMember member = team.getMember(uuid);
        Player player = member == null ? null : member.getPlayer();
        if (player == null || !player.isOnline()) {
            // 离线：清理可能残留的虚拟队伍
            if (active.containsKey(uuid)) {
                removePlayer(uuid);
            }
            return;
        }
        active.put(uuid, Boolean.TRUE);
        PacketContainer packet = buildPacket(team, member, player, 0);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            sendTeamPacket(viewer, packet);
        }
    }

    private void removePlayer(UUID uuid) {
        if (!enabled || protocolManager == null) {
            return;
        }
        String name = teamNames.remove(uuid);
        active.remove(uuid);
        String playerName = null;
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            playerName = player.getName();
        } else {
            Team team = plugin.getTeamManager().getLoadedPlayerTeam(uuid);
            if (team != null) {
                TeamMember member = team.getMember(uuid);
                if (member != null) {
                    playerName = member.getName();
                }
            }
        }
        if (name == null || playerName == null) {
            return;
        }
        Collection<String> players = Collections.singletonList(playerName);
        // 先从各观察者移除该玩家，再删除虚拟队伍
        PacketContainer removePlayer = buildRaw(name, name, "", "", players, 4);
        PacketContainer removeTeam = buildRaw(name, name, "", "", Collections.<String>emptyList(), 1);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            sendTeamPacket(viewer, removePlayer);
            sendTeamPacket(viewer, removeTeam);
        }
    }

    // ================================================================
    //  数据包构建
    // ================================================================

    private PacketContainer buildPacket(Team team, TeamMember member, Player player, int mode) {
        String tag = team.getSettings().hasTag()
                ? team.getSettings().getTag()
                : team.getName();
        String roleColor = plugin.getConfigManager().isNametagUseRoleColor()
                ? roleColor(member.getRole()) : "";
        String prefix = fitToLimit(
                ChatColor.translateAlternateColorCodes('&',
                        "&8[&e" + tag + "&8]" + roleColor),
                PREFIX_LIMIT);
        String teamName = virtualTeamName(member.getUuid());
        return buildRaw(teamName, teamName, prefix, "",
                Collections.singletonList(player.getName()), mode);
    }

    private PacketContainer buildRaw(String teamName, String displayName,
                                     String prefix, String suffix,
                                     Collection<String> players, int mode) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.SCOREBOARD_TEAM);
        // 1.12.2 字段顺序: a=name, b=displayName, c=prefix, d=suffix, e=flags(byte), f=players, g=method(int)
        packet.getStrings().write(0, teamName);
        packet.getStrings().write(1, displayName);
        packet.getStrings().write(2, prefix);
        packet.getStrings().write(3, suffix);
        packet.getBytes().write(0, (byte) 0); // friendlyFire=0, seeFriendlyInvisibles=0
        packet.getSpecificModifier(Collection.class).write(0, players);
        packet.getIntegers().write(0, mode);
        return packet;
    }

    private void sendTeamPacket(Player receiver, PacketContainer packet) {
        try {
            protocolManager.sendServerPacket(receiver, packet);
        } catch (Exception e) {
            plugin.getLogger().warning("发送头顶名称数据包失败: " + e.getMessage());
        }
    }

    /**
     * 生成稳定且不超过 16 字符的虚拟队伍名。
     */
    private String virtualTeamName(UUID uuid) {
        return teamNames.computeIfAbsent(uuid,
                id -> "SOYS_" + id.toString().replace("-", "").substring(0, 11));
    }

    private String roleColor(TeamRole role) {
        switch (role) {
            case LEADER:
                return "&6";
            case ADMIN:
                return "&b";
            default:
                return "";
        }
    }

    /**
     * 将文本截断到指定可见长度，颜色码（§x）计 2，且绝不从颜色码中间截断。
     */
    private String fitToLimit(String s, int limit) {
        StringBuilder sb = new StringBuilder();
        int len = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\u00a7' && i + 1 < s.length()) {
                if (len + 2 > limit) {
                    break;
                }
                sb.append(c).append(s.charAt(i + 1));
                len += 2;
                i++;
                continue;
            }
            if (len + 1 > limit) {
                break;
            }
            sb.append(c);
            len++;
        }
        return sb.toString();
    }

    // ================================================================
    //  事件监听
    // ================================================================

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        refreshForViewer(player); // 新观察者看到其他人
        plugin.getTeamManager().getPlayerTeamAsync(player.getUniqueId(), team -> {
            if (team != null) {
                updatePlayer(player.getUniqueId()); // 其他人看到新加入者
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        removePlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onJoinTeam(PlayerJoinTeamEvent event) {
        updatePlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onLeaveTeam(PlayerLeaveTeamEvent event) {
        removePlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onRoleChange(TeamRoleChangeEvent event) {
        updatePlayer(event.getMember().getUuid());
    }

    @EventHandler
    public void onSettingChange(TeamSettingChangeEvent event) {
        if (event.getSetting() != TeamSettingChangeEvent.SettingType.TAG) {
            return;
        }
        for (TeamMember member : event.getTeam().getMembers()) {
            updatePlayer(member.getUuid());
        }
    }
}
