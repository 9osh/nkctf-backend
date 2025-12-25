package cn.edu.ndky.nkctf.service;

import cn.edu.ndky.nkctf.dto.request.LoginRequest;
import cn.edu.ndky.nkctf.dto.request.RegisterRequest;
import cn.edu.ndky.nkctf.dto.response.LoginResponse;
import cn.edu.ndky.nkctf.dto.response.TokenRefreshResponse;

/**
 * 认证服务接口
 */
public interface AuthService {

  /**
   * 用户登录
   * @param request 登录请求
   * @param userAgent 客户端 User-Agent
   * @param ipAddress 客户端 IP 地址
   * @return 登录响应（包含 Access Token、Refresh Token 和用户信息）
   */
  LoginResponse login(LoginRequest request, String userAgent, String ipAddress);

  /**
   * 用户注册
   * @param request 注册请求
   * @param userAgent 客户端 User-Agent
   * @param ipAddress 客户端 IP 地址
   * @return 登录响应（注册成功后自动登录）
   */
  LoginResponse register(RegisterRequest request, String userAgent, String ipAddress);

  /**
   * 刷新 Token
   * @param refreshToken Refresh Token
   * @param userAgent 客户端 User-Agent
   * @param ipAddress 客户端 IP 地址
   * @return 新的 Token 对
   */
  TokenRefreshResponse refreshToken(String refreshToken, String userAgent, String ipAddress);

  /**
   * 用户登出
   * @param accessToken Access Token (JWT)
   * @param refreshToken Refresh Token（可选）
   */
  void logout(String accessToken, String refreshToken);

  /**
   * 登出所有设备
   * @param userId 用户 ID
   */
  void logoutAll(Long userId);

  /**
   * 检查 Access Token 是否在黑名单中
   * @param token Access Token
   * @return true 如果在黑名单中
   */
  boolean isTokenBlacklisted(String token);
}
