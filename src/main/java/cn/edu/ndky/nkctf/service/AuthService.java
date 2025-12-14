package cn.edu.ndky.nkctf.service;

import cn.edu.ndky.nkctf.dto.request.LoginRequest;
import cn.edu.ndky.nkctf.dto.request.RegisterRequest;
import cn.edu.ndky.nkctf.dto.response.LoginResponse;

/**
 * 认证服务接口
 */
public interface AuthService {

  /**
   * 用户登录
   * @param request 登录请求
   * @return 登录响应（包含 Token 和用户信息）
   */
  LoginResponse login(LoginRequest request);

  /**
   * 用户注册
   * @param request 注册请求
   * @return 登录响应（注册成功后自动登录）
   */
  LoginResponse register(RegisterRequest request);

  /**
   * 用户登出
   * @param token JWT Token
   */
  void logout(String token);

  /**
   * 检查 Token 是否在黑名单中
   * @param token JWT Token
   * @return true 如果在黑名单中
   */
  boolean isTokenBlacklisted(String token);
}
