package cn.edu.ndky.nkctf.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 管理员用户列表项响应 DTO
 */
@Data
@Builder
public class AdminUserListItemResponse {

  private Long id;

  private String username;

  private String email;

  private String nickname;

  private String role;

  private Boolean enabled;

  private Integer score;

  private Integer solvedCount;

  /**
   * 所在团队名称
   */
  private String teamName;

  private String createTime;

  private String lastSubmitTime;
}
