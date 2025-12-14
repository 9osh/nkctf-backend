package cn.edu.ndky.nkctf.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 排行榜条目响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntryResponse {

  /**
   * 排名
   */
  private Integer rank;

  /**
   * 用户 ID
   */
  private Long userId;

  /**
   * 昵称
   */
  private String nickname;

  /**
   * 头像 URL
   */
  private String avatar;

  /**
   * 总分数
   */
  private Integer points;

  /**
   * 解题数量
   */
  private Integer solvedCount;

  /**
   * 最后提交时间（ISO 8601 格式）
   */
  private String lastSubmitTime;
}
