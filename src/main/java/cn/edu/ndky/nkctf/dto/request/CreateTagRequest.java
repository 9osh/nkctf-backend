package cn.edu.ndky.nkctf.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建标签请求 DTO
 */
@Data
public class CreateTagRequest {

  @NotBlank(message = "标签名称不能为空")
  @Size(max = 50, message = "标签名称长度不能超过 50 字符")
  private String name;

  @Size(max = 255, message = "标签描述长度不能超过 255 字符")
  private String description;

  @Size(max = 20, message = "颜色值长度不能超过 20 字符")
  private String color;

  private Integer sortOrder;
}
