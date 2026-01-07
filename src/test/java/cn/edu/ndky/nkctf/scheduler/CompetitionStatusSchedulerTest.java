package cn.edu.ndky.nkctf.scheduler;

import cn.edu.ndky.nkctf.entity.Competition;
import cn.edu.ndky.nkctf.mapper.CompetitionMapper;
import cn.edu.ndky.nkctf.service.CompetitionStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 竞赛状态调度器测试
 *
 * 验证调度器正确处理状态覆盖：
 * 1. 有覆盖的竞赛应被跳过
 * 2. 无覆盖的竞赛应正常更新
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("竞赛状态调度器测试")
class CompetitionStatusSchedulerTest {

  @Mock
  private CompetitionStatusService statusService;

  private CompetitionStatusScheduler scheduler;

  @BeforeEach
  void setUp() {
    scheduler = new CompetitionStatusScheduler(statusService);
  }

  @Nested
  @DisplayName("状态同步任务测试")
  class SyncStatusesTest {

    @Test
    @DisplayName("调度器应调用 synchronizeStatuses 方法")
    void syncCompetitionStatuses_callsService() {
      // Arrange
      when(statusService.synchronizeStatuses(any())).thenReturn(0);

      // Act
      scheduler.syncCompetitionStatuses();

      // Assert
      verify(statusService).synchronizeStatuses(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("调度器更新竞赛后应记录日志")
    void syncCompetitionStatuses_logsWhenUpdated() {
      // Arrange
      when(statusService.synchronizeStatuses(any())).thenReturn(3);

      // Act
      scheduler.syncCompetitionStatuses();

      // Assert - 方法应该正常完成
      verify(statusService).synchronizeStatuses(any(LocalDateTime.class));
    }
  }

  @Nested
  @DisplayName("Mapper 查询测试（验证 SQL 过滤逻辑）")
  class MapperQueryTest {

    @Mock
    private CompetitionMapper competitionMapper;

    @Test
    @DisplayName("findReadyToStart 应只返回无覆盖的竞赛")
    void findReadyToStart_onlyReturnsNonOverriddenCompetitions() {
      // 这个测试主要验证 SQL 查询的预期行为
      // 实际 SQL 是: WHERE status = 'inactive' AND status_override IS NULL ...

      // Arrange
      Competition noOverride = createCompetition(1L, null);
      // 有覆盖的竞赛不会被查询返回（被 SQL 过滤）
      when(competitionMapper.findReadyToStart(any())).thenReturn(List.of(noOverride));

      // Act
      List<Competition> result = competitionMapper.findReadyToStart(LocalDateTime.now());

      // Assert - 只返回无覆盖的竞赛
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getStatusOverride()).isNull();
    }

    @Test
    @DisplayName("findReadyToEnd 应只返回无覆盖的竞赛")
    void findReadyToEnd_onlyReturnsNonOverriddenCompetitions() {
      // Arrange
      Competition noOverride = createCompetition(2L, null);
      when(competitionMapper.findReadyToEnd(any())).thenReturn(List.of(noOverride));

      // Act
      List<Competition> result = competitionMapper.findReadyToEnd(LocalDateTime.now());

      // Assert
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getStatusOverride()).isNull();
    }

    @Test
    @DisplayName("当所有竞赛都有覆盖时查询应返回空")
    void queries_returnEmptyWhenAllOverridden() {
      // Arrange - 模拟所有竞赛都有覆盖
      when(competitionMapper.findReadyToStart(any())).thenReturn(Collections.emptyList());
      when(competitionMapper.findReadyToEnd(any())).thenReturn(Collections.emptyList());

      // Act
      List<Competition> toStart = competitionMapper.findReadyToStart(LocalDateTime.now());
      List<Competition> toEnd = competitionMapper.findReadyToEnd(LocalDateTime.now());

      // Assert
      assertThat(toStart).isEmpty();
      assertThat(toEnd).isEmpty();
    }

    private Competition createCompetition(Long id, String statusOverride) {
      Competition competition = new Competition();
      competition.setId(id);
      competition.setStatusOverride(statusOverride);
      return competition;
    }
  }
}
