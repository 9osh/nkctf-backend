package cn.edu.ndky.nkctf.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 管理员竞赛题目响应 DTO
 */
@Data
@Builder
public class AdminCompetitionChallengeResponse {

  /**
   * CompetitionChallenge 关联 ID
   */
  private Long id;

  private Long challengeId;

  private String title;

  private String category;

  private String difficulty;

  private Integer points;

  /**
   * 排序顺序
   */
  private Integer sortOrder;

  /**
   * 本竞赛内的解题数
   */
  private Integer solves;
}
