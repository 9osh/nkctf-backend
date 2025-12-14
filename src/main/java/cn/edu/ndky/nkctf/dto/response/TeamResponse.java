package cn.edu.ndky.nkctf.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 队伍响应 DTO
 *
 * <h2>安全设计</h2>
 * <p>
 * 此响应不包含敏感信息（如邀请令牌）。
 * 邀请令牌通过专用的 GET /api/teams/invite-token 接口获取，仅队长可访问。
 * </p>
 */
@Data
@Builder
public class TeamResponse {

  private String id;

  private String name;

  private String description;

  private TeamMember captain;

  private List<TeamMember> members;

  private Integer memberCount;

  private String createTime;

  /**
   * 队伍成员
   */
  @Data
  @Builder
  public static class TeamMember {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String role;
  }
}
