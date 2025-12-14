package cn.edu.ndky.nkctf.controller;

import cn.edu.ndky.nkctf.annotation.RateLimit;
import cn.edu.ndky.nkctf.dto.Result;
import cn.edu.ndky.nkctf.dto.request.LoginRequest;
import cn.edu.ndky.nkctf.dto.request.RegisterRequest;
import cn.edu.ndky.nkctf.dto.response.CaptchaResponse;
import cn.edu.ndky.nkctf.dto.response.LoginResponse;
import cn.edu.ndky.nkctf.exception.BusinessException;
import cn.edu.ndky.nkctf.service.AuthService;
import cn.edu.ndky.nkctf.service.CaptchaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
@Tag(name = "认证管理", description = "用户登录、注册等认证相关接口")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final CaptchaService captchaService;

  @Operation(summary = "获取验证码", description = "获取图形验证码，用于注册")
  @GetMapping("/captcha")
  @RateLimit(window = 60, maxRequests = 10, message = "验证码请求过于频繁，请稍后再试")
  public Result<CaptchaResponse> captcha() {
    CaptchaResponse response = captchaService.generate();
    return Result.success(response);
  }

  @Operation(summary = "用户登录", description = "通过用户名和密码登录，返回 JWT Token")
  @PostMapping("/login")
  public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    LoginResponse response = authService.login(request);
    return Result.success("登录成功", response);
  }

  @Operation(summary = "用户注册", description = "注册新用户，需要验证码，成功后自动登录返回 JWT Token")
  @PostMapping("/register")
  @RateLimit(window = 60, maxRequests = 5, message = "注册请求过于频繁，请稍后再试")
  public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
    // 验证验证码
    if (!captchaService.verify(request.getCaptchaId(), request.getCaptchaCode())) {
      throw new BusinessException(400, "验证码错误或已过期");
    }

    LoginResponse response = authService.register(request);
    return Result.success("注册成功", response);
  }

  @Operation(summary = "用户登出", description = "登出当前用户，使 Token 失效")
  @PostMapping("/logout")
  public Result<Void> logout(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      String token = bearerToken.substring(7);
      authService.logout(token);
    }
    return Result.success("登出成功", null);
  }
}
