package soys.soyslinkteam.util;

import soys.soyslinkteam.SOYSLinkTeam;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 队伍名称与简称校验器。
 * <p>被 create 与 set 指令共用，保证校验规则单点维护。</p>
 */
public final class NameValidator {

    private final SOYSLinkTeam plugin;

    public NameValidator(SOYSLinkTeam plugin) {
        this.plugin = plugin;
    }

    /**
     * 校验结果。
     */
    public static class Result {

        private final boolean valid;
        private final String messageKey;
        private final Map<String, String> placeholders;

        private Result(boolean valid, String messageKey, Map<String, String> placeholders) {
            this.valid = valid;
            this.messageKey = messageKey;
            this.placeholders = placeholders == null
                    ? Collections.<String, String>emptyMap() : placeholders;
        }

        public static Result ok() {
            return new Result(true, null, null);
        }

        public static Result fail(String messageKey, Map<String, String> placeholders) {
            return new Result(false, messageKey, placeholders);
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessageKey() {
            return messageKey;
        }

        public Map<String, String> getPlaceholders() {
            return placeholders;
        }
    }

    /**
     * 校验队伍名称。
     *
     * @param name      待校验名称
     * @param excluding 判重时忽略的队伍 ID（改名时传自己的 ID），可为 null
     */
    public Result validateTeamName(String name, UUID excluding) {
        if (name == null || name.isEmpty()) {
            return Result.fail("team.create.name-too-short",
                    Placeholders.of("min", plugin.getConfigManager().getNameMinLength()).build());
        }

        int min = plugin.getConfigManager().getNameMinLength();
        int max = plugin.getConfigManager().getNameMaxLength();

        if (name.length() < min) {
            return Result.fail("team.create.name-too-short",
                    Placeholders.of("min", min).build());
        }
        if (name.length() > max) {
            return Result.fail("team.create.name-too-long",
                    Placeholders.of("max", max).build());
        }

        String pattern = plugin.getConfigManager().getNamePattern();
        if (!matches(pattern, name)) {
            return Result.fail("team.create.name-invalid", Collections.<String, String>emptyMap());
        }

        String lower = name.toLowerCase();
        for (String word : plugin.getConfigManager().getNameBlacklist()) {
            if (word != null && !word.isEmpty() && lower.contains(word.toLowerCase())) {
                return Result.fail("team.create.name-blacklisted",
                        Collections.<String, String>emptyMap());
            }
        }

        if (!plugin.getConfigManager().isAllowDuplicateName()) {
            UUID existing = plugin.getTeamManager().getTeamIdByName(name);
            if (existing != null && !existing.equals(excluding)) {
                return Result.fail("team.create.name-duplicate",
                        Placeholders.of("team", name).build());
            }
        }

        return Result.ok();
    }

    /**
     * 校验队伍简称。
     */
    public Result validateTag(String tag) {
        int min = plugin.getConfigManager().getTagMinLength();
        int max = plugin.getConfigManager().getTagMaxLength();

        if (tag == null || tag.length() < min) {
            return Result.fail("settings.tag.too-short", Placeholders.of("min", min).build());
        }
        if (tag.length() > max) {
            return Result.fail("settings.tag.too-long", Placeholders.of("max", max).build());
        }
        if (!matches(plugin.getConfigManager().getTagPattern(), tag)) {
            return Result.fail("settings.tag.invalid", Collections.<String, String>emptyMap());
        }
        return Result.ok();
    }

    /**
     * 正则匹配，表达式非法时记录警告并放行，避免配置错误导致功能不可用。
     */
    private boolean matches(String pattern, String input) {
        if (pattern == null || pattern.isEmpty()) {
            return true;
        }
        try {
            return Pattern.compile(pattern).matcher(input).matches();
        } catch (PatternSyntaxException e) {
            plugin.getLogger().warning("config.yml 中的正则表达式无效: " + pattern
                    + "，本次校验已跳过");
            return true;
        }
    }
}
