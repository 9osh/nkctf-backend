package cn.edu.ndky.nkctf.service.impl;

import cn.edu.ndky.nkctf.dto.request.StartContainerRequest;
import cn.edu.ndky.nkctf.dto.response.ContainerInfoResponse;
import cn.edu.ndky.nkctf.entity.Challenge;
import cn.edu.ndky.nkctf.entity.Competition;
import cn.edu.ndky.nkctf.entity.User;
import cn.edu.ndky.nkctf.exception.BusinessException;
import cn.edu.ndky.nkctf.mapper.ChallengeMapper;
import cn.edu.ndky.nkctf.mapper.CompetitionMapper;
import cn.edu.ndky.nkctf.mapper.CompetitionTeamMapper;
import cn.edu.ndky.nkctf.mapper.CompetitionUserMapper;
import cn.edu.ndky.nkctf.mapper.UserMapper;
import cn.edu.ndky.nkctf.service.ContainerService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import cn.edu.ndky.nkctf.config.ChallengeContainerHostConfigBuilder;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 容器管理服务实现
 *
 * <h2>安全设计说明</h2>
 *
 * <h3>请求参数信任策略</h3>
 * <ul>
 *   <li><b>challengeId</b>: 资源标识符，可从请求接受</li>
 *   <li><b>competitionId</b>: 竞赛标识符，可从请求接受（需验证参赛资格）</li>
 *   <li><b>containerId</b>: 容器标识符，可从请求接受（需验证所有权）</li>
 *   <li><b>userId</b>: <b>绝不</b>从请求接受！必须从 JWT Token 中提取</li>
 * </ul>
 *
 * <h3>安全措施</h3>
 * <ul>
 *   <li>用户身份: 从 SecurityContext 获取，由 JWT Filter 解析并验证签名</li>
 *   <li>容器所有权: 通过 Redis 存储的 userId 字段验证</li>
 *   <li>条件竞争: 使用 Redis 分布式锁防止同一用户重复启动</li>
 *   <li>资源限制: Docker 容器内存和 CPU 限制</li>
 *   <li>数量限制: 普通用户 1 个，团队赛最多 3 个</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContainerServiceImpl implements ContainerService {

  // Redis Key 前缀
  private static final String CONTAINER_INFO_PREFIX = "nkctf:container:";
  private static final String USER_CONTAINERS_PREFIX = "nkctf:container:user:";
  private static final String TEAM_CONTAINERS_PREFIX = "nkctf:container:team:";
  private static final String CHALLENGE_USER_PREFIX = "nkctf:container:challenge:";
  private static final String LOCK_PREFIX = "nkctf:lock:container:";
  private static final String DYNAMIC_FLAG_PREFIX = "nkctf:flag:";

  // 容器信息 Hash 字段
  private static final String FIELD_CONTAINER_ID = "containerId";
  private static final String FIELD_CHALLENGE_ID = "challengeId";
  private static final String FIELD_CHALLENGE_TITLE = "challengeTitle";
  private static final String FIELD_USER_ID = "userId";
  private static final String FIELD_TEAM_ID = "teamId";
  private static final String FIELD_COMPETITION_ID = "competitionId";
  private static final String FIELD_FLAG = "flag";
  private static final String FIELD_HOST = "host";
  private static final String FIELD_PORT = "port";
  private static final String FIELD_CREATE_TIME = "createTime";
  private static final String FIELD_EXPIRE_TIME = "expireTime";
  private static final String FIELD_EXTEND_COUNT = "extendCount";

  private final DockerClient dockerClient;
  private final ChallengeContainerHostConfigBuilder challengeHostConfigBuilder;
  private final StringRedisTemplate redisTemplate;
  private final ChallengeMapper challengeMapper;
  private final CompetitionMapper competitionMapper;
  private final CompetitionUserMapper competitionUserMapper;
  private final CompetitionTeamMapper competitionTeamMapper;
  private final UserMapper userMapper;

  @Value("${docker.container.timeout:3600}")
  private Integer containerTimeout;

  @Value("${docker.container.host:localhost}")
  private String containerHost;

  @Value("${docker.limits.user-max-containers:1}")
  private Integer userMaxContainers;

  @Value("${docker.limits.team-max-containers:3}")
  private Integer teamMaxContainers;

  @Value("${docker.limits.max-extend-count:3}")
  private Integer maxExtendCount;

  @Override
  public ContainerInfoResponse startContainer(StartContainerRequest request) {
    // ========== 安全验证：用户身份 ==========
    User currentUser = getCurrentUser();
    Long userId = currentUser.getId();
    Long challengeId = request.getChallengeId();
    Long competitionId = request.getCompetitionId();

    // ========== 验证题目 ==========
    Challenge challenge = challengeMapper.selectById(challengeId);
    if (challenge == null) {
      throw new BusinessException(404, "题目不存在");
    }
    if (challenge.getDockerImage() == null || challenge.getDockerImage().isBlank()) {
      throw new BusinessException(400, "该题目不支持动态容器");
    }

    if (challenge.getDockerPort() != null
        && !ChallengeContainerHostConfigBuilder.isValidPort(challenge.getDockerPort())) {
      throw new BusinessException(400, "题目容器端口配置无效，须在 1-65535 之间");
    }
    final int containerPort =
        challengeHostConfigBuilder.resolveContainerPort(challenge.getDockerPort());

    // ========== 验证竞赛参赛资格 ==========
    Competition competition = null;
    boolean isTeamCompetition = false;
    UUID teamId = null;

    if (competitionId != null) {
      competition = competitionMapper.selectById(competitionId);
      if (competition == null) {
        throw new BusinessException(404, "竞赛不存在");
      }
      isTeamCompetition = Boolean.TRUE.equals(competition.getIsTeamCompetition());

      if (isTeamCompetition) {
        // 团队赛：验证用户所在队伍是否参赛
        teamId = currentUser.getTeamId();
        if (teamId == null) {
          throw new BusinessException(403, "你尚未加入任何队伍");
        }
        Boolean inCompetition = competitionTeamMapper.isTeamInCompetition(competitionId, teamId);
        if (!Boolean.TRUE.equals(inCompetition)) {
          throw new BusinessException(403, "你的队伍未报名参加该竞赛");
        }
      } else {
        // 个人赛：验证用户是否参赛
        Boolean inCompetition = competitionUserMapper.isUserInCompetition(competitionId, userId);
        if (!Boolean.TRUE.equals(inCompetition)) {
          throw new BusinessException(403, "你未报名参加该竞赛");
        }
      }
    }

    // ========== 分布式锁：防止条件竞争 ==========
    String lockKey = LOCK_PREFIX + challengeId + ":" + userId;
    if (!tryLock(lockKey, 10)) {
      throw new BusinessException(429, "操作过于频繁，请稍后重试");
    }

    try {
      // ========== 检查容器数量限制 ==========
      String challengeUserKey = CHALLENGE_USER_PREFIX + challengeId + ":user:" + userId;

      // 检查该用户是否已有此题目的容器
      String existingContainerId = redisTemplate.opsForValue().get(challengeUserKey);
      if (existingContainerId != null) {
        throw new BusinessException(400, "你已经启动了该题目的容器，请先关闭后再试");
      }

      if (competitionId != null && isTeamCompetition) {
        // 团队赛：检查队伍总容器数
        String teamContainersKey = TEAM_CONTAINERS_PREFIX + teamId + ":" + competitionId;
        Long teamContainerCount = redisTemplate.opsForSet().size(teamContainersKey);
        if (teamContainerCount != null && teamContainerCount >= teamMaxContainers) {
          throw new BusinessException(400,
              "队伍容器数量已达上限（" + teamMaxContainers + "个），请关闭其他容器后再试");
        }
      } else {
        // 普通模式或个人赛：检查用户容器数
        String userContainersKey = USER_CONTAINERS_PREFIX + userId;
        Long userContainerCount = redisTemplate.opsForSet().size(userContainersKey);
        if (userContainerCount != null && userContainerCount >= userMaxContainers) {
          throw new BusinessException(400,
              "容器数量已达上限（" + userMaxContainers + "个），请关闭其他容器后再试");
        }
      }

      // ========== 生成动态 Flag ==========
      String flag = "flag{" + UUID.randomUUID().toString().replace("-", "") + "}";

      // ========== 创建 Docker 容器 ==========
      String containerName = "nkctf-" + challengeId + "-" + userId + "-" + System.currentTimeMillis();

      CreateContainerResponse container = dockerClient.createContainerCmd(challenge.getDockerImage())
          .withName(containerName)
          .withEnv("FLAG=" + flag)
          .withExposedPorts(ExposedPort.tcp(containerPort))
          .withHostConfig(challengeHostConfigBuilder.build(containerPort))
          .exec();

      String containerId = container.getId();

      // 启动容器
      dockerClient.startContainerCmd(containerId).exec();

      // 获取映射端口
      InspectContainerResponse inspect = dockerClient.inspectContainerCmd(containerId).exec();
      Integer hostPort = extractHostPort(inspect, containerPort);

      // ========== 存储到 Redis ==========
      long now = System.currentTimeMillis();
      long expireTime = now + containerTimeout * 1000L;

      // 容器详情
      String containerInfoKey = CONTAINER_INFO_PREFIX + containerId;
      Map<String, String> containerInfo = new HashMap<>();
      containerInfo.put(FIELD_CONTAINER_ID, containerId);
      containerInfo.put(FIELD_CHALLENGE_ID, String.valueOf(challengeId));
      containerInfo.put(FIELD_CHALLENGE_TITLE, challenge.getTitle());
      containerInfo.put(FIELD_USER_ID, String.valueOf(userId));
      containerInfo.put(FIELD_TEAM_ID, teamId != null ? teamId.toString() : "");
      containerInfo.put(FIELD_COMPETITION_ID, competitionId != null ? String.valueOf(competitionId) : "");
      containerInfo.put(FIELD_FLAG, flag);
      containerInfo.put(FIELD_HOST, containerHost);
      containerInfo.put(FIELD_PORT, hostPort != null ? String.valueOf(hostPort) : "");
      containerInfo.put(FIELD_CREATE_TIME, String.valueOf(now));
      containerInfo.put(FIELD_EXPIRE_TIME, String.valueOf(expireTime));
      containerInfo.put(FIELD_EXTEND_COUNT, "0");

      redisTemplate.opsForHash().putAll(containerInfoKey, containerInfo);
      redisTemplate.expire(containerInfoKey, containerTimeout, TimeUnit.SECONDS);

      // 用户容器集合
      String userContainersKey = USER_CONTAINERS_PREFIX + userId;
      redisTemplate.opsForSet().add(userContainersKey, containerId);
      redisTemplate.expire(userContainersKey, containerTimeout, TimeUnit.SECONDS);

      // 题目-用户容器映射
      redisTemplate.opsForValue().set(challengeUserKey, containerId, containerTimeout, TimeUnit.SECONDS);

      // 团队容器集合（团队赛）
      if (competitionId != null && isTeamCompetition && teamId != null) {
        String teamContainersKey = TEAM_CONTAINERS_PREFIX + teamId + ":" + competitionId;
        redisTemplate.opsForSet().add(teamContainersKey, containerId);
        redisTemplate.expire(teamContainersKey, containerTimeout, TimeUnit.SECONDS);
      }

      // 动态 Flag 存储（用于验证提交）
      String flagKey = DYNAMIC_FLAG_PREFIX + challengeId + ":" + userId;
      redisTemplate.opsForValue().set(flagKey, flag, containerTimeout, TimeUnit.SECONDS);

      log.info("用户 {} 启动容器成功: challengeId={}, containerId={}, port={}",
          currentUser.getUsername(), challengeId, containerId, hostPort);

      return ContainerInfoResponse.builder()
          .containerId(containerId)
          .challengeId(challengeId)
          .challengeTitle(challenge.getTitle())
          .host(containerHost)
          .port(hostPort)
          .createTime(now)
          .expireTime(expireTime)
          .remainingSeconds((long) containerTimeout)
          .build();

    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      log.error("启动容器失败: challengeId={}, userId={}", challengeId, userId, e);
      throw new BusinessException(500, "启动容器失败: " + e.getMessage());
    } finally {
      unlock(lockKey);
    }
  }

  @Override
  public void stopContainer(String containerId) {
    // ========== 安全验证：用户身份 ==========
    User currentUser = getCurrentUser();
    Long userId = currentUser.getId();

    // ========== 分布式锁：防止并发销毁 ==========
    String lockKey = LOCK_PREFIX + "stop:" + containerId;
    if (!tryLock(lockKey, 10)) {
      throw new BusinessException(429, "操作过于频繁，请稍后再试");
    }

    try {
      // ========== 安全验证：容器所有权 ==========
      String containerInfoKey = CONTAINER_INFO_PREFIX + containerId;
      Map<Object, Object> containerInfo = redisTemplate.opsForHash().entries(containerInfoKey);

      if (containerInfo.isEmpty()) {
        throw new BusinessException(404, "容器不存在或已过期");
      }

      String ownerIdStr = (String) containerInfo.get(FIELD_USER_ID);
      if (ownerIdStr == null || !ownerIdStr.equals(String.valueOf(userId))) {
        log.warn("安全警告: 用户 {} 尝试关闭不属于自己的容器 {}", currentUser.getUsername(), containerId);
        throw new BusinessException(403, "无权操作此容器");
      }

      // ========== 停止并删除容器 ==========
      try {
        dockerClient.stopContainerCmd(containerId).withTimeout(10).exec();
      } catch (Exception e) {
        log.warn("停止容器失败（可能已停止）: {}", containerId, e);
      }

      try {
        dockerClient.removeContainerCmd(containerId).withForce(true).exec();
      } catch (Exception e) {
        log.warn("删除容器失败: {}", containerId, e);
      }

      // ========== 清理 Redis ==========
      cleanupContainerRedisData(containerId, containerInfo);

      log.info("用户 {} 销毁容器成功: containerId={}", currentUser.getUsername(), containerId);
    } finally {
      unlock(lockKey);
    }
  }

  @Override
  public ContainerInfoResponse extendContainer(String containerId) {
    // ========== 安全验证：用户身份 ==========
    User currentUser = getCurrentUser();
    Long userId = currentUser.getId();

    // ========== 分布式锁：防止并发延时 ==========
    String lockKey = LOCK_PREFIX + "extend:" + containerId;
    if (!tryLock(lockKey, 10)) {
      throw new BusinessException(429, "操作过于频繁，请稍后再试");
    }

    try {
      // ========== 安全验证：容器所有权 ==========
      String containerInfoKey = CONTAINER_INFO_PREFIX + containerId;
      Map<Object, Object> containerInfo = redisTemplate.opsForHash().entries(containerInfoKey);

      if (containerInfo.isEmpty()) {
        throw new BusinessException(404, "容器不存在或已过期");
      }

      String ownerIdStr = (String) containerInfo.get(FIELD_USER_ID);
      if (ownerIdStr == null || !ownerIdStr.equals(String.valueOf(userId))) {
        log.warn("安全警告: 用户 {} 尝试延时不属于自己的容器 {}", currentUser.getUsername(), containerId);
        throw new BusinessException(403, "无权操作此容器");
      }

      // ========== 检查延时次数限制 ==========
      String extendCountStr = (String) containerInfo.get(FIELD_EXTEND_COUNT);
      int extendCount = extendCountStr != null ? Integer.parseInt(extendCountStr) : 0;
      if (extendCount >= maxExtendCount) {
        throw new BusinessException(400,
            "该容器延时次数已达上限（" + maxExtendCount + "次），请关闭后重新启动");
      }

      // ========== 重置过期时间 ==========
      long now = System.currentTimeMillis();
      long newExpireTime = now + containerTimeout * 1000L;

      // 更新过期时间字段和延时次数
      redisTemplate.opsForHash().put(containerInfoKey, FIELD_EXPIRE_TIME, String.valueOf(newExpireTime));
      redisTemplate.opsForHash().put(containerInfoKey, FIELD_EXTEND_COUNT, String.valueOf(extendCount + 1));
      redisTemplate.expire(containerInfoKey, containerTimeout, TimeUnit.SECONDS);

      // 更新相关 Key 的 TTL
      String challengeId = (String) containerInfo.get(FIELD_CHALLENGE_ID);
      String teamIdStr = (String) containerInfo.get(FIELD_TEAM_ID);
      String competitionIdStr = (String) containerInfo.get(FIELD_COMPETITION_ID);

      String userContainersKey = USER_CONTAINERS_PREFIX + userId;
      redisTemplate.expire(userContainersKey, containerTimeout, TimeUnit.SECONDS);

      String challengeUserKey = CHALLENGE_USER_PREFIX + challengeId + ":user:" + userId;
      redisTemplate.expire(challengeUserKey, containerTimeout, TimeUnit.SECONDS);

      if (teamIdStr != null && !teamIdStr.isEmpty() && competitionIdStr != null && !competitionIdStr.isEmpty()) {
        String teamContainersKey = TEAM_CONTAINERS_PREFIX + teamIdStr + ":" + competitionIdStr;
        redisTemplate.expire(teamContainersKey, containerTimeout, TimeUnit.SECONDS);
      }

      // 更新动态 Flag TTL
      String flagKey = DYNAMIC_FLAG_PREFIX + challengeId + ":" + userId;
      redisTemplate.expire(flagKey, containerTimeout, TimeUnit.SECONDS);

      log.info("用户 {} 延时容器成功: containerId={}", currentUser.getUsername(), containerId);

      return ContainerInfoResponse.builder()
          .containerId(containerId)
          .challengeId(Long.parseLong(challengeId))
          .challengeTitle((String) containerInfo.get(FIELD_CHALLENGE_TITLE))
          .host((String) containerInfo.get(FIELD_HOST))
          .port(parseIntOrNull((String) containerInfo.get(FIELD_PORT)))
          .createTime(Long.parseLong((String) containerInfo.get(FIELD_CREATE_TIME)))
          .expireTime(newExpireTime)
          .remainingSeconds((long) containerTimeout)
          .build();
    } finally {
      unlock(lockKey);
    }
  }

  @Override
  public List<ContainerInfoResponse> listContainers() {
    // ========== 安全验证：用户身份 ==========
    User currentUser = getCurrentUser();
    Long userId = currentUser.getId();

    // ========== 获取用户的所有容器 ==========
    String userContainersKey = USER_CONTAINERS_PREFIX + userId;
    Set<String> containerIds = redisTemplate.opsForSet().members(userContainersKey);

    if (containerIds == null || containerIds.isEmpty()) {
      return new ArrayList<>();
    }

    List<ContainerInfoResponse> result = new ArrayList<>();
    long now = System.currentTimeMillis();

    for (String containerId : containerIds) {
      String containerInfoKey = CONTAINER_INFO_PREFIX + containerId;
      Map<Object, Object> containerInfo = redisTemplate.opsForHash().entries(containerInfoKey);

      if (containerInfo.isEmpty()) {
        // 容器已过期，从集合中移除
        redisTemplate.opsForSet().remove(userContainersKey, containerId);
        continue;
      }

      long expireTime = Long.parseLong((String) containerInfo.get(FIELD_EXPIRE_TIME));
      long remainingSeconds = Math.max(0, (expireTime - now) / 1000);

      result.add(ContainerInfoResponse.builder()
          .containerId(containerId)
          .challengeId(Long.parseLong((String) containerInfo.get(FIELD_CHALLENGE_ID)))
          .challengeTitle((String) containerInfo.get(FIELD_CHALLENGE_TITLE))
          .host((String) containerInfo.get(FIELD_HOST))
          .port(parseIntOrNull((String) containerInfo.get(FIELD_PORT)))
          .createTime(Long.parseLong((String) containerInfo.get(FIELD_CREATE_TIME)))
          .expireTime(expireTime)
          .remainingSeconds(remainingSeconds)
          .build());
    }

    return result;
  }

  // ==================== 私有方法 ====================

  /**
   * 获取当前登录用户
   */
  private User getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new BusinessException(401, "请先登录");
    }

    Object principal = authentication.getPrincipal();
    if (!(principal instanceof UserDetails userDetails)) {
      throw new BusinessException(401, "请先登录");
    }

    if ("anonymousUser".equals(userDetails.getUsername())) {
      throw new BusinessException(401, "请先登录");
    }

    User user = userMapper.selectOne(
        new LambdaQueryWrapper<User>()
            .eq(User::getUsername, userDetails.getUsername())
    );

    if (user == null) {
      throw new BusinessException(401, "用户不存在");
    }

    return user;
  }

  /**
   * 尝试获取分布式锁
   */
  private boolean tryLock(String key, long timeoutSeconds) {
    Boolean success = redisTemplate.opsForValue()
        .setIfAbsent(key, "1", Duration.ofSeconds(timeoutSeconds));
    return Boolean.TRUE.equals(success);
  }

  /**
   * 释放分布式锁
   */
  private void unlock(String key) {
    redisTemplate.delete(key);
  }

  /**
   * 从容器检查结果中提取指定容器端口映射的宿主机端口
   */
  private Integer extractHostPort(InspectContainerResponse inspect, int containerPort) {
    Ports ports = inspect.getNetworkSettings().getPorts();
    if (ports == null || ports.getBindings() == null) {
      return null;
    }

    ExposedPort exposed = ExposedPort.tcp(containerPort);
    Ports.Binding[] bindings = ports.getBindings().get(exposed);
    if (bindings == null || bindings.length == 0) {
      return null;
    }
    String hostPortSpec = bindings[0].getHostPortSpec();
    if (hostPortSpec == null || hostPortSpec.isEmpty()) {
      return null;
    }
    return Integer.parseInt(hostPortSpec);
  }

  /**
   * 清理容器相关的 Redis 数据
   */
  private void cleanupContainerRedisData(String containerId, Map<Object, Object> containerInfo) {
    String userId = (String) containerInfo.get(FIELD_USER_ID);
    String challengeId = (String) containerInfo.get(FIELD_CHALLENGE_ID);
    String teamIdStr = (String) containerInfo.get(FIELD_TEAM_ID);
    String competitionIdStr = (String) containerInfo.get(FIELD_COMPETITION_ID);

    // 删除容器详情
    redisTemplate.delete(CONTAINER_INFO_PREFIX + containerId);

    // 从用户容器集合中移除
    if (userId != null) {
      redisTemplate.opsForSet().remove(USER_CONTAINERS_PREFIX + userId, containerId);
    }

    // 删除题目-用户映射
    if (challengeId != null && userId != null) {
      redisTemplate.delete(CHALLENGE_USER_PREFIX + challengeId + ":user:" + userId);
    }

    // 从团队容器集合中移除
    if (teamIdStr != null && !teamIdStr.isEmpty()
        && competitionIdStr != null && !competitionIdStr.isEmpty()) {
      redisTemplate.opsForSet().remove(
          TEAM_CONTAINERS_PREFIX + teamIdStr + ":" + competitionIdStr, containerId);
    }

    // 删除动态 Flag
    if (challengeId != null && userId != null) {
      redisTemplate.delete(DYNAMIC_FLAG_PREFIX + challengeId + ":" + userId);
    }
  }

  /**
   * 安全解析整数
   */
  private Integer parseIntOrNull(String value) {
    if (value == null || value.isEmpty()) {
      return null;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
