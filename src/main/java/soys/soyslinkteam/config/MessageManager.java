package soys.soyslinkteam.config;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.util.Text;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 语言文件管理器（i18n）。
 * <p>
 * 语言文件位于 {@code plugins/SOYSLinkTeam/language/<code>.yml}，code 为全小写下划线
 * （{@code zh_cn} / {@code en_us}），与 SOYSHTTPOverMC 的 i18n 规范对齐：
 * <ul>
 *   <li>平铺键：一行一条完整点分 key，前缀固定 {@code soyslinkteam.}；</li>
 *   <li>颜色码直接写 {@code §}（兼容残留的 {@code &}）；</li>
 *   <li>业务占位符为位置参数 {@code {0} {1} ...}；</li>
 *   <li>{@code {prefix}} 为内置前缀占位符，由本类按 {@code soyslinkteam.prefix} 自动替换。</li>
 * </ul>
 * 回退链：外部当前语言文件 → jar 内置当前语言 → jar 内置 en_us。
 * </p>
 */
public class MessageManager {

    private static final String PREFIX_KEY = "soyslinkteam.prefix";
    private static final String DEFAULT_LANG = "zh_cn";
    private static final String FALLBACK_LANG = "en_us";

    private final SOYSLinkTeam plugin;
    private FileConfiguration messages;
    private String prefix = "";

    public MessageManager(SOYSLinkTeam plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        String code = normalizeLanguage(plugin.getConfigManager().getLanguage());
        File langDir = new File(plugin.getDataFolder(), "language");

        // 首次运行释放内置语言文件（en_us 始终释放，当前语言一并释放）
        saveResourceIfMissing("language/" + FALLBACK_LANG + ".yml");
        saveResourceIfMissing("language/" + code + ".yml");

        // 构建三层回退：external 当前语言 → internal 当前语言 → internal en_us
        FileConfiguration internalEnUs = loadInternal("language/" + FALLBACK_LANG + ".yml");
        FileConfiguration internalTarget = loadInternal("language/" + code + ".yml");
        if (internalTarget == null) {
            internalTarget = internalEnUs;
        } else if (internalEnUs != null) {
            internalTarget.setDefaults(internalEnUs);
        }

        File extFile = new File(langDir, code + ".yml");
        FileConfiguration ext = YamlConfiguration.loadConfiguration(extFile);
        if (internalTarget != null) {
            ext.setDefaults(internalTarget);
        }
        this.messages = ext;
        this.prefix = ext.getString(PREFIX_KEY, "");
    }

    /**
     * 规范化语言标识：统一小写下划线，兼容旧的 zh_CN / zh / en 等写法。
     */
    private String normalizeLanguage(String raw) {
        if (raw == null) {
            return DEFAULT_LANG;
        }
        String c = raw.trim().toLowerCase().replace('-', '_');
        if (c.isEmpty() || c.equals("zh") || c.equals("zh_cn") || c.equals("zhcn")) {
            return DEFAULT_LANG;
        }
        if (c.equals("en") || c.equals("en_us") || c.equals("enus")) {
            return FALLBACK_LANG;
        }
        return c;
    }

    private void saveResourceIfMissing(String path) {
        File target = new File(plugin.getDataFolder(), path);
        if (target.exists()) {
            return;
        }
        try (InputStream in = plugin.getResource(path)) {
            if (in == null) {
                return;
            }
            plugin.saveResource(path, false);
        } catch (Exception ignored) {
        }
    }

    private FileConfiguration loadInternal(String path) {
        try (InputStream in = plugin.getResource(path)) {
            if (in == null) {
                return null;
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return null;
        }
    }

    // ================================================================
    //  读取
    // ================================================================

    /**
     * 读取一条消息并完成 {prefix} 与 {i} 位置参数替换。缺键返回空串（不发送）。
     */
    public String get(String key, Object... args) {
        String raw = messages.getString(key);
        if (raw == null) {
            return "";
        }
        return apply(raw, args);
    }

    /**
     * 读取一条消息，缺键时使用代码内兜底文本（对齐 SOYSHTTPOverMC 的 I18n.t 语义）。
     */
    public String t(String key, String fallback, Object... args) {
        String raw = messages.getString(key);
        if (raw == null) {
            raw = fallback;
        }
        if (raw == null) {
            return "";
        }
        return apply(raw, args);
    }

    /**
     * 读取一个消息列表。Bukkit 会把含 \n 的字符串值按行拆分为 List。
     */
    public List<String> getList(String key, Object... args) {
        List<String> raw = messages.getStringList(key);
        if (raw == null || raw.isEmpty()) {
            String single = messages.getString(key);
            if (single == null) {
                return new ArrayList<>();
            }
            raw = new ArrayList<>();
            raw.add(single);
        }
        List<String> out = new ArrayList<>(raw.size());
        for (String line : raw) {
            out.add(apply(line, args));
        }
        return out;
    }

    private String apply(String raw, Object... args) {
        if (raw == null || raw.isEmpty()) {
            return raw == null ? "" : raw;
        }
        String r = raw.replace("{prefix}", prefix);
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                r = r.replace("{" + i + "}", String.valueOf(args[i]));
            }
        }
        return r;
    }

    // ================================================================
    //  发送
    // ================================================================

    public void send(CommandSender sender, String key, Object... args) {
        if (sender == null) {
            return;
        }
        Text.send(sender, get(key, args));
    }

    public void sendList(CommandSender sender, String key, Object... args) {
        if (sender == null) {
            return;
        }
        for (String line : getList(key, args)) {
            Text.send(sender, line);
        }
    }

    public String getPrefix() {
        return prefix;
    }
}
