package soys.soyslinkteam.join;

import java.util.Collections;
import java.util.Map;

/**
 * 入队尝试的结果。
 * <p>
 * 三种结果语义：
 * <ul>
 *   <li>{@link Outcome#SUCCESS} — 条件满足，允许入队，注册表立即执行加入并停止尝试；</li>
 *   <li>{@link Outcome#DENIED} — 条件适用但被明确拒绝（如口令错误），立即中断并向玩家反馈；</li>
 *   <li>{@link Outcome#NOT_APPLICABLE} — 该方式对当前场景不适用，注册表继续尝试下一种方式。</li>
 * </ul>
 */
public class JoinResult {

    public enum Outcome {
        SUCCESS,
        DENIED,
        NOT_APPLICABLE
    }

    private final Outcome outcome;
    private final String messageKey;
    private final Map<String, String> placeholders;

    private JoinResult(Outcome outcome, String messageKey, Map<String, String> placeholders) {
        this.outcome = outcome;
        this.messageKey = messageKey;
        this.placeholders = placeholders == null
                ? Collections.<String, String>emptyMap() : placeholders;
    }

    public static JoinResult success() {
        return new JoinResult(Outcome.SUCCESS, null, null);
    }

    public static JoinResult denied(String messageKey) {
        return new JoinResult(Outcome.DENIED, messageKey, null);
    }

    public static JoinResult denied(String messageKey, Map<String, String> placeholders) {
        return new JoinResult(Outcome.DENIED, messageKey, placeholders);
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

    public Map<String, String> getPlaceholders() {
        return placeholders;
    }
}
