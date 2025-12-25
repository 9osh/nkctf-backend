package cn.edu.ndky.nkctf.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员更新用户请求 DTO
 */
@Data
public class UpdateUserByAdminRequest {

  @Size(max = 50, message = "昵称长度不能超过 50 字符")
  private String nickname;

  @Size(max = 200, message = "个性签名长度不能超过 200 字符")
  private String bio;

  /**
   * 角色 (USER, ADMIN)
   */
  private String role;

  /**
   * 是否启用
   */
  private Boolean enabled;
}
