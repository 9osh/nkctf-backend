package cn.edu.ndky.nkctf.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建队伍请求 DTO
 */
@Data
public class CreateTeamRequest {

  @NotBlank(message = "队伍名称不能为空")
  @Size(min = 2, max = 50, message = "队伍名称长度必须在 2-50 之间")
  private String name;

  @Size(max = 500, message = "队伍描述不能超过 500 字符")
  private String description;
}
