package cn.edu.ndky.nkctf.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应 DTO
 * 注意：Refresh Token 通过 HttpOnly Cookie 传输，不在响应体中返回
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {

  /**
   * Access Token (JWT)
   */
  private String accessToken;

  /**
   * Refresh Token (内部使用，不会序列化到响应)
   * @deprecated 仅用于内部传递给 Controller 设置 Cookie，响应体中不包含此字段
   */
  private String refreshToken;

  /**
   * Access Token 过期时间（秒）
   */
  private Long expiresIn;

  private Long userId;

  private String username;

  private String nickname;

  private String role;
}
