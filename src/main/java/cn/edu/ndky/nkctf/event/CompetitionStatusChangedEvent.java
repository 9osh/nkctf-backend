package cn.edu.ndky.nkctf.event;

import cn.edu.ndky.nkctf.entity.Competition;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * 竞赛状态变更事件
 *
 * 当竞赛状态发生变化时（由调度器自动更新或管理员手动设置），
 * 会发布此事件，供其他组件（如 WebSocket 广播、审计日志）订阅处理。
 */
@Getter
public class CompetitionStatusChangedEvent extends ApplicationEvent {

  /**
   * 竞赛 ID
   */
  private final Long competitionId;

  /**
   * 竞赛名称
   */
  private final String competitionName;

  /**
   * 原状态
   */
  private final Competition.Status oldStatus;

  /**
   * 新状态
   */
  private final Competition.Status newStatus;

  /**
   * 状态变更时间
   */
  private final LocalDateTime changedAt;

  /**
   * 是否为管理员手动触发
   */
  private final boolean manualOverride;

  /**
   * 构造函数
   *
   * @param source 事件源
   * @param competitionId 竞赛 ID
   * @param competitionName 竞赛名称
   * @param oldStatus 原状态
   * @param newStatus 新状态
   * @param changedAt 变更时间
   * @param manualOverride 是否为手动覆盖
   */
  public CompetitionStatusChangedEvent(Object source,
                                        Long competitionId,
                                        String competitionName,
                                        Competition.Status oldStatus,
                                        Competition.Status newStatus,
                                        LocalDateTime changedAt,
                                        boolean manualOverride) {
    super(source);
    this.competitionId = competitionId;
    this.competitionName = competitionName;
    this.oldStatus = oldStatus;
    this.newStatus = newStatus;
    this.changedAt = changedAt;
    this.manualOverride = manualOverride;
  }

  /**
   * 便捷构造函数（非手动覆盖）
   */
  public CompetitionStatusChangedEvent(Object source,
                                        Long competitionId,
                                        String competitionName,
                                        Competition.Status oldStatus,
                                        Competition.Status newStatus,
                                        LocalDateTime changedAt) {
    this(source, competitionId, competitionName, oldStatus, newStatus, changedAt, false);
  }

  @Override
  public String toString() {
    return String.format("CompetitionStatusChangedEvent[id=%d, name='%s', %s -> %s, at=%s, manual=%s]",
        competitionId, competitionName, oldStatus, newStatus, changedAt, manualOverride);
  }
}
