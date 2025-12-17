package cn.edu.ndky.nkctf.scheduler;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 容器清理定时任务
 *
 * <p>作为 Redis TTL 自动过期的备份机制，定期清理已过期但仍在运行的容器。</p>
 *
 * <h3>清理策略</h3>
 * <ul>
 *   <li>每 5 分钟执行一次</li>
 *   <li>扫描所有以 "nkctf-" 开头的容器</li>
 *   <li>检查 Redis 中是否有对应的容器记录</li>
 *   <li>若无记录（已过期），则强制停止并删除容器</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContainerCleanupScheduler {

  // 挑战容器命名格式: nkctf-{challengeId}-{userId}-{timestamp}
  private static final String CHALLENGE_CONTAINER_PATTERN = "^nkctf-\\d+-\\d+-\\d+$";
  private static final String CONTAINER_INFO_PREFIX = "nkctf:container:";

  private final DockerClient dockerClient;
  private final StringRedisTemplate redisTemplate;

  /**
   * 定期清理过期容器
   *
   * <p>每 5 分钟执行一次，清理 Redis 记录已过期但容器仍在运行的情况。</p>
   */
  @Scheduled(fixedRate = 300000) // 5 分钟
  public void cleanupExpiredContainers() {
    log.debug("开始执行容器清理任务...");

    try {
      // 获取所有运行中的容器
      List<Container> containers = dockerClient.listContainersCmd()
          .withShowAll(true)
          .exec();

      int cleanedCount = 0;

      for (Container container : containers) {
        String[] names = container.getNames();
        if (names == null || names.length == 0) {
          continue;
        }

        // 检查是否是平台创建的挑战容器（排除基础设施容器如 nkctf-postgres, nkctf-redis）
        String containerName = names[0].startsWith("/") ? names[0].substring(1) : names[0];
        if (!containerName.matches(CHALLENGE_CONTAINER_PATTERN)) {
          continue;
        }

        String containerId = container.getId();

        // 检查 Redis 中是否有记录
        String containerInfoKey = CONTAINER_INFO_PREFIX + containerId;
        Boolean exists = redisTemplate.hasKey(containerInfoKey);

        if (!Boolean.TRUE.equals(exists)) {
          // Redis 记录不存在，说明已过期，清理容器
          log.info("清理过期容器: name={}, id={}", containerName, containerId);

          try {
            // 停止容器
            if ("running".equals(container.getState())) {
              dockerClient.stopContainerCmd(containerId).withTimeout(5).exec();
            }
            // 删除容器
            dockerClient.removeContainerCmd(containerId).withForce(true).exec();
            cleanedCount++;
          } catch (Exception e) {
            log.warn("清理容器失败: id={}, error={}", containerId, e.getMessage());
          }
        }
      }

      if (cleanedCount > 0) {
        log.info("容器清理任务完成，清理了 {} 个过期容器", cleanedCount);
      } else {
        log.debug("容器清理任务完成，无需清理");
      }

    } catch (Exception e) {
      log.error("容器清理任务执行失败", e);
    }
  }
}
