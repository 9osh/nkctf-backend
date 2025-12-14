package cn.edu.ndky.nkctf.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 竞赛 Flag 提交响应 DTO
 */
@Data
@Builder
public class CompetitionSubmitFlagResponse {

  /**
   * 是否正确
   */
  private Boolean correct;

  /**
   * 获得的分数（答对时）
   */
  private Integer pointsAwarded;

  /**
   * 提示消息
   */
  private String message;

  /**
   * 当前队伍/用户在竞赛中的总分
   */
  private Integer totalScore;

  /**
   * 当前队伍/用户在竞赛中的排名
   */
  private Integer rank;

  /**
   * 是否计分（只有 active 状态才计分）
   */
  private Boolean scored;
}
