package cn.edu.ndky.nkctf.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 管理员用户详情响应 DTO
 */
@Data
@Builder
public class AdminUserDetailResponse {

  private Long id;

  private String username;

  private String email;

  private String nickname;

  private String avatar;

  private String bio;

  private String role;

  private Boolean enabled;

  private Integer score;

  private Integer solvedCount;

  /**
   * 团队信息
   */
  private TeamInfo team;

  private String createTime;

  private String updateTime;

  private String lastSubmitTime;

  @Data
  @Builder
  public static class TeamInfo {
    private String id;
    private String name;
    /**
     * 在团队中的角色: CAPTAIN 或 MEMBER
     */
    private String role;
  }
}
