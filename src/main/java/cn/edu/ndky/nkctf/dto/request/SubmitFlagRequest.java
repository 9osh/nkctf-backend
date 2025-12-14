package cn.edu.ndky.nkctf.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 提交 Flag 请求 DTO
 */
@Data
public class SubmitFlagRequest {

  @NotNull(message = "题目 ID 不能为空")
  private Long challengeId;

  @NotBlank(message = "Flag 不能为空")
  @Size(max = 255, message = "Flag 长度不能超过 255 字符")
  private String flag;
}
