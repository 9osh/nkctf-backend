package cn.edu.ndky.nkctf.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 生产环境启动时校验 JWT / 数据库 / Redis 密钥，防止误用默认配置上线。
 */
@Slf4j
@Component
@Profile("prod")
public class ProductionSecretsValidator implements ApplicationRunner {

  @Value("${jwt.secret}")
  private String jwtSecret;

  @Value("${spring.datasource.password}")
  private String dbPassword;

  @Value("${spring.data.redis.password}")
  private String redisPassword;

  @Override
  public void run(ApplicationArguments args) {
    log.info("校验生产环境密钥配置…");
    ProductionSecretsPolicy.validateJwtSecret(jwtSecret);
    ProductionSecretsPolicy.validateDatabasePassword(dbPassword);
    ProductionSecretsPolicy.validateRedisPassword(redisPassword);
    log.info("生产环境密钥校验通过");
  }
}
