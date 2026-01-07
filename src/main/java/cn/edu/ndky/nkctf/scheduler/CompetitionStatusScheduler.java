package cn.edu.ndky.nkctf.scheduler;

import cn.edu.ndky.nkctf.service.CompetitionStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 竞赛状态同步调度器
 *
 * 定期扫描数据库中的竞赛，将需要状态变更的竞赛更新为正确的状态。
 *
 * 设计说明：
 * - 每 30 秒执行一次，平衡响应速度和数据库负载
 * - 使用 ShedLock 确保多实例部署时只有一个实例执行
 * - 只更新确实需要变更的竞赛，避免不必要的数据库写入
 *
 * 注意：
 * - API 读取使用实时计算的状态，不依赖于调度器的更新频率
 * - 调度器主要负责：1) 持久化状态以供数据库查询 2) 触发状态变更事件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompetitionStatusScheduler {

  private final CompetitionStatusService statusService;

  /**
   * 同步竞赛状态
   *
   * @Scheduled(fixedRate = 30000): 每 30 秒执行一次
   * @SchedulerLock: 分布式锁配置
   *   - name: 锁的唯一名称
   *   - lockAtLeastFor: 最少持有锁 10 秒，防止多实例快速重复执行
   *   - lockAtMostFor: 最多持有锁 30 秒，防止实例崩溃后锁无法释放
   */
  @Scheduled(fixedRate = 30000)
  @SchedulerLock(
      name = "CompetitionStatusSync",
      lockAtLeastFor = "PT10S",
      lockAtMostFor = "PT30S"
  )
  public void syncCompetitionStatuses() {
    log.debug("开始竞赛状态同步任务");

    LocalDateTime now = LocalDateTime.now();
    int updated = statusService.synchronizeStatuses(now);

    if (updated > 0) {
      log.info("竞赛状态同步完成，更新了 {} 个竞赛", updated);
    } else {
      log.debug("竞赛状态同步完成，无需更新");
    }
  }
}
