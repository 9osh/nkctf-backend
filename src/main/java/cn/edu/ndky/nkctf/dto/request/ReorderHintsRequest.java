package cn.edu.ndky.nkctf.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 重排序提示请求 DTO
 */
@Data
public class ReorderHintsRequest {

  @NotNull(message = "提示 ID 列表不能为空")
  @Size(min = 1, message = "提示 ID 列表不能为空")
  private List<Long> hintIds;
}
