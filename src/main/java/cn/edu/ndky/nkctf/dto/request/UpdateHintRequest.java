package cn.edu.ndky.nkctf.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 更新提示请求 DTO
 */
@Data
public class UpdateHintRequest {

  private String content;

  @Min(value = 0, message = "积分消耗最小为 0")
  private Integer cost;

  private Integer sortOrder;
}
