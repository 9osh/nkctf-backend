package cn.edu.ndky.nkctf.service.impl;

import cn.edu.ndky.nkctf.dto.response.UserProfileResponse;
import cn.edu.ndky.nkctf.dto.response.UserProfileResponse.ActivityData;
import cn.edu.ndky.nkctf.dto.response.UserProfileResponse.UserTeam;
import cn.edu.ndky.nkctf.entity.Team;
import cn.edu.ndky.nkctf.entity.User;
import cn.edu.ndky.nkctf.exception.BusinessException;
import cn.edu.ndky.nkctf.mapper.ChallengeMapper;
import cn.edu.ndky.nkctf.mapper.SubmissionMapper;
import cn.edu.ndky.nkctf.mapper.TeamMapper;
import cn.edu.ndky.nkctf.mapper.UserMapper;
import cn.edu.ndky.nkctf.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserMapper userMapper;
  private final TeamMapper teamMapper;
  private final SubmissionMapper submissionMapper;
  private final ChallengeMapper challengeMapper;

  @Override
  public UserProfileResponse getUserProfile(Long userId) {
    User user = userMapper.selectById(userId);
    if (user == null) {
      throw new BusinessException(404, "用户不存在");
    }
    return buildUserProfile(user);
  }

  @Override
  public UserProfileResponse getCurrentUserProfile() {
    Long userId = getCurrentUserId();
    return getUserProfile(userId);
  }

  private UserProfileResponse buildUserProfile(User user) {
    // 获取团队信息
    UserTeam team = null;
    if (user.getTeamId() != null) {
      Team teamEntity = teamMapper.selectById(user.getTeamId());
      if (teamEntity != null) {
        // 判断用户在队伍中的角色
        String role = user.getId().equals(teamEntity.getCaptainId())
            ? "CAPTAIN"
            : "MEMBER";
        team = UserTeam.builder()
            .id(teamEntity.getId().toString())
            .name(teamEntity.getName())
            .role(role)
            .build();
      }
    }

    // 获取解题数量
    Integer solvedCount = submissionMapper.countCorrectSolves(user.getId());
    if (solvedCount == null) {
      solvedCount = 0;
    }

    // 获取用户排名
    Integer rank = userMapper.getUserRank(user.getId());
    if (rank == null) {
      rank = 0;
    }

    // 获取分类解题统计
    Map<String, Integer> solveStats = getSolveStats(user.getId());

    // 获取各分类题目总数
    Map<String, Integer> categoryTotals = getCategoryTotals();

    // 获取活动数据
    List<ActivityData> activityData = getActivityData(user.getId());

    // 格式化加入时间
    String joinedAt = user.getCreateTime() != null
        ? user.getCreateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        : null;

    return UserProfileResponse.builder()
        .id(user.getId())
        .username(user.getUsername())
        .nickname(user.getNickname())
        .avatar(user.getAvatar())
        .bio(user.getBio())
        .points(user.getScore() != null ? user.getScore() : 0)
        .rank(rank)
        .solvedCount(solvedCount)
        .team(team)
        .solveStats(solveStats)
        .categoryTotals(categoryTotals)
        .activityData(activityData)
        .joinedAt(joinedAt)
        .build();
  }

  private Map<String, Integer> getSolveStats(Long userId) {
    List<Map<String, Object>> stats = submissionMapper.countSolvesByCategory(userId);
    Map<String, Integer> result = new HashMap<>();

    // 初始化所有分类为 0
    result.put("web", 0);
    result.put("pwn", 0);
    result.put("crypto", 0);
    result.put("reverse", 0);
    result.put("misc", 0);
    result.put("blockchain", 0);

    // 填充实际数据
    for (Map<String, Object> stat : stats) {
      String category = ((String) stat.get("category")).toLowerCase();
      Long count = (Long) stat.get("count");
      result.put(category, count.intValue());
    }

    return result;
  }

  private Map<String, Integer> getCategoryTotals() {
    List<Map<String, Object>> totals = challengeMapper.countByCategory();
    Map<String, Integer> result = new HashMap<>();

    // 初始化所有分类为 0
    result.put("web", 0);
    result.put("pwn", 0);
    result.put("crypto", 0);
    result.put("reverse", 0);
    result.put("misc", 0);
    result.put("blockchain", 0);

    // 填充实际数据
    for (Map<String, Object> total : totals) {
      String category = ((String) total.get("category")).toLowerCase();
      Long count = (Long) total.get("count");
      result.put(category, count.intValue());
    }

    return result;
  }

  private List<ActivityData> getActivityData(Long userId) {
    List<Map<String, Object>> dailySolves = submissionMapper.countDailySolves(userId);
    return dailySolves.stream()
        .map(data -> ActivityData.builder()
            .date(data.get("date").toString())
            .count(((Long) data.get("count")).intValue())
            .build())
        .collect(Collectors.toList());
  }

  private Long getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new BusinessException(401, "用户未登录");
    }

    Object principal = authentication.getPrincipal();
    if (principal instanceof UserDetails userDetails) {
      String username = userDetails.getUsername();
      User user = userMapper.selectOne(
          new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
              .eq(User::getUsername, username)
      );
      if (user == null) {
        throw new BusinessException(401, "用户不存在");
      }
      return user.getId();
    }

    throw new BusinessException(401, "无法获取用户信息");
  }

  @Override
  public void updateBio(String bio) {
    Long userId = getCurrentUserId();
    User user = new User();
    user.setId(userId);
    user.setBio(bio);
    userMapper.updateById(user);
    log.info("用户 {} 更新个性签名成功", userId);
  }
}
