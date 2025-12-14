package cn.edu.ndky.nkctf.service.impl;

import cn.edu.ndky.nkctf.dto.request.UnlockHintRequest;
import cn.edu.ndky.nkctf.dto.response.UnlockHintResponse;
import cn.edu.ndky.nkctf.entity.Challenge;
import cn.edu.ndky.nkctf.entity.Hint;
import cn.edu.ndky.nkctf.entity.HintUnlock;
import cn.edu.ndky.nkctf.entity.User;
import cn.edu.ndky.nkctf.exception.BusinessException;
import cn.edu.ndky.nkctf.mapper.ChallengeMapper;
import cn.edu.ndky.nkctf.mapper.HintMapper;
import cn.edu.ndky.nkctf.mapper.HintUnlockMapper;
import cn.edu.ndky.nkctf.mapper.UserMapper;
import cn.edu.ndky.nkctf.service.HintService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 提示服务实现
 *
 * <h2>安全设计说明</h2>
 *
 * <h3>请求参数信任策略</h3>
 * <ul>
 *   <li><b>hintId</b>: 资源标识符，可从请求接受。用于标识用户要解锁的提示。</li>
 *   <li><b>userId</b>: <b>绝不</b>从请求接受！必须从 JWT Token 中提取。</li>
 * </ul>
 *
 * <h3>访问控制验证流程</h3>
 * <ol>
 *   <li>从 JWT Token 获取用户身份（不可伪造）</li>
 *   <li>根据 hintId 查询提示及其关联的题目</li>
 *   <li>验证题目是否对用户可访问（enabled=TRUE）</li>
 *   <li>验证用户积分是否足够</li>
 * </ol>
 *
 * <h3>防篡改措施</h3>
 * <ul>
 *   <li>用户身份: 从 SecurityContext 获取，由 JWT Filter 解析并验证签名</li>
 *   <li>解锁记录: 使用服务端获取的 userId，而非请求参数</li>
 *   <li>积分扣除: 服务端从数据库读取并计算，不信任客户端</li>
 *   <li>提示内容: 只有解锁后才返回，防止信息泄露</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HintServiceImpl implements HintService {

  private final HintMapper hintMapper;
  private final HintUnlockMapper hintUnlockMapper;
  private final ChallengeMapper challengeMapper;
  private final UserMapper userMapper;

  @Override
  @Transactional
  public UnlockHintResponse unlockHint(UnlockHintRequest request) {
    // ========== 安全验证：用户身份 ==========
    // 用户 ID 从 JWT Token 获取，不信任任何请求参数
    User currentUser = getCurrentUser();

    // ========== 安全验证：提示存在性 ==========
    Hint hint = hintMapper.selectById(request.getHintId());
    if (hint == null) {
      throw new BusinessException(404, "提示不存在");
    }

    // ========== 安全验证：题目访问权限 ==========
    // 验证提示所属题目是否对用户可访问
    Challenge challenge = challengeMapper.selectById(hint.getChallengeId());
    if (challenge == null) {
      log.error("数据完整性问题: 提示 {} 关联的题目 {} 不存在",
          hint.getId(), hint.getChallengeId());
      throw new BusinessException(500, "系统错误，请联系管理员");
    }

    // enabled=TRUE 的题目为练习题，所有用户可访问其提示
    // enabled=FALSE 的题目可能是竞赛专用题目，拒绝通过此接口解锁提示
    if (!Boolean.TRUE.equals(challenge.getEnabled())) {
      log.warn("安全警告: 用户 {} 尝试解锁未启用题目 {} 的提示 {} (可能是竞赛题目)",
          currentUser.getUsername(), challenge.getId(), hint.getId());
      throw new BusinessException(403, "该题目的提示不可通过此接口解锁");
    }

    // ========== 业务逻辑 ==========

    // 检查是否已经解锁（防止重复扣分）
    Boolean alreadyUnlocked = hintUnlockMapper.hasUserUnlocked(
        currentUser.getId(), hint.getId());
    if (Boolean.TRUE.equals(alreadyUnlocked)) {
      // 已解锁，直接返回内容，不扣分
      log.debug("用户 {} 请求已解锁的提示 {}，直接返回内容",
          currentUser.getUsername(), hint.getId());
      return UnlockHintResponse.builder()
          .hintId(hint.getId())
          .content(hint.getContent())
          .cost(0)
          .remainingScore(currentUser.getScore())
          .build();
    }

    // 验证积分是否足够（从数据库获取最新积分）
    int userScore = currentUser.getScore() != null ? currentUser.getScore() : 0;
    if (userScore < hint.getCost()) {
      throw new BusinessException(400,
          "积分不足，需要 " + hint.getCost() + " 积分，当前 " + userScore + " 积分");
    }

    // 扣除积分（服务端计算）
    int newScore = userScore - hint.getCost();
    currentUser.setScore(newScore);
    userMapper.updateById(currentUser);

    // 创建解锁记录（使用服务端获取的用户 ID）
    HintUnlock hintUnlock = new HintUnlock();
    hintUnlock.setUserId(currentUser.getId());  // 从 JWT 获取，非请求参数
    hintUnlock.setHintId(hint.getId());
    hintUnlock.setCost(hint.getCost());
    hintUnlockMapper.insert(hintUnlock);

    log.info("用户 {} 解锁了题目 {} 的提示 {}，花费 {} 积分",
        currentUser.getUsername(), challenge.getTitle(), hint.getId(), hint.getCost());

    return UnlockHintResponse.builder()
        .hintId(hint.getId())
        .content(hint.getContent())
        .cost(hint.getCost())
        .remainingScore(currentUser.getScore())
        .build();
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
