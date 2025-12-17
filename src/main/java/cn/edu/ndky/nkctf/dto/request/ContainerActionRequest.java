package cn.edu.ndky.nkctf.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 容器操作请求（销毁、延时）
 */
@Data
public class ContainerActionRequest {

  @NotBlank(message = "容器ID不能为空")
  private String containerId;
}
