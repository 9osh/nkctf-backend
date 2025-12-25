package cn.edu.ndky.nkctf.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更改角色请求 DTO
 */
@Data
public class ChangeRoleRequest {

  @NotBlank(message = "角色不能为空")
  private String role;
}
