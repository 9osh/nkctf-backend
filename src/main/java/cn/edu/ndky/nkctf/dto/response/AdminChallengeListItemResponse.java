package cn.edu.ndky.nkctf.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 管理员题目列表项响应 DTO
 */
@Data
@Builder
public class AdminChallengeListItemResponse {

  private Long id;

  private String title;

  private String description;

  private String category;

  private String difficulty;

  private Integer points;

  private String author;

  private Boolean isDynamic;

  private Boolean enabled;

  /**
   * 解题数
   */
  private Integer solves;

  private String createTime;

  private String updateTime;
}
