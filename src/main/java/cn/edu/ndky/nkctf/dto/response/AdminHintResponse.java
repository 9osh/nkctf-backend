package cn.edu.ndky.nkctf.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 管理员提示响应 DTO
 */
@Data
@Builder
public class AdminHintResponse {

  private Long id;

  private Long challengeId;

  /**
   * 提示内容（对管理员始终可见）
   */
  private String content;

  /**
   * 解锁消耗积分
   */
  private Integer cost;

  /**
   * 排序顺序
   */
  private Integer sortOrder;

  private String createTime;
}
