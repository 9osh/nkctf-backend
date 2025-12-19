package cn.edu.ndky.nkctf.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新标签请求 DTO
 */
@Data
public class UpdateTagRequest {

  @NotNull(message = "标签 ID 不能为空")
  private Long id;

  @NotBlank(message = "标签名称不能为空")
  @Size(max = 50, message = "标签名称长度不能超过 50 字符")
  private String name;

  @Size(max = 255, message = "标签描述长度不能超过 255 字符")
  private String description;

  @Size(max = 20, message = "颜色值长度不能超过 20 字符")
  private String color;

  private Integer sortOrder;
}
