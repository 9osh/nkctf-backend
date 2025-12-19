package cn.edu.ndky.nkctf.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建文章请求 DTO
 */
@Data
public class CreateArticleRequest {

  @NotBlank(message = "文章标题不能为空")
  @Size(max = 200, message = "标题长度不能超过 200 字符")
  private String title;

  @Size(max = 500, message = "摘要长度不能超过 500 字符")
  private String summary;

  @NotBlank(message = "文章内容不能为空")
  private String content;

  /**
   * 标签 ID 列表
   */
  private List<Long> tagIds;
}
