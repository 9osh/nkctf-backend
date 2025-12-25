package cn.edu.ndky.nkctf.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 管理员参赛者响应 DTO
 */
@Data
@Builder
public class AdminParticipantResponse {

  /**
   * CompetitionUser/CompetitionTeam 关联 ID
   */
  private Long id;

  /**
   * 参赛者 ID（用户 ID 或团队 ID）
   */
  private String participantId;

  /**
   * 参赛者名称（用户名或团队名）
   */
  private String name;

  /**
   * 类型: user 或 team
   */
  private String type;

  /**
   * 当前积分
   */
  private Integer score;

  /**
   * 解题数
   */
  private Integer solvedCount;

  /**
   * 报名时间
   */
  private String registerTime;

  /**
   * 团队成员数（仅团队竞赛）
   */
  private Integer memberCount;

  /**
   * 队长名称（仅团队竞赛）
   */
  private String captainName;
}
