package cn.edu.ndky.nkctf.config;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * 生产环境密钥策略：禁止默认口令、JWT 密钥长度须满足 HS256 最低要求（256 bit）。
 */
public final class ProductionSecretsPolicy {

  /** JJWT {@code Keys.hmacShaKeyFor} 要求至少 256 bit */
  public static final int MIN_JWT_SECRET_BYTES = 32;

  private static final Set<String> FORBIDDEN_VALUES =
      Set.of(
          "",
          "nkctf123456",
          "nkctf-secret-key-please-change-in-production-environment-2024",
          "nkctf-dev-jwt-secret-local-only-32bytes-min!!");

  private ProductionSecretsPolicy() {}

  public static void validateJwtSecret(String secret) {
    if (secret == null || secret.isBlank()) {
      throw new IllegalStateException(
          "生产环境必须设置 NKCTF_JWT_SECRET（建议 openssl rand -base64 48）");
    }
    if (isForbidden(secret)) {
      throw new IllegalStateException("生产环境 NKCTF_JWT_SECRET 不得使用仓库内置或默认密钥");
    }
    int byteLength = secret.getBytes(StandardCharsets.UTF_8).length;
    if (byteLength < MIN_JWT_SECRET_BYTES) {
      throw new IllegalStateException(
          "生产环境 NKCTF_JWT_SECRET 过短（当前 "
              + byteLength
              + " 字节，HS256 至少需要 "
              + MIN_JWT_SECRET_BYTES
              + " 字节）");
    }
  }

  public static void validateDatabasePassword(String password) {
    if (password == null || password.isBlank()) {
      throw new IllegalStateException("生产环境必须设置 NKCTF_DB_PASSWORD");
    }
    if (isForbidden(password)) {
      throw new IllegalStateException("生产环境 NKCTF_DB_PASSWORD 不得使用默认口令 nkctf123456");
    }
  }

  public static void validateRedisPassword(String password) {
    if (password == null || password.isBlank()) {
      throw new IllegalStateException("生产环境必须设置 NKCTF_REDIS_PASSWORD");
    }
    if (isForbidden(password)) {
      throw new IllegalStateException("生产环境 NKCTF_REDIS_PASSWORD 不得使用默认口令 nkctf123456");
    }
  }

  static boolean isForbidden(String value) {
    return FORBIDDEN_VALUES.contains(value.trim());
  }
}
