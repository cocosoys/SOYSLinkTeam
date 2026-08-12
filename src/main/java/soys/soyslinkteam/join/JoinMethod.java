package soys.soyslinkteam.join;

/**
 * 入队方式抽象。
 * <p>
 * 每一种玩家进入队伍的途径都是一个独立实现：队长邀请、公开加入、口令加入……
 * {@link JoinMethodRegistry} 按 config.yml 中配置的 order 顺序依次尝试，
 * 直到某个方式给出 SUCCESS 或 DENIED。
 * </p>
 * <p>
 * 第三方插件实现本接口并通过
 * {@link JoinMethodRegistry#register(JoinMethod)} 注册后，即可新增入队途径
 * （如消耗道具入队、达成成就入队、赛季资格入队）。
 * </p>
 */
public interface JoinMethod {

    /**
     * 方式唯一标识，同时作为 config.yml 中 {@code join.methods.<id>} 的配置节名。
     */
    String getId();

    /**
     * 展示名称，用于消息提示。
     */
    String getDisplayName();

    /**
     * 该方式当前是否启用。默认读取 {@code join.methods.<id>.enabled}。
     */
    boolean isEnabled();

    /**
     * 尝试顺序，数字越小越先被尝试。默认读取 {@code join.methods.<id>.order}。
     */
    int getOrder();

    /**
     * 判定玩家能否通过本方式加入队伍。
     * <p>
     * <b>本方法不应执行实际的入队动作</b>，只做条件判定；
     * 实际入队由 {@link JoinMethodRegistry} 统一处理，以保证索引与存储一致。
     * </p>
     */
    JoinResult attempt(JoinContext context);

    /**
     * 入队成功后的回调，用于清理本方式的临时状态（如消费掉一条邀请）。默认无操作。
     */
    default void onJoinSuccess(JoinContext context) {
    }

    /**
     * 配置重载回调。默认无操作。
     */
    default void reload() {
    }
}
