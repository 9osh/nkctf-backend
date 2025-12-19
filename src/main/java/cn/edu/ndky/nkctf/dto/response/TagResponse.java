package cn.edu.ndky.nkctf.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 标签响应 DTO
 */
@Data
@Builder
public class TagResponse {

  private Long id;

  private String name;

  private String description;

  private String color;

  private Integer sortOrder;

  /**
   * 文章数量（仅统计已发布的文章）
   */
  private Integer articleCount;
}
