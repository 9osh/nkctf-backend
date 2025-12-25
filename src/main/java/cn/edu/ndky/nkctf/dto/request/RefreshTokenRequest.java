package cn.edu.ndky.nkctf.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 刷新 Token 请求 DTO
 */
@Data
public class RefreshTokenRequest {

  @NotBlank(message = "Refresh Token 不能为空")
  private String refreshToken;
}
