package cn.edu.ndky.nkctf.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 切换启用状态请求 DTO
 */
@Data
public class ToggleEnabledRequest {

  @NotNull(message = "enabled 不能为空")
  private Boolean enabled;
}
