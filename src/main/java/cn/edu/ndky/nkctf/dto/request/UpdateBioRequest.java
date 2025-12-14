package cn.edu.ndky.nkctf.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新个性签名请求 DTO
 */
@Data
public class UpdateBioRequest {

  @Size(max = 500, message = "个性签名长度不能超过500字符")
  private String bio;
}
