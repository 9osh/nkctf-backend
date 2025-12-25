package cn.edu.ndky.nkctf.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新题目请求 DTO
 */
@Data
public class UpdateChallengeRequest {

  @Size(max = 200, message = "标题长度不能超过 200 字符")
  private String title;

  @Size(max = 500, message = "简述长度不能超过 500 字符")
  private String description;

  private String content;

  private String category;

  private String difficulty;

  @Min(value = 1, message = "分数最小为 1")
  private Integer points;

  @Size(max = 100, message = "作者长度不能超过 100 字符")
  private String author;

  /**
   * 静态 flag
   */
  private String flag;

  /**
   * 是否为动态容器题目
   */
  private Boolean isDynamic;

  /**
   * Docker 镜像名
   */
  private String dockerImage;

  /**
   * 是否启用
   */
  private Boolean enabled;
}
