package cn.edu.ndky.nkctf.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 解锁提示响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnlockHintResponse {

  /**
   * 提示 ID
   */
  private Long hintId;

  /**
   * 提示内容
   */
  private String content;

  /**
   * 花费的积分
   */
  private Integer cost;

  /**
   * 用户剩余积分
   */
  private Integer remainingScore;
}
