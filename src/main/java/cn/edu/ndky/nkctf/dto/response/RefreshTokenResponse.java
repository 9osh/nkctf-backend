package cn.edu.ndky.nkctf.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 刷新邀请令牌响应 DTO
 */
@Data
@Builder
public class RefreshTokenResponse {

  private String teamId;

  private String newInviteToken;

  private String message;
}
