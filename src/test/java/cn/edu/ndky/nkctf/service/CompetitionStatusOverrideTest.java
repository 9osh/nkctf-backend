package cn.edu.ndky.nkctf.service;

import cn.edu.ndky.nkctf.entity.Competition;
import cn.edu.ndky.nkctf.event.CompetitionStatusChangedEvent;
import cn.edu.ndky.nkctf.mapper.CompetitionMapper;
import cn.edu.ndky.nkctf.service.impl.CompetitionStatusServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 竞赛状态覆盖功能测试
 *
 * 测试场景：
 * 1. 覆盖设置后调度器应跳过该竞赛
 * 2. 覆盖清除后应恢复正常的时间计算
 * 3. 边界情况：竞赛结束后设置覆盖
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("竞赛状态覆盖功能测试")
class CompetitionStatusOverrideTest {

  @Mock
  private CompetitionMapper competitionMapper;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  private CompetitionStatusService statusService;

  // 固定时间点用于测试
  private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2025, 6, 15, 14, 0, 0);
  private static final Clock FIXED_CLOCK = Clock.fixed(
      FIXED_NOW.atZone(ZoneId.systemDefault()).toInstant(),
      ZoneId.systemDefault()
  );

  @Captor
  private ArgumentCaptor<CompetitionStatusChangedEvent> eventCaptor;

  @BeforeEach
  void setUp() {
    statusService = new CompetitionStatusServiceImpl(competitionMapper, eventPublisher, FIXED_CLOCK);
  }

  @Nested
  @DisplayName("覆盖设置测试")
  class SetOverrideTest {

    @Test
    @DisplayName("设置状态覆盖应更新数据库并发布事件")
    void setStatusOverride_updatesDbAndPublishesEvent() {
      // Arrange
      Competition competition = createCompetition(1L, "Test CTF",
          FIXED_NOW.minusHours(2), FIXED_NOW.plusHours(2), "active", null);
      when(competitionMapper.selectById(1L)).thenReturn(competition);
      when(competitionMapper.setStatusOverride(eq(1L), eq("inactive"), any())).thenReturn(1);

      // Act
      statusService.setStatusOverride(1L, Competition.Status.INACTIVE);

      // Assert
      verify(competitionMapper).setStatusOverride(eq(1L), eq("inactive"), any(LocalDateTime.class));
      verify(eventPublisher).publishEvent(eventCaptor.capture());

      CompetitionStatusChangedEvent event = eventCaptor.getValue();
      assertThat(event.getCompetitionId()).isEqualTo(1L);
      assertThat(event.getOldStatus()).isEqualTo(Competition.Status.ACTIVE);
      assertThat(event.getNewStatus()).isEqualTo(Competition.Status.INACTIVE);
      assertThat(event.isManualOverride()).isTrue();
    }

    @Test
    @DisplayName("设置覆盖后 getEffectiveStatus 应返回覆盖值")
    void setStatusOverride_effectiveStatusReturnsOverride() {
      // Arrange
      Competition competition = createCompetition(1L, "Test CTF",
          FIXED_NOW.minusHours(2), FIXED_NOW.plusHours(2), "active", "inactive");

      // Act
      Competition.Status effectiveStatus = statusService.getEffectiveStatus(competition);

      // Assert
      assertThat(effectiveStatus).isEqualTo(Competition.Status.INACTIVE);
    }
  }

  @Nested
  @DisplayName("调度器跳过覆盖竞赛测试")
  class SchedulerSkipsOverrideTest {

    @Test
    @DisplayName("同步状态时应跳过有覆盖的竞赛（通过 SQL 过滤）")
    void synchronizeStatuses_skipsOverriddenCompetitions() {
      // Arrange - 模拟 SQL 查询已经过滤掉有覆盖的竞赛
      when(competitionMapper.findReadyToStart(any())).thenReturn(Collections.emptyList());
      when(competitionMapper.findReadyToEnd(any())).thenReturn(Collections.emptyList());

      // Act
      int updated = statusService.synchronizeStatuses(FIXED_NOW);

      // Assert
      assertThat(updated).isZero();
      verify(competitionMapper, never()).updateStatus(anyLong(), anyString(), any());
    }

    @Test
    @DisplayName("有覆盖的竞赛不应被调度器查询返回")
    void schedulerQueries_shouldNotReturnOverriddenCompetitions() {
      // 这个测试验证 SQL 查询行为
      // findReadyToStart 和 findReadyToEnd 应该只返回 status_override IS NULL 的竞赛

      // Arrange - 只有一个无覆盖的竞赛准备开始
      Competition noOverride = createCompetition(1L, "No Override",
          FIXED_NOW.minusMinutes(5), FIXED_NOW.plusHours(2), "inactive", null);
      when(competitionMapper.findReadyToStart(FIXED_NOW)).thenReturn(List.of(noOverride));
      when(competitionMapper.findReadyToEnd(FIXED_NOW)).thenReturn(Collections.emptyList());

      // Act
      int updated = statusService.synchronizeStatuses(FIXED_NOW);

      // Assert - 只有一个竞赛被更新
      assertThat(updated).isEqualTo(1);
      verify(competitionMapper).updateStatus(eq(1L), eq("active"), any());
    }

    @Test
    @DisplayName("覆盖设置后调度器不应改变状态")
    void overrideApplied_schedulerPreservesStatus() {
      // 场景：竞赛正在进行中（应该是 active），但管理员覆盖为 inactive
      // 调度器运行时不应该把它改回 active

      // Arrange - 模拟竞赛有覆盖（SQL 查询不会返回它）
      when(competitionMapper.findReadyToStart(any())).thenReturn(Collections.emptyList());
      when(competitionMapper.findReadyToEnd(any())).thenReturn(Collections.emptyList());

      // Act
      int updated = statusService.synchronizeStatuses(FIXED_NOW);

      // Assert
      assertThat(updated).isZero();
      // 验证 updateStatus 从未被调用（因为有覆盖的竞赛被 SQL 过滤）
      verify(competitionMapper, never()).updateStatus(anyLong(), anyString(), any());
    }
  }

  @Nested
  @DisplayName("覆盖清除测试")
  class ClearOverrideTest {

    @Test
    @DisplayName("清除覆盖后应恢复正常时间计算")
    void clearStatusOverride_resumesNormalCalculation() {
      // Arrange - 竞赛已结束但被覆盖为 active，现在清除覆盖
      Competition competition = createCompetition(1L, "Test CTF",
          FIXED_NOW.minusHours(4), FIXED_NOW.minusHours(2), "active", "active");
      when(competitionMapper.selectById(1L)).thenReturn(competition);
      when(competitionMapper.clearStatusOverride(eq(1L), eq("ending"), any())).thenReturn(1);

      // Act
      statusService.clearStatusOverride(1L);

      // Assert - 应该计算出 ending 状态（因为已过结束时间）
      verify(competitionMapper).clearStatusOverride(eq(1L), eq("ending"), any(LocalDateTime.class));
      verify(eventPublisher).publishEvent(eventCaptor.capture());

      CompetitionStatusChangedEvent event = eventCaptor.getValue();
      assertThat(event.getOldStatus()).isEqualTo(Competition.Status.ACTIVE);
      assertThat(event.getNewStatus()).isEqualTo(Competition.Status.ENDING);
      assertThat(event.isManualOverride()).isTrue();
    }

    @Test
    @DisplayName("清除覆盖后无覆盖竞赛应被调度器正常处理")
    void clearOverride_schedulerResumesNormalUpdates() {
      // 场景：清除覆盖后，竞赛应该回到调度器的管理之下

      // Arrange - 清除覆盖后的竞赛现在可以被调度器查询
      Competition competition = createCompetition(2L, "Resumed CTF",
          FIXED_NOW.minusHours(1), FIXED_NOW.plusHours(1), "inactive", null);
      when(competitionMapper.findReadyToStart(FIXED_NOW)).thenReturn(List.of(competition));
      when(competitionMapper.findReadyToEnd(FIXED_NOW)).thenReturn(Collections.emptyList());

      // Act
      int updated = statusService.synchronizeStatuses(FIXED_NOW);

      // Assert
      assertThat(updated).isEqualTo(1);
      verify(competitionMapper).updateStatus(eq(2L), eq("active"), any());
    }

    @Test
    @DisplayName("清除覆盖时如果状态未变则不发布事件")
    void clearOverride_noEventIfStatusUnchanged() {
      // Arrange - 竞赛正在进行，覆盖也是 active，清除后仍是 active
      Competition competition = createCompetition(1L, "Test CTF",
          FIXED_NOW.minusHours(1), FIXED_NOW.plusHours(1), "active", "active");
      when(competitionMapper.selectById(1L)).thenReturn(competition);
      when(competitionMapper.clearStatusOverride(eq(1L), eq("active"), any())).thenReturn(1);

      // Act
      statusService.clearStatusOverride(1L);

      // Assert - 不应发布事件（状态没有变化）
      verify(eventPublisher, never()).publishEvent(any());
    }
  }

  @Nested
  @DisplayName("边界情况测试")
  class EdgeCaseTest {

    @Test
    @DisplayName("竞赛结束后设置覆盖为 active，调度器不应改回 ending")
    void overrideAfterEnd_schedulerMustNotRevert() {
      // 场景：竞赛已经结束（ending），管理员覆盖为 active
      // 调度器不应该把它改回 ending

      // Arrange - 有覆盖的竞赛不会被 findReadyToEnd 返回
      when(competitionMapper.findReadyToStart(any())).thenReturn(Collections.emptyList());
      when(competitionMapper.findReadyToEnd(any())).thenReturn(Collections.emptyList());

      // Act
      int updated = statusService.synchronizeStatuses(FIXED_NOW);

      // Assert
      assertThat(updated).isZero();
      verify(competitionMapper, never()).updateStatus(anyLong(), anyString(), any());
    }

    @Test
    @DisplayName("getEffectiveStatus 对已结束但覆盖为 active 的竞赛返回 active")
    void getEffectiveStatus_respectsOverrideAfterEnd() {
      // Arrange
      Competition competition = createCompetition(1L, "Ended but Active",
          FIXED_NOW.minusHours(4), FIXED_NOW.minusHours(2), "active", "active");

      // Act
      Competition.Status status = statusService.getEffectiveStatus(competition);

      // Assert
      assertThat(status).isEqualTo(Competition.Status.ACTIVE);
    }

    @Test
    @DisplayName("多次覆盖遵循最后写入优先")
    void multipleOverrides_lastWriteWins() {
      // Arrange
      Competition competition = createCompetition(1L, "Test CTF",
          FIXED_NOW.minusHours(1), FIXED_NOW.plusHours(1), "active", null);
      when(competitionMapper.selectById(1L)).thenReturn(competition);
      when(competitionMapper.setStatusOverride(anyLong(), anyString(), any())).thenReturn(1);

      // Act - 先设为 inactive，再设为 ending
      statusService.setStatusOverride(1L, Competition.Status.INACTIVE);

      // 更新 mock 返回值模拟第二次调用
      Competition afterFirstOverride = createCompetition(1L, "Test CTF",
          FIXED_NOW.minusHours(1), FIXED_NOW.plusHours(1), "inactive", "inactive");
      when(competitionMapper.selectById(1L)).thenReturn(afterFirstOverride);

      statusService.setStatusOverride(1L, Competition.Status.ENDING);

      // Assert - 第二次调用应该生效
      verify(competitionMapper, times(2)).setStatusOverride(anyLong(), anyString(), any());
      verify(competitionMapper).setStatusOverride(eq(1L), eq("ending"), any());
    }

    @Test
    @DisplayName("未开始的竞赛覆盖为 active 应被尊重")
    void overrideBeforeStart_shouldBeRespected() {
      // Arrange - 竞赛尚未开始但覆盖为 active
      Competition competition = createCompetition(1L, "Early Start",
          FIXED_NOW.plusHours(1), FIXED_NOW.plusHours(3), "active", "active");

      // Act
      Competition.Status status = statusService.getEffectiveStatus(competition);

      // Assert
      assertThat(status).isEqualTo(Competition.Status.ACTIVE);
    }

    @Test
    @DisplayName("无效的覆盖值应回退到计算值")
    void invalidOverride_fallsBackToComputed() {
      // Arrange - 设置了无效的覆盖值
      Competition competition = createCompetition(1L, "Test CTF",
          FIXED_NOW.minusHours(1), FIXED_NOW.plusHours(1), "active", "invalid_status");

      // Act
      Competition.Status status = statusService.getEffectiveStatus(competition);

      // Assert - 应该回退到计算值 (active，因为在开始和结束之间)
      assertThat(status).isEqualTo(Competition.Status.ACTIVE);
    }
  }

  @Nested
  @DisplayName("isCompetitionViewable 和 isCompetitionScorable 测试")
  class ViewableAndScorableTest {

    @Test
    @DisplayName("覆盖为 active 的已结束竞赛应可查看和计分")
    void overriddenAsActive_isViewableAndScorable() {
      // Arrange - 已结束但覆盖为 active
      Competition competition = createCompetition(1L, "Extended CTF",
          FIXED_NOW.minusHours(4), FIXED_NOW.minusHours(2), "active", "active");

      // Act & Assert
      assertThat(statusService.isCompetitionViewable(competition)).isTrue();
      assertThat(statusService.isCompetitionScorable(competition)).isTrue();
    }

    @Test
    @DisplayName("覆盖为 inactive 的进行中竞赛不应可计分")
    void overriddenAsInactive_isNotScorable() {
      // Arrange - 进行中但覆盖为 inactive（暂停）
      Competition competition = createCompetition(1L, "Paused CTF",
          FIXED_NOW.minusHours(1), FIXED_NOW.plusHours(1), "inactive", "inactive");

      // Act & Assert
      assertThat(statusService.isCompetitionViewable(competition)).isFalse();
      assertThat(statusService.isCompetitionScorable(competition)).isFalse();
    }

    @Test
    @DisplayName("覆盖为 ending 的进行中竞赛可查看但不可计分")
    void overriddenAsEnding_isViewableButNotScorable() {
      // Arrange - 进行中但覆盖为 ending（提前结束）
      Competition competition = createCompetition(1L, "Early End CTF",
          FIXED_NOW.minusHours(1), FIXED_NOW.plusHours(1), "ending", "ending");

      // Act & Assert
      assertThat(statusService.isCompetitionViewable(competition)).isTrue();
      assertThat(statusService.isCompetitionScorable(competition)).isFalse();
    }
  }

  // 辅助方法：创建测试用竞赛对象
  private Competition createCompetition(Long id, String name,
                                         LocalDateTime startTime, LocalDateTime endTime,
                                         String status, String statusOverride) {
    Competition competition = new Competition();
    competition.setId(id);
    competition.setName(name);
    competition.setStartTime(startTime);
    competition.setEndTime(endTime);
    competition.setStatus(status);
    competition.setStatusOverride(statusOverride);
    competition.setDeleted(0);
    return competition;
  }
}
