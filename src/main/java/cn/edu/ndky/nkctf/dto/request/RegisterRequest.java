package cn.edu.ndky.nkctf.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求 DTO
 */
@Data
public class RegisterRequest {

  @NotBlank(message = "用户名不能为空")
  @Size(min = 3, max = 50, message = "用户名长度必须在3-50之间")
  @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
  private String username;

  @NotBlank(message = "密码不能为空")
  @Size(min = 6, max = 100, message = "密码长度必须在6-100之间")
  private String password;

  @Email(message = "邮箱格式不正确")
  @Size(max = 100, message = "邮箱长度不能超过100")
  private String email;

  @Size(max = 50, message = "昵称长度不能超过50")
  private String nickname;

  @NotBlank(message = "验证码 ID 不能为空")
  private String captchaId;

  @NotBlank(message = "验证码不能为空")
  @Size(min = 4, max = 6, message = "验证码长度必须在4-6之间")
  private String captchaCode;
}
