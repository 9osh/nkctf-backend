package cn.edu.ndky.nkctf.service.impl;

import cn.edu.ndky.nkctf.dto.request.LoginRequest;
import cn.edu.ndky.nkctf.dto.request.RegisterRequest;
import cn.edu.ndky.nkctf.dto.response.LoginResponse;
import cn.edu.ndky.nkctf.dto.response.TokenRefreshResponse;
import cn.edu.ndky.nkctf.entity.RefreshToken;
import cn.edu.ndky.nkctf.entity.User;
import cn.edu.ndky.nkctf.exception.BusinessException;
import cn.edu.ndky.nkctf.mapper.RefreshTokenMapper;
import cn.edu.ndky.nkctf.mapper.UserMapper;
import cn.edu.ndky.nkctf.service.AuthService;
import cn.edu.ndky.nkctf.util.JwtUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private static final String LOGIN_FAIL_COUNT_KEY = "nkctf:login:fail:";
  private static final String LOGIN_LOCK_KEY = "nkctf:login:lock:";
  private static final String TOKEN_BLACKLIST_KEY = "nkctf:token:blacklist:";
  private static final int MAX_FAIL_COUNT = 5;
  private static final int LOCK_MINUTES = 30;

  private final UserMapper userMapper;
  private final RefreshTokenMapper refreshTokenMapper;
  private final JwtUtil jwtUtil;
  private final PasswordEncoder passwordEncoder;
  private final RedisTemplate<String, Object> redisTemplate;

  @Override
  @Transactional
  public LoginResponse login(LoginRequest request, String userAgent, String ipAddress) {
    String username = request.getUsername();

    // 检查账户是否被锁定
    checkAccountLock(username);

    // 根据用户名查询用户（使用 MyBatis-Plus 参数化查询，防止 SQL 注入）
    User user = userMapper.selectOne(
        new LambdaQueryWrapper<User>()
            .eq(User::getUsername, username)
    );

    // 用户不存在
    if (user == null) {
      log.warn("登录失败: 用户不存在 - {}", username);
      handleLoginFailure(username);
      throw new BusinessException(401, "用户名或密码错误");
    }

    // 用户被禁用
    if (!Boolean.TRUE.equals(user.getEnabled())) {
      log.warn("登录失败: 用户被禁用 - {}", username);
      throw new BusinessException(401, "账户已被禁用");
    }

    // 验证密码
    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
      log.warn("登录失败: 密码错误 - {}", username);
      handleLoginFailure(username);
      throw new BusinessException(401, "用户名或密码错误");
    }

    // 登录成功，清除失败计数
    clearLoginFailCount(username);

    // 生成双令牌
    return generateTokenPair(user, userAgent, ipAddress);
  }

  /**
   * 生成 Access Token 和 Refresh Token
   */
  private LoginResponse generateTokenPair(User user, String userAgent, String ipAddress) {
    // 生成 Access Token (JWT, 15分钟)
    String accessToken = jwtUtil.generateToken(user.getId(), user.getUsername());

    // 生成 Refresh Token (高熵随机值, 7天)
    String refreshTokenValue = generateRefreshToken();

    // 保存 Refresh Token 到数据库
    RefreshToken refreshToken = new RefreshToken();
    refreshToken.setToken(refreshTokenValue);
    refreshToken.setUserId(user.getId());
    refreshToken.setUserAgent(truncateUserAgent(userAgent));
    refreshToken.setIpAddress(ipAddress);
    refreshToken.setExpiresAt(
        LocalDateTime.now().plus(jwtUtil.getRefreshTokenExpiration(), ChronoUnit.MILLIS));
    refreshToken.setRevoked(false);
    refreshTokenMapper.insert(refreshToken);

    log.info("用户登录成功: {}", user.getUsername());

    return LoginResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshTokenValue)
        .expiresIn(jwtUtil.getAccessTokenExpiration() / 1000) // 转换为秒
        .userId(user.getId())
        .username(user.getUsername())
        .nickname(user.getNickname())
        .role(user.getRole())
        .build();
  }

  /**
   * 生成 Refresh Token (高熵随机值)
   */
  private String generateRefreshToken() {
    return UUID.randomUUID().toString().replace("-", "") +
        UUID.randomUUID().toString().replace("-", "");
  }

  /**
   * 截断 User-Agent (最大 500 字符)
   */
  private String truncateUserAgent(String userAgent) {
    if (userAgent == null) {
      return null;
    }
    return userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent;
  }

  /**
   * 检查账户是否被锁定
   */
  private void checkAccountLock(String username) {
    String lockKey = LOGIN_LOCK_KEY + username;
    Boolean isLocked = redisTemplate.hasKey(lockKey);
    if (Boolean.TRUE.equals(isLocked)) {
      Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.MINUTES);
      log.warn("账户已被锁定: {} - 剩余 {} 分钟", username, ttl);
      throw new BusinessException(423, "账户已被锁定，请 " + ttl + " 分钟后再试");
    }
  }

  /**
   * 处理登录失败
   */
  private void handleLoginFailure(String username) {
    String failCountKey = LOGIN_FAIL_COUNT_KEY + username;
    String lockKey = LOGIN_LOCK_KEY + username;

    // 增加失败计数
    Long failCount = redisTemplate.opsForValue().increment(failCountKey);
    if (failCount == 1) {
      // 首次失败，设置过期时间（30分钟内的失败次数）
      redisTemplate.expire(failCountKey, LOCK_MINUTES, TimeUnit.MINUTES);
    }

    log.warn("登录失败计数: {} - 第 {} 次", username, failCount);

    // 达到最大失败次数，锁定账户
    if (failCount >= MAX_FAIL_COUNT) {
      redisTemplate.opsForValue().set(lockKey, "locked", LOCK_MINUTES, TimeUnit.MINUTES);
      redisTemplate.delete(failCountKey);
      log.warn("账户已被锁定: {} - 锁定 {} 分钟", username, LOCK_MINUTES);
      throw new BusinessException(423, "登录失败次数过多，账户已被锁定 " + LOCK_MINUTES + " 分钟");
    }
  }

  /**
   * 清除登录失败计数
   */
  private void clearLoginFailCount(String username) {
    String failCountKey = LOGIN_FAIL_COUNT_KEY + username;
    redisTemplate.delete(failCountKey);
  }

  @Override
  @Transactional
  public LoginResponse register(RegisterRequest request, String userAgent, String ipAddress) {
    // 检查用户名是否已存在
    Long usernameCount = userMapper.selectCount(
        new LambdaQueryWrapper<User>()
            .eq(User::getUsername, request.getUsername())
    );
    if (usernameCount > 0) {
      throw new BusinessException(400, "用户名已存在");
    }

    // 检查邮箱是否已存在（如果提供了邮箱）
    if (StringUtils.hasText(request.getEmail())) {
      Long emailCount = userMapper.selectCount(
          new LambdaQueryWrapper<User>()
              .eq(User::getEmail, request.getEmail())
      );
      if (emailCount > 0) {
        throw new BusinessException(400, "邮箱已被注册");
      }
    }

    // 创建用户实体
    User user = new User();
    user.setUsername(request.getUsername());
    user.setPassword(passwordEncoder.encode(request.getPassword()));
    user.setEmail(request.getEmail());
    user.setNickname(StringUtils.hasText(request.getNickname())
        ? request.getNickname() : request.getUsername());
    user.setRole("USER");
    user.setEnabled(true);
    user.setScore(0);

    // 保存用户
    userMapper.insert(user);

    log.info("用户注册成功: {}", user.getUsername());

    // 生成双令牌并返回（注册成功后自动登录）
    return generateTokenPair(user, userAgent, ipAddress);
  }

  @Override
  @Transactional
  public TokenRefreshResponse refreshToken(String refreshTokenValue, String userAgent,
      String ipAddress) {
    // 查询 Refresh Token
    RefreshToken refreshToken = refreshTokenMapper.selectOne(
        new LambdaQueryWrapper<RefreshToken>()
            .eq(RefreshToken::getToken, refreshTokenValue)
    );

    // Token 不存在
    if (refreshToken == null) {
      log.warn("刷新 Token 失败: Token 不存在");
      throw new BusinessException(401, "无效的 Refresh Token");
    }

    // Token 已被撤销
    if (Boolean.TRUE.equals(refreshToken.getRevoked())) {
      log.warn("刷新 Token 失败: Token 已被撤销 - userId: {}", refreshToken.getUserId());
      // 安全措施：检测到被撤销的 Token 被重用，可能是 Token 被盗，撤销该用户所有 Token
      refreshTokenMapper.revokeAllByUserId(refreshToken.getUserId());
      throw new BusinessException(401, "Refresh Token 已失效");
    }

    // Token 已过期
    if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
      log.warn("刷新 Token 失败: Token 已过期 - userId: {}", refreshToken.getUserId());
      throw new BusinessException(401, "Refresh Token 已过期");
    }

    // 查询用户
    User user = userMapper.selectById(refreshToken.getUserId());
    if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
      log.warn("刷新 Token 失败: 用户不存在或已禁用 - userId: {}", refreshToken.getUserId());
      throw new BusinessException(401, "用户不存在或已被禁用");
    }

    // Token 轮转：撤销旧 Token
    refreshTokenMapper.revokeByToken(refreshTokenValue);

    // 生成新的 Access Token
    String newAccessToken = jwtUtil.generateToken(user.getId(), user.getUsername());

    // 生成新的 Refresh Token
    String newRefreshTokenValue = generateRefreshToken();
    RefreshToken newRefreshToken = new RefreshToken();
    newRefreshToken.setToken(newRefreshTokenValue);
    newRefreshToken.setUserId(user.getId());
    newRefreshToken.setUserAgent(truncateUserAgent(userAgent));
    newRefreshToken.setIpAddress(ipAddress);
    newRefreshToken.setExpiresAt(
        LocalDateTime.now().plus(jwtUtil.getRefreshTokenExpiration(), ChronoUnit.MILLIS));
    newRefreshToken.setRevoked(false);
    refreshTokenMapper.insert(newRefreshToken);

    log.info("Token 刷新成功: userId={}", user.getId());

    return TokenRefreshResponse.builder()
        .accessToken(newAccessToken)
        .refreshToken(newRefreshTokenValue)
        .expiresIn(jwtUtil.getAccessTokenExpiration() / 1000)
        .userId(user.getId())
        .username(user.getUsername())
        .nickname(user.getNickname())
        .role(user.getRole())
        .build();
  }

  @Override
  @Transactional
  public void logout(String accessToken, String refreshToken) {
    // 将 Access Token 加入黑名单
    if (StringUtils.hasText(accessToken)) {
      try {
        var claims = jwtUtil.parseToken(accessToken);
        long expiration = claims.getExpiration().getTime();
        long now = System.currentTimeMillis();
        long ttl = expiration - now;

        if (ttl > 0) {
          String blacklistKey = TOKEN_BLACKLIST_KEY + accessToken;
          redisTemplate.opsForValue().set(blacklistKey, "1", ttl, TimeUnit.MILLISECONDS);
          log.info("Access Token 已加入黑名单");
        }
      } catch (Exception e) {
        log.warn("登出时 Access Token 解析失败: {}", e.getMessage());
      }
    }

    // 撤销 Refresh Token
    if (StringUtils.hasText(refreshToken)) {
      int count = refreshTokenMapper.revokeByToken(refreshToken);
      if (count > 0) {
        log.info("Refresh Token 已撤销");
      }
    }
  }

  @Override
  @Transactional
  public void logoutAll(Long userId) {
    int count = refreshTokenMapper.revokeAllByUserId(userId);
    log.info("已撤销用户所有 Refresh Token: userId={}, count={}", userId, count);
  }

  @Override
  public boolean isTokenBlacklisted(String token) {
    String blacklistKey = TOKEN_BLACKLIST_KEY + token;
    return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
  }
}
