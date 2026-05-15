package cn.edu.ndky.nkctf.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ProductionSecretsPolicyTest {

  private static final String VALID_JWT =
      "a".repeat(ProductionSecretsPolicy.MIN_JWT_SECRET_BYTES);

  @Test
  void acceptsStrongJwtSecret() {
    assertDoesNotThrow(() -> ProductionSecretsPolicy.validateJwtSecret(VALID_JWT));
  }

  @Test
  void rejectsShortJwtSecret() {
    assertThrows(
        IllegalStateException.class,
        () -> ProductionSecretsPolicy.validateJwtSecret("too-short"));
  }

  @Test
  void rejectsKnownDevJwtSecret() {
    assertThrows(
        IllegalStateException.class,
        () ->
            ProductionSecretsPolicy.validateJwtSecret(
                "nkctf-dev-jwt-secret-local-only-32bytes-min!!"));
  }

  @Test
  void rejectsDefaultDatabasePassword() {
    assertThrows(
        IllegalStateException.class,
        () -> ProductionSecretsPolicy.validateDatabasePassword("nkctf123456"));
  }

  @Test
  void rejectsDefaultRedisPassword() {
    assertThrows(
        IllegalStateException.class,
        () -> ProductionSecretsPolicy.validateRedisPassword("nkctf123456"));
  }
}
