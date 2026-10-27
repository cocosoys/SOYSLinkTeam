package soys.soyslinkteam.util;

import soys.soyslinkteam.SOYSLinkTeam;

import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 队伍名称与简称校验器。
 * <p>被 create 与 set 指令共用，保证校验规则单点维护。</p>
 * <p>校验失败消息采用位置参数 {@code {0} {1} ...}，与语言文件顺序一致。</p>
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
        private final Object[] args;

        private Result(boolean valid, String messageKey, Object[] args) {
            this.valid = valid;
            this.messageKey = messageKey;
            this.args = args == null ? new Object[0] : args;
        }

        public static Result ok() {
            return new Result(true, null, null);
        }

        public static Result fail(String messageKey, Object... args) {
            return new Result(false, messageKey, args);
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessageKey() {
            return messageKey;
        }

        public Object[] getArgs() {
            return args;
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
            return Result.fail("soyslinkteam.team.create.name-too-short",
                    plugin.getConfigManager().getNameMinLength());
        }

        int min = plugin.getConfigManager().getNameMinLength();
        int max = plugin.getConfigManager().getNameMaxLength();

        if (name.length() < min) {
            return Result.fail("soyslinkteam.team.create.name-too-short", min);
        }
        if (name.length() > max) {
            return Result.fail("soyslinkteam.team.create.name-too-long", max);
        }

        String pattern = plugin.getConfigManager().getNamePattern();
        if (!matches(pattern, name)) {
            return Result.fail("soyslinkteam.team.create.name-invalid");
        }

        String lower = name.toLowerCase();
        for (String word : plugin.getConfigManager().getNameBlacklist()) {
            if (word != null && !word.isEmpty() && lower.contains(word.toLowerCase())) {
                return Result.fail("soyslinkteam.team.create.name-blacklisted");
            }
        }

        if (!plugin.getConfigManager().isAllowDuplicateName()) {
            UUID existing = plugin.getTeamManager().getTeamIdByName(name);
            if (existing != null && !existing.equals(excluding)) {
                return Result.fail("soyslinkteam.team.create.name-duplicate", name);
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
            return Result.fail("soyslinkteam.settings.tag.too-short", min);
        }
        if (tag.length() > max) {
            return Result.fail("soyslinkteam.settings.tag.too-long", max);
        }
        if (!matches(plugin.getConfigManager().getTagPattern(), tag)) {
            return Result.fail("soyslinkteam.settings.tag.invalid");
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
