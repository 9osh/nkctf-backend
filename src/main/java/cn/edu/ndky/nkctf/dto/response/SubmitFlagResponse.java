package cn.edu.ndky.nkctf.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 提交 Flag 响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitFlagResponse {

  /**
   * 是否正确
   */
  private Boolean correct;

  /**
   * 获得的积分（正确时返回）
   */
  private Integer pointsAwarded;

  /**
   * 提示消息
   */
  private String message;

  /**
   * 用户当前总分
   */
  private Integer totalScore;

  /**
   * 用户当前排名
   */
  private Integer rank;
}
