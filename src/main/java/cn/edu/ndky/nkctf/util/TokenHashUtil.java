package cn.edu.ndky.nkctf.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Token 哈希工具类
 * 使用 SHA-256 对 Refresh Token 进行哈希，确保数据库中不存储明文 Token
 */
public final class TokenHashUtil {

    private TokenHashUtil() {
        // 工具类不允许实例化
    }

    /**
     * 使用 SHA-256 对 Token 进行哈希
     *
     * @param token 明文 Token
     * @return 64 字符的十六进制哈希字符串
     * @throws IllegalArgumentException 如果 token 为 null
     */
    public static String hashToken(String token) {
        if (token == null) {
            throw new IllegalArgumentException("Token cannot be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 是 Java 标准实现，不应该抛出此异常
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * 将字节数组转换为十六进制字符串
     *
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(64);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
