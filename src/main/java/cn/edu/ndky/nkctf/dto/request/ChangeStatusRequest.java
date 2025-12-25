package cn.edu.ndky.nkctf.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更改状态请求 DTO
 */
@Data
public class ChangeStatusRequest {

  @NotBlank(message = "状态不能为空")
  private String status;
}
