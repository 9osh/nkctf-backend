package cn.edu.ndky.nkctf.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;
import java.time.Clock;

/**
 * ShedLock 配置
 *
 * 在多实例部署时，确保调度任务只在一个实例上执行。
 * 使用数据库（PostgreSQL）作为锁存储。
 *
 * 配置说明：
 * - defaultLockAtMostFor: 最长持有锁的时间（防止实例崩溃后锁无法释放）
 * - 使用数据库时间而非应用时间，避免时钟不同步问题
 */
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT30S")
public class ShedLockConfig {

  /**
   * 配置锁提供者
   *
   * 使用 JdbcTemplate 实现，将锁信息存储在 shedlock 表中。
   * 使用数据库时间（usingDbTime）确保多实例时间一致。
   */
  @Bean
  public LockProvider lockProvider(DataSource dataSource) {
    return new JdbcTemplateLockProvider(
        JdbcTemplateLockProvider.Configuration.builder()
            .withJdbcTemplate(new JdbcTemplate(dataSource))
            .usingDbTime()
            .build()
    );
  }

  /**
   * 系统时钟 Bean
   *
   * 用于服务中的时间获取，支持单元测试中的时间模拟。
   * 生产环境使用系统默认时钟。
   */
  @Bean
  public Clock clock() {
    return Clock.systemDefaultZone();
  }
}
