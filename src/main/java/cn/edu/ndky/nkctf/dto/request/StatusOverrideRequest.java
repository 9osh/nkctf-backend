package cn.edu.ndky.nkctf.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 状态覆盖请求 DTO
 *
 * 用于管理员手动设置竞赛状态。
 */
@Data
public class StatusOverrideRequest {

  /**
   * 要设置的状态
   *
   * 必须是以下值之一：inactive, active, ending
   */
  @NotBlank(message = "状态不能为空")
  @Pattern(regexp = "^(inactive|active|ending)$", message = "状态必须是 inactive、active 或 ending")
  private String status;
}
