package soys.soyslinkteam.util;

import java.util.HashMap;
import java.util.Map;

/**
 * 占位符构建器，用于以链式方式组装消息占位符。
 *
 * <pre>
 * Placeholders.of("team", team.getName()).and("size", "3").build()
 * </pre>
 */
public final class Placeholders {

    private final Map<String, String> map = new HashMap<>();

    private Placeholders() {
    }

    public static Placeholders create() {
        return new Placeholders();
    }

    public static Placeholders of(String key, String value) {
        return new Placeholders().and(key, value);
    }

    public static Placeholders of(String key, Object value) {
        return new Placeholders().and(key, value);
    }

    public Placeholders and(String key, String value) {
        map.put(key, value == null ? "" : value);
        return this;
    }

    public Placeholders and(String key, Object value) {
        map.put(key, value == null ? "" : String.valueOf(value));
        return this;
    }

    public Map<String, String> build() {
        return map;
    }
}
