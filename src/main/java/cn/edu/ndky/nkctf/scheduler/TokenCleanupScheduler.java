package cn.edu.ndky.nkctf.scheduler;

import cn.edu.ndky.nkctf.mapper.RefreshTokenMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Token 清理定时任务
 *
 * <p>定期清理数据库中已过期或已撤销的 Refresh Token，防止表无限增长。</p>
 *
 * <h3>清理策略</h3>
 * <ul>
 *   <li>每天凌晨 3:00 执行</li>
 *   <li>删除已撤销的 Token（revoked = TRUE）</li>
 *   <li>删除已过期的 Token（expires_at < NOW()）</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

  private final RefreshTokenMapper refreshTokenMapper;

  /**
   * 清理已过期或已撤销的 Refresh Token
   *
   * <p>每天凌晨 3:00 执行，清理无效的 Refresh Token 记录。</p>
   */
  @Scheduled(cron = "0 0 3 * * ?")
  @Transactional
  public void cleanupExpiredTokens() {
    log.info("开始清理过期和已撤销的 Refresh Token...");
    int deleted = refreshTokenMapper.deleteExpiredAndRevoked();
    log.info("Refresh Token 清理完成，删除 {} 条记录", deleted);
  }
}
