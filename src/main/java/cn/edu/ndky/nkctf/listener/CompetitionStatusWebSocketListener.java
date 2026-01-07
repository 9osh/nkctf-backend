package cn.edu.ndky.nkctf.listener;

import cn.edu.ndky.nkctf.dto.response.CompetitionStatusNotification;
import cn.edu.ndky.nkctf.event.CompetitionStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 竞赛状态变更 WebSocket 广播监听器
 *
 * 监听竞赛状态变更事件，通过 WebSocket 广播给订阅的客户端。
 *
 * 广播频道：
 * - /topic/competition/{competitionId}/status: 特定竞赛的状态变更
 * - /topic/competitions/status: 全局竞赛状态变更
 *
 * 客户端订阅示例：
 * ```javascript
 * // 订阅特定竞赛
 * stompClient.subscribe('/topic/competition/1/status', callback);
 *
 * // 订阅全局变更
 * stompClient.subscribe('/topic/competitions/status', callback);
 * ```
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompetitionStatusWebSocketListener {

  private final SimpMessagingTemplate messagingTemplate;

  /**
   * 处理竞赛状态变更事件
   *
   * @param event 状态变更事件
   */
  @EventListener
  public void onStatusChanged(CompetitionStatusChangedEvent event) {
    log.info("广播竞赛状态变更: 竞赛 {} ({}) {} -> {}",
        event.getCompetitionId(),
        event.getCompetitionName(),
        event.getOldStatus().getValue(),
        event.getNewStatus().getValue()
    );

    // 构建通知消息
    CompetitionStatusNotification notification = CompetitionStatusNotification.builder()
        .competitionId(event.getCompetitionId())
        .competitionName(event.getCompetitionName())
        .oldStatus(event.getOldStatus().getValue())
        .newStatus(event.getNewStatus().getValue())
        .changedAt(event.getChangedAt().toString())
        .manualOverride(event.isManualOverride())
        .build();

    // 广播到特定竞赛频道
    String competitionChannel = "/topic/competition/" + event.getCompetitionId() + "/status";
    messagingTemplate.convertAndSend(competitionChannel, notification);
    log.debug("已广播到频道: {}", competitionChannel);

    // 广播到全局频道
    String globalChannel = "/topic/competitions/status";
    messagingTemplate.convertAndSend(globalChannel, notification);
    log.debug("已广播到频道: {}", globalChannel);
  }
}
