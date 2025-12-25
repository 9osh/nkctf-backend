package cn.edu.ndky.nkctf.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token 刷新响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenRefreshResponse {

  /**
   * 新的 Access Token (JWT)
   */
  private String accessToken;

  /**
   * 新的 Refresh Token
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
