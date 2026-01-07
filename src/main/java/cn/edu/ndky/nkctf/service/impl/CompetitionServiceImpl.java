package cn.edu.ndky.nkctf.service.impl;

import cn.edu.ndky.nkctf.dto.request.CompetitionSubmitFlagRequest;
import cn.edu.ndky.nkctf.dto.response.CompetitionChallengeDetailResponse;
import cn.edu.ndky.nkctf.dto.response.CompetitionDetailResponse;
import cn.edu.ndky.nkctf.dto.response.CompetitionLeaderboardResponse;
import cn.edu.ndky.nkctf.dto.response.CompetitionListItemResponse;
import cn.edu.ndky.nkctf.dto.response.CompetitionSubmitFlagResponse;
import cn.edu.ndky.nkctf.entity.*;
import cn.edu.ndky.nkctf.exception.BusinessException;
import cn.edu.ndky.nkctf.mapper.*;
import cn.edu.ndky.nkctf.service.CompetitionService;
import cn.edu.ndky.nkctf.service.CompetitionStatusService;
import cn.edu.ndky.nkctf.service.DynamicScoringService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 竞赛服务实现
 *
 * <h2>安全设计说明</h2>
 *
 * <h3>1. 身份验证</h3>
 * <ul>
 *   <li>用户身份通过 JWT Token 从 SecurityContext 获取</li>
 *   <li>不信任任何请求参数中的用户 ID 或队伍 ID</li>
 *   <li>所有敏感操作都从数据库重新查询用户信息</li>
 * </ul>
 *
 * <h3>2. 授权检查（团队赛）</h3>
 * <ul>
 *   <li>从数据库获取用户的 team_id（不信任请求参数）</li>
 *   <li>验证该队伍是否在 competition_team 表中有报名记录</li>
 *   <li>即使攻击者篡改请求，也无法绕过服务端的数据库验证</li>
 * </ul>
 *
 * <h3>3. 授权检查（个人赛）</h3>
 * <ul>
 *   <li>从 SecurityContext 获取用户 ID</li>
 *   <li>验证用户是否在 competition_user 表中有报名记录</li>
 * </ul>
 *
 * <h3>4. 防篡改措施</h3>
 * <ul>
 *   <li>用户 ID: 从 JWT 解析，JWT 由服务端签名，无法伪造</li>
 *   <li>队伍 ID: 从 sys_user.team_id 获取，不从请求读取</li>
 *   <li>参赛状态: 从 competition_team/competition_user 表验证</li>
 *   <li>竞赛状态: 从 competition 表实时查询</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompetitionServiceImpl implements CompetitionService {

  private final CompetitionMapper competitionMapper;
  private final CompetitionTeamMapper competitionTeamMapper;
  private final CompetitionUserMapper competitionUserMapper;
  private final CompetitionChallengeMapper competitionChallengeMapper;
  private final ChallengeMapper challengeMapper;
  private final SubmissionMapper submissionMapper;
  private final HintMapper hintMapper;
  private final HintUnlockMapper hintUnlockMapper;
  private final UserMapper userMapper;
  private final TeamMapper teamMapper;
  private final StringRedisTemplate stringRedisTemplate;
  private final DynamicScoringService dynamicScoringService;
  private final CompetitionStatusService competitionStatusService;

  private static final String DYNAMIC_FLAG_KEY_PREFIX = "nkctf:flag:";
  private static final String CONTAINER_CHALLENGE_USER_PREFIX = "nkctf:container:challenge:";
  private static final String LOCK_PREFIX = "nkctf:lock:cflag:";
  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  @Override
  public List<CompetitionListItemResponse> getCompetitionList() {
    User currentUser = getCurrentUserOrNull();

    List<Competition> competitions = competitionMapper.selectList(
        new LambdaQueryWrapper<Competition>().orderByDesc(Competition::getCreateTime));

    return competitions.stream()
        .map(comp -> buildCompetitionListItem(comp, currentUser))
        .collect(Collectors.toList());
  }

  @Override
  public CompetitionDetailResponse getCompetitionDetail(Long competitionId) {
    Competition competition = competitionMapper.selectById(competitionId);
    if (competition == null) {
      throw new BusinessException(404, "竞赛不存在");
    }

    User currentUser = getCurrentUserOrNull();
    boolean registered = isUserRegistered(competition, currentUser);

    // 只有已报名且竞赛状态为 active/ending 时才返回题目列表
    List<CompetitionDetailResponse.CompetitionChallengeItem> challenges = null;
    if (registered && isCompetitionViewable(competition)) {
      challenges = buildChallengeList(competition, currentUser);
    }

    return CompetitionDetailResponse.builder()
        .id(competition.getId())
        .name(competition.getName())
        .description(competition.getDescription())
        .isTeamCompetition(competition.getIsTeamCompetition())
        .status(competitionStatusService.getEffectiveStatus(competition).getValue())
        .startTime(formatDateTime(competition.getStartTime()))
        .endTime(formatDateTime(competition.getEndTime()))
        .participantCount(getParticipantCount(competition))
        .registered(registered)
        .challenges(challenges)
        .build();
  }

  @Override
  public CompetitionChallengeDetailResponse getCompetitionChallengeDetail(
      Long competitionId, Long challengeId) {

    // ========== 安全验证开始 ==========

    // 1. 获取竞赛信息
    Competition competition = competitionMapper.selectById(competitionId);
    if (competition == null) {
      throw new BusinessException(404, "竞赛不存在");
    }

    // 2. 验证竞赛状态（必须为 active 或 ending）
    if (!isCompetitionViewable(competition)) {
      throw new BusinessException(403, "竞赛尚未开始，无法查看题目");
    }

    // 3. 获取当前用户（从 JWT Token 中提取，不信任请求参数）
    User currentUser = getCurrentUser();

    // 4. 验证用户是否有权限查看该竞赛的题目
    verifyCompetitionAccess(competition, currentUser);

    // 5. 验证题目是否属于该竞赛
    Boolean challengeInCompetition = competitionChallengeMapper.isChallengeInCompetition(
        competitionId, challengeId);
    if (!Boolean.TRUE.equals(challengeInCompetition)) {
      throw new BusinessException(404, "题目不属于该竞赛");
    }

    // ========== 安全验证结束 ==========

    // 获取题目详情
    Challenge challenge = challengeMapper.selectById(challengeId);
    if (challenge == null) {
      throw new BusinessException(404, "题目不存在");
    }

    // 计算解题数和用户解题状态
    int solves;
    boolean solved;
    if (Boolean.TRUE.equals(competition.getIsTeamCompetition())) {
      // 团队赛：统计队伍解题数，检查当前队伍是否已解决
      solves = submissionMapper.countCompetitionChallengeSolvesByTeam(competitionId, challengeId);
      solved = Boolean.TRUE.equals(submissionMapper.hasTeamSolvedInCompetition(
          competitionId, currentUser.getTeamId(), challengeId));
    } else {
      // 个人赛：统计用户解题数，检查当前用户是否已解决
      solves = submissionMapper.countCompetitionChallengeSolvesByUser(competitionId, challengeId);
      solved = Boolean.TRUE.equals(submissionMapper.hasUserSolvedInCompetition(
          competitionId, currentUser.getId(), challengeId));
    }

    // 获取提示列表
    List<Hint> hints = hintMapper.findByChallengeId(challengeId);
    List<Long> unlockedHintIds = hintUnlockMapper.getUnlockedHintIds(currentUser.getId());
    Set<Long> unlockedSet = new HashSet<>(unlockedHintIds);

    List<CompetitionChallengeDetailResponse.HintResponse> hintResponses = hints.stream()
        .map(hint -> {
          boolean unlocked = unlockedSet.contains(hint.getId());
          return CompetitionChallengeDetailResponse.HintResponse.builder()
              .id(hint.getId())
              .cost(hint.getCost())
              .unlocked(unlocked)
              .content(unlocked ? hint.getContent() : null)
              .build();
        })
        .collect(Collectors.toList());

    // 构建附件列表
    List<CompetitionChallengeDetailResponse.AttachmentResponse> attachments = new ArrayList<>();
    if (challenge.getAttachmentUrl() != null && !challenge.getAttachmentUrl().isEmpty()) {
      attachments.add(CompetitionChallengeDetailResponse.AttachmentResponse.builder()
          .name(challenge.getAttachmentName())
          .url(challenge.getAttachmentUrl())
          .build());
    }

    log.info("用户 {} 查看竞赛 {} 的题目 {}",
        currentUser.getUsername(), competition.getName(), challenge.getTitle());

    // 计算动态积分当前值
    int currentPoints = dynamicScoringService.calculateCurrentPoints(challenge, solves);

    return CompetitionChallengeDetailResponse.builder()
        .competitionId(competitionId)
        .challengeId(challengeId)
        .title(challenge.getTitle())
        .description(challenge.getDescription())
        .category(challenge.getCategory())
        .difficulty(challenge.getDifficulty())
        .points(challenge.getPoints())
        .scoringType(challenge.getScoringType())
        .currentPoints(currentPoints)
        .maxPoints(challenge.getMaxPoints())
        .minPoints(challenge.getMinPoints())
        .decay(challenge.getDecay())
        .solves(solves)
        .solved(solved)
        .author(challenge.getAuthor())
        .content(challenge.getContent())
        .hints(hintResponses)
        .attachments(attachments)
        .hasDocker(challenge.getDockerImage() != null && !challenge.getDockerImage().isBlank())
        .build();
  }

  @Override
  @Transactional
  public void registerCompetition(Long competitionId) {
    User currentUser = getCurrentUser();

    Competition competition = competitionMapper.selectById(competitionId);
    if (competition == null) {
      throw new BusinessException(404, "竞赛不存在");
    }

    // 只有 inactive 状态的竞赛可以报名
    if (!Competition.Status.INACTIVE.getValue().equals(competition.getStatus())) {
      throw new BusinessException(400, "竞赛已开始或已结束，无法报名");
    }

    if (Boolean.TRUE.equals(competition.getIsTeamCompetition())) {
      // 团队赛：需要有队伍
      if (currentUser.getTeamId() == null) {
        throw new BusinessException(400, "请先加入或创建队伍后再报名团队赛");
      }

      // 检查是否已报名
      Boolean alreadyRegistered = competitionTeamMapper.isTeamInCompetition(
          competitionId, currentUser.getTeamId());
      if (Boolean.TRUE.equals(alreadyRegistered)) {
        throw new BusinessException(400, "您的队伍已报名该竞赛");
      }

      // 报名
      CompetitionTeam ct = new CompetitionTeam();
      ct.setCompetitionId(competitionId);
      ct.setTeamId(currentUser.getTeamId());
      ct.setScore(0);
      competitionTeamMapper.insert(ct);

      log.info("队伍 {} 报名了竞赛 {}", currentUser.getTeamId(), competition.getName());
    } else {
      // 个人赛
      Boolean alreadyRegistered = competitionUserMapper.isUserInCompetition(
          competitionId, currentUser.getId());
      if (Boolean.TRUE.equals(alreadyRegistered)) {
        throw new BusinessException(400, "您已报名该竞赛");
      }

      CompetitionUser cu = new CompetitionUser();
      cu.setCompetitionId(competitionId);
      cu.setUserId(currentUser.getId());
      cu.setScore(0);
      competitionUserMapper.insert(cu);

      log.info("用户 {} 报名了竞赛 {}", currentUser.getUsername(), competition.getName());
    }
  }

  /**
   * 验证用户是否有权限访问竞赛题目
   *
   * 安全说明：
   * - 用户身份从 JWT 中获取，无法伪造
   * - 团队 ID 从数据库中的 sys_user.team_id 获取，不信任请求参数
   * - 参赛记录从 competition_team/competition_user 表验证
   *
   * @param competition 竞赛
   * @param user 当前用户（从 SecurityContext 获取）
   * @throws BusinessException 如果用户无权访问
   */
  private void verifyCompetitionAccess(Competition competition, User user) {
    if (Boolean.TRUE.equals(competition.getIsTeamCompetition())) {
      // 团队赛验证
      // 1. 用户必须有队伍
      if (user.getTeamId() == null) {
        log.warn("安全警告: 用户 {} 尝试访问团队赛 {} 但没有队伍",
            user.getUsername(), competition.getName());
        throw new BusinessException(403, "您尚未加入队伍，无法查看团队赛题目");
      }

      // 2. 队伍必须已报名该竞赛（从数据库验证，不信任任何请求参数）
      Boolean isTeamRegistered = competitionTeamMapper.isTeamInCompetition(
          competition.getId(), user.getTeamId());
      if (!Boolean.TRUE.equals(isTeamRegistered)) {
        log.warn("安全警告: 用户 {} (队伍 {}) 尝试访问未报名的团队赛 {}",
            user.getUsername(), user.getTeamId(), competition.getName());
        throw new BusinessException(403, "您的队伍未报名该竞赛，无法查看题目");
      }

      log.debug("团队赛访问验证通过: 用户={}, 队伍={}, 竞赛={}",
          user.getUsername(), user.getTeamId(), competition.getName());
    } else {
      // 个人赛验证
      Boolean isUserRegistered = competitionUserMapper.isUserInCompetition(
          competition.getId(), user.getId());
      if (!Boolean.TRUE.equals(isUserRegistered)) {
        log.warn("安全警告: 用户 {} 尝试访问未报名的个人赛 {}",
            user.getUsername(), competition.getName());
        throw new BusinessException(403, "您未报名该竞赛，无法查看题目");
      }

      log.debug("个人赛访问验证通过: 用户={}, 竞赛={}",
          user.getUsername(), competition.getName());
    }
  }

  /**
   * 检查用户是否已报名竞赛
   */
  private boolean isUserRegistered(Competition competition, User user) {
    if (user == null) {
      return false;
    }

    if (Boolean.TRUE.equals(competition.getIsTeamCompetition())) {
      if (user.getTeamId() == null) {
        return false;
      }
      return Boolean.TRUE.equals(
          competitionTeamMapper.isTeamInCompetition(competition.getId(), user.getTeamId()));
    } else {
      return Boolean.TRUE.equals(
          competitionUserMapper.isUserInCompetition(competition.getId(), user.getId()));
    }
  }

  /**
   * 检查竞赛是否可查看（active 或 ending）
   * 使用实时计算的状态，而非数据库中的持久化状态
   */
  private boolean isCompetitionViewable(Competition competition) {
    return competitionStatusService.isCompetitionViewable(competition);
  }

  /**
   * 获取参赛人数/队伍数
   */
  private int getParticipantCount(Competition competition) {
    if (Boolean.TRUE.equals(competition.getIsTeamCompetition())) {
      Integer count = competitionTeamMapper.countTeamsByCompetition(competition.getId());
      return count != null ? count : 0;
    } else {
      Integer count = competitionUserMapper.countUsersByCompetition(competition.getId());
      return count != null ? count : 0;
    }
  }

  /**
   * 构建竞赛列表项
   * 使用实时计算的状态
   */
  private CompetitionListItemResponse buildCompetitionListItem(Competition comp, User user) {
    return CompetitionListItemResponse.builder()
        .id(comp.getId())
        .name(comp.getName())
        .description(comp.getDescription())
        .isTeamCompetition(comp.getIsTeamCompetition())
        .status(competitionStatusService.getEffectiveStatus(comp).getValue())
        .startTime(formatDateTime(comp.getStartTime()))
        .endTime(formatDateTime(comp.getEndTime()))
        .participantCount(getParticipantCount(comp))
        .registered(isUserRegistered(comp, user))
        .build();
  }

  /**
   * 构建竞赛题目列表
   */
  private List<CompetitionDetailResponse.CompetitionChallengeItem> buildChallengeList(
      Competition competition, User user) {

    List<Long> challengeIds = competitionChallengeMapper.getCompetitionChallengeIds(
        competition.getId());

    // 获取用户/队伍已解决的题目
    Set<Long> solvedIds;
    if (Boolean.TRUE.equals(competition.getIsTeamCompetition())) {
      List<Long> solved = submissionMapper.getTeamSolvedChallengeIdsInCompetition(
          competition.getId(), user.getTeamId());
      solvedIds = new HashSet<>(solved);
    } else {
      List<Long> solved = submissionMapper.getUserSolvedChallengeIdsInCompetition(
          competition.getId(), user.getId());
      solvedIds = new HashSet<>(solved);
    }

    return challengeIds.stream()
        .map(challengeId -> {
          Challenge challenge = challengeMapper.selectById(challengeId);
          if (challenge == null) {
            return null;
          }

          int solves;
          if (Boolean.TRUE.equals(competition.getIsTeamCompetition())) {
            solves = submissionMapper.countCompetitionChallengeSolvesByTeam(
                competition.getId(), challengeId);
          } else {
            solves = submissionMapper.countCompetitionChallengeSolvesByUser(
                competition.getId(), challengeId);
          }

          return CompetitionDetailResponse.CompetitionChallengeItem.builder()
              .id(challenge.getId())
              .title(challenge.getTitle())
              .description(challenge.getDescription())
              .category(challenge.getCategory())
              .difficulty(challenge.getDifficulty())
              .points(challenge.getPoints())
              .solves(solves)
              .solved(solvedIds.contains(challengeId))
              .build();
        })
        .filter(item -> item != null)
        .collect(Collectors.toList());
  }

  private String formatDateTime(java.time.LocalDateTime dateTime) {
    if (dateTime == null) {
      return null;
    }
    return dateTime.format(DATE_FORMATTER);
  }

  /**
   * 获取当前登录用户
   *
   * 安全说明：
   * - 用户身份从 SecurityContext 获取，而非请求参数
   * - SecurityContext 中的用户信息来自 JWT Token 解析
   * - JWT Token 由服务端签名，攻击者无法伪造或篡改
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

    // 从数据库查询最新的用户信息（包括 team_id）
    User user = userMapper.selectOne(
        new LambdaQueryWrapper<User>().eq(User::getUsername, userDetails.getUsername()));

    if (user == null) {
      throw new BusinessException(401, "用户不存在");
    }

    return user;
  }

  private User getCurrentUserOrNull() {
    try {
      return getCurrentUser();
    } catch (BusinessException e) {
      return null;
    }
  }

  // ========== 竞赛排行榜 ==========

  @Override
  public CompetitionLeaderboardResponse getCompetitionLeaderboard(Long competitionId) {
    // ========== 安全验证 ==========

    // 1. 获取竞赛信息
    Competition competition = competitionMapper.selectById(competitionId);
    if (competition == null) {
      throw new BusinessException(404, "竞赛不存在");
    }

    // 2. 验证竞赛状态（必须为 active 或 ending）
    if (!isCompetitionViewable(competition)) {
      throw new BusinessException(403, "竞赛尚未开始，无法查看排行榜");
    }

    // 3. 获取当前用户（从 JWT Token 中提取）
    User currentUser = getCurrentUser();

    // 4. 验证用户是否有权限查看排行榜
    verifyCompetitionAccess(competition, currentUser);

    // ========== 构建排行榜 ==========

    List<CompetitionLeaderboardResponse.LeaderboardEntry> entries;
    CompetitionLeaderboardResponse.LeaderboardEntry currentRank;

    if (Boolean.TRUE.equals(competition.getIsTeamCompetition())) {
      entries = buildTeamLeaderboard(competition);
      currentRank = buildCurrentTeamRank(competition, currentUser);
    } else {
      entries = buildUserLeaderboard(competition);
      currentRank = buildCurrentUserRank(competition, currentUser);
    }

    log.info("用户 {} 查看竞赛 {} 的排行榜", currentUser.getUsername(), competition.getName());

    return CompetitionLeaderboardResponse.builder()
        .competitionId(competitionId)
        .competitionName(competition.getName())
        .isTeamCompetition(competition.getIsTeamCompetition())
        .entries(entries)
        .currentRank(currentRank)
        .build();
  }

  /**
   * 构建团队赛排行榜
   */
  private List<CompetitionLeaderboardResponse.LeaderboardEntry> buildTeamLeaderboard(
      Competition competition) {
    List<UUID> teamIds = competitionTeamMapper.getCompetitionTeamIds(competition.getId());

    // 获取所有队伍信息并按分数排序
    List<CompetitionLeaderboardResponse.LeaderboardEntry> entries = teamIds.stream()
        .map(teamId -> {
          Team team = teamMapper.selectById(teamId);
          if (team == null) return null;

          Integer score = competitionTeamMapper.getTeamScore(competition.getId(), teamId);
          int solvedCount = submissionMapper.getTeamSolvedChallengeIdsInCompetition(
              competition.getId(), teamId).size();

          return CompetitionLeaderboardResponse.LeaderboardEntry.builder()
              .participantId(teamId.toString())
              .name(team.getName())
              .score(score != null ? score : 0)
              .solvedCount(solvedCount)
              .build();
        })
        .filter(e -> e != null)
        .sorted((a, b) -> {
          int scoreDiff = b.getScore() - a.getScore();
          if (scoreDiff != 0) return scoreDiff;
          return a.getSolvedCount() - b.getSolvedCount();
        })
        .collect(Collectors.toList());

    // 设置排名
    for (int i = 0; i < entries.size(); i++) {
      entries.get(i).setRank(i + 1);
    }

    return entries;
  }

  /**
   * 构建个人赛排行榜
   */
  private List<CompetitionLeaderboardResponse.LeaderboardEntry> buildUserLeaderboard(
      Competition competition) {
    List<Long> userIds = competitionUserMapper.getCompetitionUserIds(competition.getId());

    List<CompetitionLeaderboardResponse.LeaderboardEntry> entries = userIds.stream()
        .map(userId -> {
          User user = userMapper.selectById(userId);
          if (user == null) return null;

          Integer score = competitionUserMapper.getUserScore(competition.getId(), userId);
          int solvedCount = submissionMapper.getUserSolvedChallengeIdsInCompetition(
              competition.getId(), userId).size();

          return CompetitionLeaderboardResponse.LeaderboardEntry.builder()
              .participantId(String.valueOf(userId))
              .name(user.getNickname() != null ? user.getNickname() : user.getUsername())
              .avatar(user.getAvatar())
              .score(score != null ? score : 0)
              .solvedCount(solvedCount)
              .build();
        })
        .filter(e -> e != null)
        .sorted((a, b) -> {
          int scoreDiff = b.getScore() - a.getScore();
          if (scoreDiff != 0) return scoreDiff;
          return a.getSolvedCount() - b.getSolvedCount();
        })
        .collect(Collectors.toList());

    for (int i = 0; i < entries.size(); i++) {
      entries.get(i).setRank(i + 1);
    }

    return entries;
  }

  /**
   * 构建当前队伍的排名信息
   */
  private CompetitionLeaderboardResponse.LeaderboardEntry buildCurrentTeamRank(
      Competition competition, User user) {
    Team team = teamMapper.selectById(user.getTeamId());
    if (team == null) return null;

    Integer score = competitionTeamMapper.getTeamScore(competition.getId(), user.getTeamId());
    Integer rank = competitionTeamMapper.getTeamRank(competition.getId(), user.getTeamId());
    int solvedCount = submissionMapper.getTeamSolvedChallengeIdsInCompetition(
        competition.getId(), user.getTeamId()).size();

    return CompetitionLeaderboardResponse.LeaderboardEntry.builder()
        .rank(rank != null ? rank : 0)
        .participantId(user.getTeamId().toString())
        .name(team.getName())
        .score(score != null ? score : 0)
        .solvedCount(solvedCount)
        .build();
  }

  /**
   * 构建当前用户的排名信息
   */
  private CompetitionLeaderboardResponse.LeaderboardEntry buildCurrentUserRank(
      Competition competition, User user) {
    Integer score = competitionUserMapper.getUserScore(competition.getId(), user.getId());
    Integer rank = competitionUserMapper.getUserRank(competition.getId(), user.getId());
    int solvedCount = submissionMapper.getUserSolvedChallengeIdsInCompetition(
        competition.getId(), user.getId()).size();

    return CompetitionLeaderboardResponse.LeaderboardEntry.builder()
        .rank(rank != null ? rank : 0)
        .participantId(String.valueOf(user.getId()))
        .name(user.getNickname() != null ? user.getNickname() : user.getUsername())
        .avatar(user.getAvatar())
        .score(score != null ? score : 0)
        .solvedCount(solvedCount)
        .build();
  }

  // ========== 竞赛 Flag 提交 ==========

  /**
   * 提交竞赛 Flag
   *
   * 安全设计：
   * - 用户身份从 JWT Token 获取，不信任请求中的任何用户/队伍 ID
   * - 团队赛：从数据库获取用户的 team_id，验证队伍参赛状态
   * - 个人赛：验证用户参赛状态
   * - 只有 active 状态才计分
   * - 提交记录中的 userId/teamId 使用服务端获取的值
   */
  @Override
  @Transactional
  public CompetitionSubmitFlagResponse submitCompetitionFlag(CompetitionSubmitFlagRequest request) {
    // ========== 安全验证：用户身份 ==========
    User currentUser = getCurrentUser();

    // ========== 安全验证：竞赛信息 ==========
    Competition competition = competitionMapper.selectById(request.getCompetitionId());
    if (competition == null) {
      throw new BusinessException(404, "竞赛不存在");
    }

    // 验证竞赛状态（active 或 ending 可提交）
    if (!isCompetitionViewable(competition)) {
      throw new BusinessException(403, "竞赛尚未开始，无法提交 Flag");
    }

    // 验证用户参赛权限
    verifyCompetitionAccess(competition, currentUser);

    // ========== 安全验证：题目信息 ==========
    Boolean challengeInCompetition = competitionChallengeMapper.isChallengeInCompetition(
        request.getCompetitionId(), request.getChallengeId());
    if (!Boolean.TRUE.equals(challengeInCompetition)) {
      throw new BusinessException(404, "题目不属于该竞赛");
    }

    Challenge challenge = challengeMapper.selectById(request.getChallengeId());
    if (challenge == null) {
      throw new BusinessException(404, "题目不存在");
    }

    // ========== 分布式锁：防止并发重复计分 ==========
    // 团队赛：按队伍+题目加锁，防止队员同时提交
    // 个人赛：按用户+题目加锁，防止重复提交
    String lockKey;
    if (Boolean.TRUE.equals(competition.getIsTeamCompetition())) {
      lockKey = LOCK_PREFIX + competition.getId() + ":" + currentUser.getTeamId()
          + ":" + challenge.getId();
    } else {
      lockKey = LOCK_PREFIX + competition.getId() + ":" + currentUser.getId()
          + ":" + challenge.getId();
    }
    if (!tryLock(lockKey, 10)) {
      throw new BusinessException(429, "操作过于频繁，请稍后再试");
    }

    try {
      // ========== 检查是否已解决（用于判断是否计分，不阻止重复提交） ==========
      boolean alreadySolved;
      if (Boolean.TRUE.equals(competition.getIsTeamCompetition())) {
        alreadySolved = Boolean.TRUE.equals(submissionMapper.hasTeamSolvedInCompetition(
            competition.getId(), currentUser.getTeamId(), challenge.getId()));
      } else {
        alreadySolved = Boolean.TRUE.equals(submissionMapper.hasUserSolvedInCompetition(
            competition.getId(), currentUser.getId(), challenge.getId()));
      }

      // 动态题目需要检查容器是否活跃
      if (Boolean.TRUE.equals(challenge.getIsDynamic())) {
        String challengeUserKey = CONTAINER_CHALLENGE_USER_PREFIX
            + challenge.getId() + ":user:" + currentUser.getId();
        String containerId = stringRedisTemplate.opsForValue().get(challengeUserKey);
        if (containerId == null) {
          return CompetitionSubmitFlagResponse.builder()
              .correct(false)
              .message("容器不存在或已过期，请重新启动容器后再提交")
              .totalScore(getParticipantScore(competition, currentUser))
              .rank(getParticipantRank(competition, currentUser))
              .scored(false)
              .build();
        }
      }

      // ========== 验证 Flag ==========
      String submittedFlag = request.getFlag().trim();
      String correctFlag = getCorrectFlag(challenge, currentUser.getId());
      boolean isCorrect = correctFlag != null && correctFlag.equals(submittedFlag);

      // 判断是否计分（只有 active 状态且首次正确提交才计分）
      // 使用实时计算的状态，确保准确性
      boolean isActive = competitionStatusService.isCompetitionScorable(competition);
      boolean shouldScore = isCorrect && isActive && !alreadySolved;

      // ========== 创建提交记录 ==========
      // 注意：数据库有唯一索引 idx_submission_competition_correct_unique 限制
      // 同一队伍/用户在同一竞赛中对同一题目只能有一条 is_correct=true 的记录，因此：
      // - 首次正确提交：插入 is_correct=true 的记录
      // - 重复正确提交：跳过插入（已有正确记录）
      // - 错误提交：始终插入 is_correct=false 的记录
      Submission submission = null;
      if (!isCorrect || !alreadySolved) {
        submission = new Submission();
        submission.setUserId(currentUser.getId());  // 从 JWT 获取
        submission.setChallengeId(challenge.getId());
        submission.setCompetitionId(competition.getId());
        submission.setFlag(submittedFlag);
        submission.setIsCorrect(isCorrect);

        if (Boolean.TRUE.equals(competition.getIsTeamCompetition())) {
          submission.setTeamId(currentUser.getTeamId());  // 从数据库获取
        }
      }

      // ========== 动态积分计算 ==========
      int pointsAwarded = 0;
      int firstBloodBonus = 0;
      Integer firstBloodRank = null;

      if (shouldScore) {
        boolean isTeamCompetition = Boolean.TRUE.equals(competition.getIsTeamCompetition());

        // 获取当前解题排名（在插入新提交之前）
        int solveRank = dynamicScoringService.getNextSolveRank(
            competition.getId(), challenge.getId(), isTeamCompetition);

        // 计算积分
        if (dynamicScoringService.isDynamicScoring(challenge)) {
          // 动态积分：根据当前解题数计算分值
          int currentSolves = solveRank - 1;  // 当前解题数（不含本次）
          pointsAwarded = dynamicScoringService.calculateCurrentPoints(challenge, currentSolves + 1);
          firstBloodBonus = dynamicScoringService.calculateFirstBloodBonus(pointsAwarded, solveRank);
          if (solveRank <= 3) {
            firstBloodRank = solveRank;
          }

          log.info("动态积分: 竞赛={}, 题目={}, 排名={}, 基础分={}, 一血奖励={}",
              competition.getName(), challenge.getTitle(), solveRank, pointsAwarded, firstBloodBonus);
        } else {
          // 静态积分：使用固定分值
          pointsAwarded = challenge.getPoints() != null ? challenge.getPoints() : 0;
          // 静态积分也支持一血奖励
          firstBloodBonus = dynamicScoringService.calculateFirstBloodBonus(pointsAwarded, solveRank);
          if (solveRank <= 3) {
            firstBloodRank = solveRank;
          }
        }

        int totalPoints = pointsAwarded + firstBloodBonus;

        // 更新参赛者分数
        if (isTeamCompetition) {
          competitionTeamMapper.addTeamScore(
              competition.getId(), currentUser.getTeamId(), totalPoints);
          log.info("竞赛 {} 中队伍 {} 解决了题目 {}，获得 {} 分 (基础: {}, 一血: {})",
              competition.getName(), currentUser.getTeamId(),
              challenge.getTitle(), totalPoints, pointsAwarded, firstBloodBonus);
        } else {
          competitionUserMapper.addUserScore(
              competition.getId(), currentUser.getId(), totalPoints);
          log.info("竞赛 {} 中用户 {} 解决了题目 {}，获得 {} 分 (基础: {}, 一血: {})",
              competition.getName(), currentUser.getUsername(),
              challenge.getTitle(), totalPoints, pointsAwarded, firstBloodBonus);
        }

        // 动态积分需要重新计算之前解题者的积分
        if (dynamicScoringService.isDynamicScoring(challenge) && solveRank > 1) {
          dynamicScoringService.recalculateChallengeScores(
              competition.getId(), challenge.getId(), isTeamCompetition);
        }
      }

      // 只有在创建了提交记录时才插入
      if (submission != null) {
        submission.setPointsAwarded(pointsAwarded);
        submission.setFirstBloodRank(firstBloodRank);
        submission.setFirstBloodBonus(firstBloodBonus);
        submissionMapper.insert(submission);
      }

      // ========== 构建响应 ==========
      Integer totalScore = getParticipantScore(competition, currentUser);
      Integer rank = getParticipantRank(competition, currentUser);

      if (isCorrect) {
        String message;
        if (alreadySolved) {
          message = "Flag 正确！该题目已被解决";
        } else if (isActive) {
          message = "恭喜你，Flag 正确！";
        } else {
          message = "Flag 正确，但竞赛已结束，不计分。";
        }

        int totalPointsAwarded = pointsAwarded + firstBloodBonus;

        return CompetitionSubmitFlagResponse.builder()
            .correct(true)
            .pointsAwarded(shouldScore ? totalPointsAwarded : 0)
            .message(message)
            .totalScore(totalScore)
            .rank(rank)
            .scored(shouldScore)
            .build();
      } else {
        return CompetitionSubmitFlagResponse.builder()
            .correct(false)
            .message("Flag 错误，请再试一次")
            .totalScore(totalScore)
            .rank(rank)
            .scored(false)
            .build();
      }
    } finally {
      unlock(lockKey);
    }
  }

  /**
   * 获取参赛者当前分数
   */
  private Integer getParticipantScore(Competition competition, User user) {
    if (Boolean.TRUE.equals(competition.getIsTeamCompetition())) {
      return competitionTeamMapper.getTeamScore(competition.getId(), user.getTeamId());
    } else {
      return competitionUserMapper.getUserScore(competition.getId(), user.getId());
    }
  }

  /**
   * 获取参赛者当前排名
   */
  private Integer getParticipantRank(Competition competition, User user) {
    if (Boolean.TRUE.equals(competition.getIsTeamCompetition())) {
      return competitionTeamMapper.getTeamRank(competition.getId(), user.getTeamId());
    } else {
      return competitionUserMapper.getUserRank(competition.getId(), user.getId());
    }
  }

  /**
   * 获取正确的 Flag
   *
   * 安全说明：
   * - 静态 Flag: 从数据库获取
   * - 动态 Flag: 从 Redis 获取，key 包含用户 ID（从 JWT 获取）
   */
  private String getCorrectFlag(Challenge challenge, Long userId) {
    if (Boolean.TRUE.equals(challenge.getIsDynamic())) {
      String key = DYNAMIC_FLAG_KEY_PREFIX + challenge.getId() + ":" + userId;
      return stringRedisTemplate.opsForValue().get(key);
    } else {
      return challenge.getFlag();
    }
  }

  /**
   * 尝试获取分布式锁
   */
  private boolean tryLock(String key, long timeoutSeconds) {
    Boolean success = stringRedisTemplate.opsForValue()
        .setIfAbsent(key, "1", Duration.ofSeconds(timeoutSeconds));
    return Boolean.TRUE.equals(success);
  }

  /**
   * 释放分布式锁
   */
  private void unlock(String key) {
    stringRedisTemplate.delete(key);
  }
}
