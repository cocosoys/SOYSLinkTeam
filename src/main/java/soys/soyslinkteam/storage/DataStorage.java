package soys.soyslinkteam.storage;

import soys.soyslinkteam.team.Team;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 数据存储后端抽象。
 * <p>
 * 所有实现必须满足：
 * <ul>
 *   <li>方法可能在异步线程被调用，实现需自行保证线程安全；</li>
 *   <li>任何失败都以抛出异常的形式上报，由 {@link StorageManager} 统一降级处理；</li>
 *   <li>{@link #saveTeam(Team)} 语义为 upsert，队伍不存在时插入，存在时整体覆盖。</li>
 * </ul>
 * 新增后端只需实现本接口并在 {@link StorageManager#buildStorage} 中注册。
 * </p>
 */
public interface DataStorage {

    /**
     * 后端类型。
     */
    StorageType getType();

    /**
     * 初始化连接 / 建表 / 创建数据文件。
     *
     * @throws Exception 初始化失败，该后端将被标记为不可用
     */
    void initialize() throws Exception;

    /**
     * 释放资源，关服或重载时调用。
     */
    void shutdown();

    /**
     * 后端当前是否可用。不可用的后端会被跳过而非导致插件崩溃。
     */
    boolean isAvailable();

    /**
     * 供 /teamadmin storage 展示的简要描述，如文件路径或数据库地址。
     */
    String describe();

    // ================================================================
    //  读
    // ================================================================

    /**
     * 按 ID 读取单支队伍。
     *
     * @return 队伍，不存在时返回 null
     */
    Team loadTeam(UUID teamId) throws Exception;

    /**
     * 读取全部队伍。仅用于迁移、同步与管理指令，常规流程不应调用。
     */
    Collection<Team> loadAllTeams() throws Exception;

    /**
     * 读取「玩家 UUID -&gt; 队伍 ID」索引。
     * <p>该索引常驻内存，使插件无需装载队伍即可判断玩家归属。</p>
     */
    Map<UUID, UUID> loadPlayerIndex() throws Exception;

    /**
     * 读取「队伍 ID -&gt; 队伍名称」映射，用于重名校验与队伍列表。
     */
    Map<UUID, String> loadTeamNames() throws Exception;

    /**
     * 统计队伍总数。
     */
    int countTeams() throws Exception;

    // ================================================================
    //  写
    // ================================================================

    /**
     * 保存（upsert）单支队伍。
     */
    void saveTeam(Team team) throws Exception;

    /**
     * 批量保存。实现应尽可能使用事务或单次落盘以提升性能。
     */
    void saveTeams(Collection<Team> teams) throws Exception;

    /**
     * 删除队伍。
     */
    void deleteTeam(UUID teamId) throws Exception;

    /**
     * 清空全部数据。仅由迁移覆盖流程调用。
     */
    void clear() throws Exception;

    /**
     * 已存在的队伍名集合（小写），用于重名校验。
     */
    Set<String> loadLowerCaseNames() throws Exception;
}
