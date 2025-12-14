package cn.edu.ndky.nkctf.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 邀请令牌响应 DTO
 *
 * <h2>安全说明</h2>
 * <p>此接口仅队长可访问，邀请令牌是敏感信息，泄露后可被任意用户用于加入队伍。</p>
 */
@Data
@Builder
public class InviteTokenResponse {

  /**
   * 队伍 ID
   */
  private String teamId;

  /**
   * 队伍名称
   */
  private String teamName;

  /**
   * 邀请令牌
   */
  private String inviteToken;
}
