package cn.edu.ndky.nkctf.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 解锁提示请求 DTO
 */
@Data
public class UnlockHintRequest {

  @NotNull(message = "提示 ID 不能为空")
  private Long hintId;
}
