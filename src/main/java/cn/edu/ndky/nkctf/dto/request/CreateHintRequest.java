package cn.edu.ndky.nkctf.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建提示请求 DTO
 */
@Data
public class CreateHintRequest {

  @NotBlank(message = "提示内容不能为空")
  private String content;

  @NotNull(message = "积分消耗不能为空")
  @Min(value = 0, message = "积分消耗最小为 0")
  private Integer cost;

  /**
   * 排序顺序（可选，不提供则自动分配）
   */
  private Integer sortOrder;
}
