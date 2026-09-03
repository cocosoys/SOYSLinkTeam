package soys.soyslinkteam.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 口令哈希工具。
 * <p>使用 SHA-256 存储口令哈希，验证时同时兼容旧版明文存储（自动回退比对）。</p>
 */
public final class PasswordUtil {

    private PasswordUtil() {
    }

    /**
     * 计算明文口令的 SHA-256 哈希（小写十六进制，64 字符）。
     */
    public static String hash(String plain) {
        if (plain == null) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(plain.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 算法不可用", e);
        }
    }

    /**
     * 验证明文口令是否与存储值匹配。
     * <p>优先按哈希比对；若不匹配则回退到明文比对，以兼容升级前的旧数据。</p>
     *
     * @param plain         用户输入的明文口令
     * @param stored        存储的值（可能是哈希或旧版明文）
     * @param caseSensitive 是否区分大小写
     * @return 匹配返回 true
     */
    public static boolean matches(String plain, String stored, boolean caseSensitive) {
        if (stored == null || stored.isEmpty() || plain == null) {
            return false;
        }
        String normalized = caseSensitive ? plain : plain.toLowerCase();
        // 优先哈希比对
        if (hash(normalized).equals(stored)) {
            return true;
        }
        // 兼容旧数据：明文比对
        return caseSensitive ? stored.equals(plain) : stored.equalsIgnoreCase(plain);
    }

    /**
     * 判断存储值是否为 SHA-256 哈希格式（64 位小写十六进制）。
     */
    public static boolean isHashed(String stored) {
        return stored != null && stored.matches("^[a-f0-9]{64}$");
    }
}
