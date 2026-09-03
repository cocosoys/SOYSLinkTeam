package soys.soyslinkteam.config;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.util.Text;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息文件（messages.yml）管理器。
 * <p>负责消息读取、占位符替换与发送。缺失的键会回退到 jar 内的默认值。</p>
 */
public class MessageManager {

    private static final String PREFIX_KEY = "prefix";

    private final SOYSLinkTeam plugin;
    private FileConfiguration messages;
    private FileConfiguration defaults;
    private String prefix = "";

    public MessageManager(SOYSLinkTeam plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        String language = plugin.getConfigManager().getLanguage();
        String fileName = resolveFileName(language);

        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            // 尝试从 jar 释放对应语言文件，失败则回退到默认 messages.yml
            if (!trySaveLanguageResource(language, fileName)) {
                file = new File(plugin.getDataFolder(), "messages.yml");
                if (!file.exists()) {
                    plugin.saveResource("messages.yml", false);
                }
            }
        }
        this.messages = YamlConfiguration.loadConfiguration(file);
        this.defaults = loadInternalDefaults(language);
        if (defaults != null) {
            messages.setDefaults(defaults);
        }
        this.prefix = messages.getString(PREFIX_KEY, "");
    }

    /**
     * 根据语言配置解析消息文件名。
     * zh_cn / 空 -> messages.yml（默认中文）
     * en -> messages_en.yml
     */
    private String resolveFileName(String language) {
        if (language == null || language.isEmpty()
                || language.equalsIgnoreCase("zh_cn")
                || language.equalsIgnoreCase("zhcn")
                || language.equalsIgnoreCase("zh")) {
            return "messages.yml";
        }
        return "messages_" + language.toLowerCase() + ".yml";
    }

    /**
     * 尝试从 jar 释放指定语言的消息文件到插件目录。
     * @return true 表示成功释放，false 表示 jar 中不存在该语言文件
     */
    private boolean trySaveLanguageResource(String language, String fileName) {
        String resourcePath = resolveFileName(language);
        try (InputStream stream = plugin.getResource(resourcePath)) {
            if (stream == null) {
                return false;
            }
            plugin.saveResource(resourcePath, false);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private FileConfiguration loadInternalDefaults(String language) {
        String resourcePath = resolveFileName(language);
        try (InputStream stream = plugin.getResource(resourcePath)) {
            if (stream == null) {
                // 指定语言不存在时回退到默认中文
                try (InputStream fallback = plugin.getResource("messages.yml")) {
                    if (fallback == null) {
                        return null;
                    }
                    return YamlConfiguration.loadConfiguration(
                            new InputStreamReader(fallback, StandardCharsets.UTF_8));
                }
            }
            return YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return null;
        }
    }

    // ================================================================
    //  读取
    // ================================================================

    /**
     * 读取一条消息（已完成前缀与占位符替换，未翻译颜色）。
     */
    public String get(String key, Map<String, String> placeholders) {
        String raw = messages.getString(key);
        if (raw == null) {
            return "&c[缺失消息: " + key + "]";
        }
        return apply(raw, placeholders);
    }

    public String get(String key) {
        return get(key, null);
    }

    /**
     * 读取一个消息列表。若该键实际是单条字符串，则包装为单元素列表。
     */
    public List<String> getList(String key, Map<String, String> placeholders) {
        List<String> raw = messages.getStringList(key);
        if (raw.isEmpty()) {
            String single = messages.getString(key);
            if (single == null) {
                List<String> missing = new ArrayList<>();
                missing.add("&c[缺失消息: " + key + "]");
                return missing;
            }
            raw = new ArrayList<>();
            raw.add(single);
        }
        List<String> result = new ArrayList<>(raw.size());
        for (String line : raw) {
            result.add(apply(line, placeholders));
        }
        return result;
    }

    public List<String> getList(String key) {
        return getList(key, null);
    }

    private String apply(String raw, Map<String, String> placeholders) {
        Map<String, String> merged = new HashMap<>();
        merged.put("prefix", prefix);
        if (placeholders != null) {
            merged.putAll(placeholders);
        }
        return Text.replace(raw, merged);
    }

    // ================================================================
    //  发送
    // ================================================================

    public void send(CommandSender sender, String key) {
        send(sender, key, null);
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        if (sender == null) {
            return;
        }
        Text.send(sender, get(key, placeholders));
    }

    public void sendList(CommandSender sender, String key) {
        sendList(sender, key, null);
    }

    public void sendList(CommandSender sender, String key, Map<String, String> placeholders) {
        if (sender == null) {
            return;
        }
        Text.send(sender, getList(key, placeholders));
    }

    public String getPrefix() {
        return prefix;
    }
}
