package cn.edu.ndky.nkctf.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 竞赛状态变更通知 DTO
 *
 * 用于 WebSocket 广播竞赛状态变更事件。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionStatusNotification {

  /**
   * 竞赛 ID
   */
  private Long competitionId;

  /**
   * 竞赛名称
   */
  private String competitionName;

  /**
   * 原状态
   */
  private String oldStatus;

  /**
   * 新状态
   */
  private String newStatus;

  /**
   * 变更时间 (ISO 8601 格式)
   */
  private String changedAt;

  /**
   * 是否为管理员手动触发
   */
  private boolean manualOverride;
}
