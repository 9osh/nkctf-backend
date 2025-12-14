package cn.edu.ndky.nkctf.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 挑战列表项响应 DTO（卡片展示用）
 */
@Data
@Builder
public class ChallengeListItemResponse {

  private Long id;

  private String title;

  private String description;

  private String category;

  private String difficulty;

  private Integer points;

  /**
   * 解题人数
   */
  private Integer solves;

  /**
   * 当前用户是否已解决
   */
  private Boolean solved;

  /**
   * 发布时间
   */
  private String releaseDate;
}
