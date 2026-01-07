package cn.edu.ndky.nkctf.service.impl;

import cn.edu.ndky.nkctf.entity.Competition;
import cn.edu.ndky.nkctf.event.CompetitionStatusChangedEvent;
import cn.edu.ndky.nkctf.exception.BusinessException;
import cn.edu.ndky.nkctf.mapper.CompetitionMapper;
import cn.edu.ndky.nkctf.service.CompetitionStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 竞赛状态服务实现
 *
 * 职责：
 * 1. 提供纯函数计算竞赛状态
 * 2. 同步数据库中的竞赛状态（供调度器调用）
 * 3. 发布状态变更事件
 * 4. 支持管理员状态覆盖
 *
 * 设计说明：
 * - 使用 Clock 注入以支持单元测试中的时间模拟
 * - 状态计算为纯函数，无副作用
 * - 状态同步在事务中执行，确保原子性
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompetitionStatusServiceImpl implements CompetitionStatusService {

  private final CompetitionMapper competitionMapper;
  private final ApplicationEventPublisher eventPublisher;
  private final Clock clock;

  @Override
  @Transactional
  public int synchronizeStatuses(LocalDateTime currentTime) {
    int totalUpdated = 0;

    // 1. 查找 inactive 状态且应该变为 active 的竞赛
    List<Competition> toActivate = competitionMapper.findReadyToStart(currentTime);
    for (Competition comp : toActivate) {
      updateStatusInternal(comp, Competition.Status.ACTIVE, currentTime, false);
      totalUpdated++;
    }

    // 2. 查找 active 状态且应该变为 ending 的竞赛
    List<Competition> toEnd = competitionMapper.findReadyToEnd(currentTime);
    for (Competition comp : toEnd) {
      updateStatusInternal(comp, Competition.Status.ENDING, currentTime, false);
      totalUpdated++;
    }

    return totalUpdated;
  }

  /**
   * 内部方法：更新状态并发布事件
   */
  private void updateStatusInternal(Competition competition,
                                     Competition.Status newStatus,
                                     LocalDateTime now,
                                     boolean manualOverride) {
    String oldStatusStr = competition.getStatus();
    Competition.Status oldStatus;
    try {
      oldStatus = Competition.Status.valueOf(oldStatusStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      oldStatus = Competition.Status.INACTIVE;
    }

    // 更新数据库
    competitionMapper.updateStatus(competition.getId(), newStatus.getValue(), now);

    log.info("竞赛 '{}' 状态变更: {} -> {} (手动: {})",
        competition.getName(), oldStatusStr, newStatus.getValue(), manualOverride);

    // 发布事件
    eventPublisher.publishEvent(new CompetitionStatusChangedEvent(
        this,
        competition.getId(),
        competition.getName(),
        oldStatus,
        newStatus,
        now,
        manualOverride
    ));
  }

  @Override
  public Competition.Status computeCurrentStatus(Competition competition) {
    return getEffectiveStatus(competition);
  }

  @Override
  public Competition.Status computeStatus(LocalDateTime startTime,
                                           LocalDateTime endTime,
                                           LocalDateTime currentTime) {
    return doComputeStatus(startTime, endTime, currentTime);
  }

  /**
   * 纯函数：根据时间计算状态（静态方法，方便测试）
   *
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @param currentTime 当前时间
   * @return 计算出的状态
   */
  public static Competition.Status doComputeStatus(LocalDateTime startTime,
                                                    LocalDateTime endTime,
                                                    LocalDateTime currentTime) {
    if (currentTime.isBefore(startTime)) {
      return Competition.Status.INACTIVE;
    } else if (currentTime.isBefore(endTime)) {
      return Competition.Status.ACTIVE;
    } else {
      return Competition.Status.ENDING;
    }
  }

  @Override
  public Competition.Status getEffectiveStatus(Competition competition) {
    // 如果设置了状态覆盖，使用覆盖值
    if (competition.getStatusOverride() != null && !competition.getStatusOverride().isBlank()) {
      try {
        return Competition.Status.valueOf(competition.getStatusOverride().toUpperCase());
      } catch (IllegalArgumentException e) {
        log.warn("无效的状态覆盖值: {}, 将使用计算值", competition.getStatusOverride());
      }
    }

    // 否则根据时间计算
    return computeStatus(
        competition.getStartTime(),
        competition.getEndTime(),
        LocalDateTime.now(clock)
    );
  }

  @Override
  @Transactional
  public void setStatusOverride(Long competitionId, Competition.Status status) {
    Competition competition = competitionMapper.selectById(competitionId);
    if (competition == null) {
      throw new BusinessException(404, "竞赛不存在");
    }

    String oldStatusStr = competition.getStatus();
    Competition.Status oldStatus;
    try {
      oldStatus = Competition.Status.valueOf(oldStatusStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      oldStatus = Competition.Status.INACTIVE;
    }

    LocalDateTime now = LocalDateTime.now(clock);

    // 更新数据库
    competitionMapper.setStatusOverride(competitionId, status.getValue(), now);

    log.info("管理员设置竞赛 '{}' 状态覆盖: {} -> {}",
        competition.getName(), oldStatusStr, status.getValue());

    // 发布事件
    eventPublisher.publishEvent(new CompetitionStatusChangedEvent(
        this,
        competition.getId(),
        competition.getName(),
        oldStatus,
        status,
        now,
        true
    ));
  }

  @Override
  @Transactional
  public void clearStatusOverride(Long competitionId) {
    Competition competition = competitionMapper.selectById(competitionId);
    if (competition == null) {
      throw new BusinessException(404, "竞赛不存在");
    }

    String oldStatusStr = competition.getStatus();
    Competition.Status oldStatus;
    try {
      oldStatus = Competition.Status.valueOf(oldStatusStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      oldStatus = Competition.Status.INACTIVE;
    }

    LocalDateTime now = LocalDateTime.now(clock);

    // 计算应该恢复到的状态
    Competition.Status computedStatus = computeStatus(
        competition.getStartTime(),
        competition.getEndTime(),
        now
    );

    // 更新数据库
    competitionMapper.clearStatusOverride(competitionId, computedStatus.getValue(), now);

    log.info("管理员清除竞赛 '{}' 状态覆盖: {} -> {} (计算值)",
        competition.getName(), oldStatusStr, computedStatus.getValue());

    // 如果状态发生了变化，发布事件
    if (oldStatus != computedStatus) {
      eventPublisher.publishEvent(new CompetitionStatusChangedEvent(
          this,
          competition.getId(),
          competition.getName(),
          oldStatus,
          computedStatus,
          now,
          true
      ));
    }
  }

  @Override
  public boolean isCompetitionViewable(Competition competition) {
    Competition.Status status = getEffectiveStatus(competition);
    return status == Competition.Status.ACTIVE || status == Competition.Status.ENDING;
  }

  @Override
  public boolean isCompetitionScorable(Competition competition) {
    return getEffectiveStatus(competition) == Competition.Status.ACTIVE;
  }
}
