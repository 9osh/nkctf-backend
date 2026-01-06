package cn.edu.ndky.nkctf.controller;

import cn.edu.ndky.nkctf.annotation.RateLimit;
import cn.edu.ndky.nkctf.dto.Result;
import cn.edu.ndky.nkctf.dto.request.LoginRequest;
import cn.edu.ndky.nkctf.dto.request.RegisterRequest;
import cn.edu.ndky.nkctf.dto.response.CaptchaResponse;
import cn.edu.ndky.nkctf.dto.response.LoginResponse;
import cn.edu.ndky.nkctf.dto.response.TokenRefreshResponse;
import cn.edu.ndky.nkctf.exception.BusinessException;
import cn.edu.ndky.nkctf.service.AuthService;
import cn.edu.ndky.nkctf.service.CaptchaService;
import cn.edu.ndky.nkctf.util.CookieUtil;
import cn.edu.ndky.nkctf.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器
 */
@Tag(name = "认证管理", description = "用户登录、注册、Token 刷新等认证相关接口")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final CaptchaService captchaService;
  private final JwtUtil jwtUtil;
  private final CookieUtil cookieUtil;

  @Operation(summary = "获取验证码", description = "获取图形验证码，用于注册")
  @GetMapping("/captcha")
  @RateLimit(window = 60, maxRequests = 10, message = "验证码请求过于频繁，请稍后再试")
  public Result<CaptchaResponse> captcha() {
    CaptchaResponse response = captchaService.generate();
    return Result.success(response);
  }

  @Operation(summary = "用户登录",
      description = "通过用户名和密码登录，Access Token 返回在响应体中，Refresh Token 设置为 HttpOnly Cookie")
  @PostMapping("/login")
  public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request,
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse) {
    String userAgent = httpRequest.getHeader("User-Agent");
    String ipAddress = getClientIp(httpRequest);

    LoginResponse response = authService.login(request, userAgent, ipAddress);

    // 将 Refresh Token 设置为 HttpOnly Cookie
    cookieUtil.setRefreshTokenCookie(httpResponse, response.getRefreshToken());

    // 清除响应中的 refreshToken (通过 Cookie 传输，不在响应体中返回)
    response.setRefreshToken(null);

    return Result.success("登录成功", response);
  }

  @Operation(summary = "用户注册",
      description = "注册新用户，需要验证码，成功后自动登录，Refresh Token 设置为 HttpOnly Cookie")
  @PostMapping("/register")
  @RateLimit(window = 60, maxRequests = 5, message = "注册请求过于频繁，请稍后再试")
  public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request,
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse) {
    // 验证验证码
    if (!captchaService.verify(request.getCaptchaId(), request.getCaptchaCode())) {
      throw new BusinessException(400, "验证码错误或已过期");
    }

    String userAgent = httpRequest.getHeader("User-Agent");
    String ipAddress = getClientIp(httpRequest);

    LoginResponse response = authService.register(request, userAgent, ipAddress);

    // 将 Refresh Token 设置为 HttpOnly Cookie
    cookieUtil.setRefreshTokenCookie(httpResponse, response.getRefreshToken());

    // 清除响应中的 refreshToken
    response.setRefreshToken(null);

    return Result.success("注册成功", response);
  }

  @Operation(summary = "刷新 Token",
      description = "从 HttpOnly Cookie 读取 Refresh Token，返回新的 Access Token 并更新 Cookie (Token 轮转)")
  @PostMapping("/refresh")
  @RateLimit(window = 60, maxRequests = 30, message = "Token 刷新请求过于频繁，请稍后再试")
  public Result<TokenRefreshResponse> refresh(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse) {
    // 从 Cookie 中读取 Refresh Token
    String refreshTokenValue = cookieUtil.getRefreshTokenFromCookie(httpRequest)
        .orElseThrow(() -> new BusinessException(401, "缺少 Refresh Token"));

    String userAgent = httpRequest.getHeader("User-Agent");
    String ipAddress = getClientIp(httpRequest);

    TokenRefreshResponse response = authService.refreshToken(
        refreshTokenValue, userAgent, ipAddress);

    // 设置新的 Refresh Token Cookie (Token 轮转)
    cookieUtil.setRefreshTokenCookie(httpResponse, response.getRefreshToken());

    // 清除响应中的 refreshToken
    response.setRefreshToken(null);

    return Result.success("Token 刷新成功", response);
  }

  @Operation(summary = "用户登出", description = "登出当前设备，使当前 Access Token 和 Refresh Token 失效，并清除 Cookie")
  @PostMapping("/logout")
  public Result<Void> logout(HttpServletRequest request, HttpServletResponse response) {
    String bearerToken = request.getHeader("Authorization");
    String accessToken = null;
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      accessToken = bearerToken.substring(7);
    }

    // 从 Cookie 中获取 Refresh Token
    String refreshToken = cookieUtil.getRefreshTokenFromCookie(request).orElse(null);

    authService.logout(accessToken, refreshToken);

    // 清除 Refresh Token Cookie
    cookieUtil.clearRefreshTokenCookie(response);

    return Result.success("登出成功", null);
  }

  @Operation(summary = "登出所有设备", description = "撤销当前用户所有的 Refresh Token，所有设备都需重新登录，并清除当前设备 Cookie")
  @PostMapping("/logout-all")
  public Result<Void> logoutAll(HttpServletRequest request, HttpServletResponse response) {
    String bearerToken = request.getHeader("Authorization");
    if (!StringUtils.hasText(bearerToken) || !bearerToken.startsWith("Bearer ")) {
      throw new BusinessException(401, "未授权");
    }

    String token = bearerToken.substring(7);
    Long userId = jwtUtil.getUserId(token);
    authService.logoutAll(userId);

    // 清除当前设备的 Refresh Token Cookie
    cookieUtil.clearRefreshTokenCookie(response);

    return Result.success("已登出所有设备", null);
  }

  /**
   * 获取客户端真实 IP 地址
   */
  private String getClientIp(HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");
    if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("Proxy-Client-IP");
    }
    if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("WL-Proxy-Client-IP");
    }
    if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("HTTP_CLIENT_IP");
    }
    if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("HTTP_X_FORWARDED_FOR");
    }
    if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getRemoteAddr();
    }
    // 多个代理时，取第一个 IP
    if (ip != null && ip.contains(",")) {
      ip = ip.split(",")[0].trim();
    }
    return ip;
  }
}
