package cn.edu.ndky.nkctf.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 审核文章请求 DTO
 */
@Data
public class ReviewArticleRequest {

  @NotNull(message = "文章 ID 不能为空")
  private Long articleId;

  /**
   * 审核结果: true=通过, false=拒绝
   */
  @NotNull(message = "审核结果不能为空")
  private Boolean approved;

  /**
   * 审核意见（拒绝时必填）
   */
  @Size(max = 500, message = "审核意见长度不能超过 500 字符")
  private String comment;
}
