package soys.soyslinkteam.join;

/**
 * 入队尝试的结果。
 * <p>
 * 三种结果语义：
 * <ul>
 *   <li>{@link Outcome#SUCCESS} — 条件满足，允许入队，注册表立即执行加入并停止尝试；</li>
 *   <li>{@link Outcome#DENIED} — 条件适用但被明确拒绝（如口令错误），立即中断并向玩家反馈；</li>
 *   <li>{@link Outcome#NOT_APPLICABLE} — 该方式对当前场景不适用，注册表继续尝试下一种方式。</li>
 * </ul>
 * 占位符采用位置参数 {@code {0} {1} ...}，与语言文件中的顺序一致。
 * </p>
 */
public class JoinResult {

    public enum Outcome {
        SUCCESS,
        DENIED,
        NOT_APPLICABLE
    }

    private final Outcome outcome;
    private final String messageKey;
    private final Object[] args;

    private JoinResult(Outcome outcome, String messageKey, Object[] args) {
        this.outcome = outcome;
        this.messageKey = messageKey;
        this.args = args == null ? new Object[0] : args;
    }

    public static JoinResult success() {
        return new JoinResult(Outcome.SUCCESS, null, null);
    }

    public static JoinResult denied(String messageKey, Object... args) {
        return new JoinResult(Outcome.DENIED, messageKey, args);
    }

    public static JoinResult notApplicable() {
        return new JoinResult(Outcome.NOT_APPLICABLE, null, null);
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public boolean isSuccess() {
        return outcome == Outcome.SUCCESS;
    }

    public boolean isDenied() {
        return outcome == Outcome.DENIED;
    }

    public boolean isNotApplicable() {
        return outcome == Outcome.NOT_APPLICABLE;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getArgs() {
        return args;
    }
}
