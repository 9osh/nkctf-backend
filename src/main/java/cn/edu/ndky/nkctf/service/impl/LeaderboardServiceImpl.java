package cn.edu.ndky.nkctf.service.impl;

import cn.edu.ndky.nkctf.dto.response.LeaderboardEntryResponse;
import cn.edu.ndky.nkctf.dto.response.LeaderboardResponse;
import cn.edu.ndky.nkctf.entity.User;
import cn.edu.ndky.nkctf.mapper.UserMapper;
import cn.edu.ndky.nkctf.service.LeaderboardService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 排行榜服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardServiceImpl implements LeaderboardService {

  /**
   * 每页大小（固定值）
   */
  private static final int PAGE_SIZE = 50;

  /**
   * 缓存 Key 前缀
   */
  private static final String CACHE_KEY_PREFIX = "nkctf:leaderboard:";

  /**
   * 缓存过期时间（分钟）
   */
  private static final long CACHE_TTL_MINUTES = 5;

  private final UserMapper userMapper;
  private final RedisTemplate<String, LeaderboardResponse> leaderboardRedisTemplate;

  @Override
  public LeaderboardResponse getLeaderboard(int page) {
    int pageNum = Math.max(1, page);
    String cacheKey = CACHE_KEY_PREFIX + "page:" + pageNum;

    // 尝试从缓存获取
    LeaderboardResponse cached = getFromCache(cacheKey);
    if (cached != null) {
      log.debug("排行榜缓存命中: page={}", pageNum);
      // 缓存命中，但需要更新当前用户排名
      cached.setCurrentUserRank(getCurrentUserRank());
      return cached;
    }

    log.debug("排行榜缓存未命中，从数据库查询: page={}", pageNum);

    // 从数据库查询
    int offset = (pageNum - 1) * PAGE_SIZE;
    Long total = userMapper.countLeaderboardUsers();
    int pages = (int) Math.ceil((double) total / PAGE_SIZE);

    List<Map<String, Object>> rawEntries = userMapper.getLeaderboardPage(PAGE_SIZE, offset);
    List<LeaderboardEntryResponse> entries = rawEntries.stream()
        .map(this::mapToEntry)
        .toList();

    LeaderboardResponse response = LeaderboardResponse.builder()
        .entries(entries)
        .total(total)
        .page(pageNum)
        .pages(pages)
        .hasNext(pageNum < pages)
        .hasPrevious(pageNum > 1)
        .currentUserRank(null) // 缓存中不存储当前用户排名
        .build();

    // 存入缓存
    saveToCache(cacheKey, response);

    // 设置当前用户排名
    response.setCurrentUserRank(getCurrentUserRank());

    return response;
  }

  /**
   * 清除排行榜缓存（解题成功后调用）
   */
  @Override
  public void invalidateCache() {
    try {
      var keys = leaderboardRedisTemplate.keys(CACHE_KEY_PREFIX + "*");
      if (keys != null && !keys.isEmpty()) {
        leaderboardRedisTemplate.delete(keys);
        log.info("排行榜缓存已清除，共 {} 个 key", keys.size());
      }
    } catch (Exception e) {
      log.warn("清除排行榜缓存失败: {}", e.getMessage());
    }
  }

  /**
   * 用户解题成功后更新排行榜相关数据
   */
  @Override
  public void onUserSolved(Long userId) {
    // 1. 更新用户的冗余字段
    userMapper.updateLeaderboardFields(userId);
    log.debug("已更新用户 {} 的排行榜冗余字段", userId);

    // 2. 清除排行榜缓存
    invalidateCache();
  }

  /**
   * 从缓存获取
   */
  private LeaderboardResponse getFromCache(String key) {
    try {
      return leaderboardRedisTemplate.opsForValue().get(key);
    } catch (Exception e) {
      log.warn("读取排行榜缓存失败: {}", e.getMessage());
    }
    return null;
  }

  /**
   * 存入缓存
   */
  private void saveToCache(String key, LeaderboardResponse response) {
    try {
      leaderboardRedisTemplate.opsForValue().set(key, response, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
    } catch (Exception e) {
      log.warn("写入排行榜缓存失败: {}", e.getMessage());
    }
  }

  /**
   * 将数据库返回的 Map 转换为 LeaderboardEntryResponse
   */
  private LeaderboardEntryResponse mapToEntry(Map<String, Object> row) {
    return LeaderboardEntryResponse.builder()
        .rank(((Number) row.get("rank")).intValue())
        .userId(((Number) row.get("user_id")).longValue())
        .nickname((String) row.get("nickname"))
        .avatar((String) row.get("avatar"))
        .points(((Number) row.get("points")).intValue())
        .solvedCount(((Number) row.get("solved_count")).intValue())
        .lastSubmitTime(formatDateTime(row.get("last_submit_time")))
        .build();
  }

  /**
   * 格式化日期时间
   */
  private String formatDateTime(Object dateTime) {
    if (dateTime == null) {
      return null;
    }
    if (dateTime instanceof LocalDateTime ldt) {
      return ldt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
    return dateTime.toString();
  }

  /**
   * 获取当前登录用户的排名信息
   */
  private LeaderboardEntryResponse getCurrentUserRank() {
    Long userId = getCurrentUserIdOrNull();
    if (userId == null) {
      return null;
    }

    // 用户排名不缓存，实时查询
    Map<String, Object> entry = userMapper.getUserLeaderboardEntry(userId);
    if (entry == null) {
      return null;
    }

    return mapToEntry(entry);
  }

  /**
   * 获取当前用户 ID，未登录返回 null
   */
  private Long getCurrentUserIdOrNull() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return null;
    }

    Object principal = authentication.getPrincipal();
    if (principal instanceof UserDetails userDetails) {
      if ("anonymousUser".equals(userDetails.getUsername())) {
        return null;
      }
      User user = userMapper.selectOne(
          new LambdaQueryWrapper<User>()
              .eq(User::getUsername, userDetails.getUsername())
      );
      return user != null ? user.getId() : null;
    }

    return null;
  }
}
