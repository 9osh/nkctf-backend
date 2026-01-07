package cn.edu.ndky.nkctf.listener;

import cn.edu.ndky.nkctf.event.CompetitionStatusChangedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 竞赛状态变更审计监听器
 *
 * 监听竞赛状态变更事件，记录审计日志。
 * 可扩展为将审计记录持久化到数据库。
 */
@Slf4j
@Component
public class CompetitionStatusAuditListener {

  /**
   * 处理竞赛状态变更事件
   *
   * @param event 状态变更事件
   */
  @EventListener
  public void onStatusChanged(CompetitionStatusChangedEvent event) {
    String trigger = event.isManualOverride() ? "管理员手动" : "系统自动";

    log.info("[审计] 竞赛状态变更 - ID: {}, 名称: '{}', 变更: {} -> {}, 触发方式: {}, 时间: {}",
        event.getCompetitionId(),
        event.getCompetitionName(),
        event.getOldStatus().getValue(),
        event.getNewStatus().getValue(),
        trigger,
        event.getChangedAt()
    );

    // TODO: 可扩展为将审计记录持久化到数据库
    // auditLogService.log(AuditLog.builder()
    //     .entityType("COMPETITION")
    //     .entityId(event.getCompetitionId())
    //     .action("STATUS_CHANGE")
    //     .oldValue(event.getOldStatus().getValue())
    //     .newValue(event.getNewStatus().getValue())
    //     .triggeredBy(event.isManualOverride() ? "ADMIN" : "SYSTEM")
    //     .timestamp(event.getChangedAt())
    //     .build());
  }
}
