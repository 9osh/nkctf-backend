package cn.edu.ndky.nkctf.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 验证码响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaResponse {

  /**
   * 验证码 ID（用于验证时提交）
   */
  private String captchaId;

  /**
   * 验证码图片（Base64 编码）
   */
  private String image;
}
