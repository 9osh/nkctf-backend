package cn.edu.ndky.nkctf.service.impl;

import cn.edu.ndky.nkctf.dto.request.SubmitFlagRequest;
import cn.edu.ndky.nkctf.dto.response.SubmitFlagResponse;
import cn.edu.ndky.nkctf.entity.Challenge;
import cn.edu.ndky.nkctf.entity.Submission;
import cn.edu.ndky.nkctf.entity.User;
import cn.edu.ndky.nkctf.exception.BusinessException;
import cn.edu.ndky.nkctf.mapper.ChallengeMapper;
import cn.edu.ndky.nkctf.mapper.SubmissionMapper;
import cn.edu.ndky.nkctf.mapper.UserMapper;
import cn.edu.ndky.nkctf.service.SubmissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 提交服务实现
 *
 * <h2>安全设计说明</h2>
 *
 * <h3>请求参数信任策略</h3>
 * <ul>
 *   <li><b>challengeId</b>: 资源标识符，可从请求接受。用于标识用户要提交的题目。</li>
 *   <li><b>flag</b>: 用户提交的答案，可从请求接受。</li>
 *   <li><b>userId</b>: <b>绝不</b>从请求接受！必须从 JWT Token 中提取。</li>
 * </ul>
 *
 * <h3>访问控制</h3>
 * <ul>
 *   <li>练习题目（enabled=TRUE）: 所有登录用户可提交</li>
 *   <li>未启用题目（enabled=FALSE）: 拒绝访问（可能是竞赛专用题目，需通过竞赛接口提交）</li>
 * </ul>
 *
 * <h3>防篡改措施</h3>
 * <ul>
 *   <li>用户身份: 从 SecurityContext 获取，由 JWT Filter 解析并验证签名</li>
 *   <li>提交记录: 使用服务端获取的 userId，而非请求参数</li>
 *   <li>分数更新: 服务端计算，不信任客户端</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

  private static final String DYNAMIC_FLAG_KEY_PREFIX = "nkctf:flag:";

  private final SubmissionMapper submissionMapper;
  private final ChallengeMapper challengeMapper;
  private final UserMapper userMapper;
  private final StringRedisTemplate stringRedisTemplate;

  @Override
  @Transactional
  public SubmitFlagResponse submitFlag(SubmitFlagRequest request) {
    // ========== 安全验证：用户身份 ==========
    // 用户 ID 从 JWT Token 获取，不信任任何请求参数
    User currentUser = getCurrentUser();

    // ========== 安全验证：题目访问权限 ==========
    Challenge challenge = challengeMapper.selectById(request.getChallengeId());
    if (challenge == null) {
      throw new BusinessException(404, "题目不存在");
    }

    // 验证题目是否对当前用户可访问
    // enabled=TRUE 的题目为练习题，所有用户可提交
    // enabled=FALSE 的题目可能是竞赛专用题目，拒绝通过此接口提交
    if (!Boolean.TRUE.equals(challenge.getEnabled())) {
      log.warn("安全警告: 用户 {} 尝试通过练习接口提交未启用的题目 {} (可能是竞赛题目)",
          currentUser.getUsername(), challenge.getId());
      throw new BusinessException(403, "该题目不可通过此接口提交，请检查是否为竞赛题目");
    }

    // ========== 业务逻辑 ==========

    // 检查是否已经解决（防止重复计分）
    Boolean alreadySolved = submissionMapper.hasUserSolved(
        currentUser.getId(), challenge.getId());
    if (Boolean.TRUE.equals(alreadySolved)) {
      return SubmitFlagResponse.builder()
          .correct(false)
          .message("你已经解决了这道题目")
          .totalScore(currentUser.getScore())
          .rank(userMapper.getUserRank(currentUser.getId()))
          .build();
    }

    // 验证 Flag（使用服务端存储的正确答案）
    String submittedFlag = request.getFlag().trim();
    String correctFlag = getCorrectFlag(challenge, currentUser.getId());
    boolean isCorrect = correctFlag != null && correctFlag.equals(submittedFlag);

    // 创建提交记录（使用服务端获取的用户 ID）
    Submission submission = new Submission();
    submission.setUserId(currentUser.getId());  // 从 JWT 获取，非请求参数
    submission.setChallengeId(challenge.getId());
    submission.setFlag(submittedFlag);
    submission.setIsCorrect(isCorrect);
    submission.setPointsAwarded(isCorrect ? challenge.getPoints() : 0);
    submissionMapper.insert(submission);

    if (isCorrect) {
      // 更新用户分数（服务端计算）
      int newScore = (currentUser.getScore() != null ? currentUser.getScore() : 0)
          + challenge.getPoints();
      currentUser.setScore(newScore);
      userMapper.updateById(currentUser);

      // 更新排行榜冗余字段
      userMapper.updateLeaderboardFields(currentUser.getId());

      log.info("用户 {} 成功解决题目 {}，获得 {} 分",
          currentUser.getUsername(), challenge.getTitle(), challenge.getPoints());

      return SubmitFlagResponse.builder()
          .correct(true)
          .pointsAwarded(challenge.getPoints())
          .message("恭喜你，Flag 正确！")
          .totalScore(currentUser.getScore())
          .rank(userMapper.getUserRank(currentUser.getId()))
          .build();
    } else {
      log.debug("用户 {} 提交了错误的 Flag，题目: {}",
          currentUser.getUsername(), challenge.getTitle());

      return SubmitFlagResponse.builder()
          .correct(false)
          .message("Flag 错误，请再试一次")
          .totalScore(currentUser.getScore())
          .rank(userMapper.getUserRank(currentUser.getId()))
          .build();
    }
  }

  /**
   * 获取正确的 Flag
   *
   * 安全说明：
   * - 静态 Flag: 从数据库获取，不对外暴露
   * - 动态 Flag: 从 Redis 获取，key 包含用户 ID（从 JWT 获取）
   * - 攻击者无法通过篡改请求获取其他用户的动态 Flag
   */
  private String getCorrectFlag(Challenge challenge, Long userId) {
    if (Boolean.TRUE.equals(challenge.getIsDynamic())) {
      // 动态 Flag: nkctf:flag:{challengeId}:{userId}
      // userId 从 JWT 获取，攻击者无法篡改
      String key = DYNAMIC_FLAG_KEY_PREFIX + challenge.getId() + ":" + userId;
      return stringRedisTemplate.opsForValue().get(key);
    } else {
      // 静态 Flag
      return challenge.getFlag();
    }
  }

  /**
   * 获取当前登录用户
   *
   * 安全说明：
   * - 用户身份从 SecurityContext 获取
   * - SecurityContext 由 JwtAuthenticationFilter 填充
   * - JWT Token 由服务端签名，攻击者无法伪造
   * - 从数据库查询最新用户信息，确保数据一致性
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

    // 从数据库查询用户，确保获取最新信息
    User user = userMapper.selectOne(
        new LambdaQueryWrapper<User>()
            .eq(User::getUsername, userDetails.getUsername())
    );

    if (user == null) {
      throw new BusinessException(401, "用户不存在");
    }

    return user;
  }
}
