package cn.edu.ndky.nkctf.service;

import cn.edu.ndky.nkctf.entity.Competition;
import cn.edu.ndky.nkctf.service.impl.CompetitionStatusServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CompetitionStatusService 单元测试
 *
 * 测试状态计算的纯函数逻辑，不依赖数据库或其他外部服务。
 */
@DisplayName("竞赛状态服务测试")
class CompetitionStatusServiceTest {

  @Nested
  @DisplayName("状态计算测试 (computeStatus)")
  class ComputeStatusTest {

    @Test
    @DisplayName("竞赛开始前应返回 INACTIVE 状态")
    void beforeStart_returnsInactive() {
      LocalDateTime start = LocalDateTime.of(2025, 1, 15, 9, 0);
      LocalDateTime end = LocalDateTime.of(2025, 1, 15, 18, 0);
      LocalDateTime current = LocalDateTime.of(2025, 1, 15, 8, 59, 59);

      Competition.Status result = CompetitionStatusServiceImpl.doComputeStatus(start, end, current);

      assertThat(result).isEqualTo(Competition.Status.INACTIVE);
    }

    @Test
    @DisplayName("竞赛开始前一秒应返回 INACTIVE 状态")
    void oneSecondBeforeStart_returnsInactive() {
      LocalDateTime start = LocalDateTime.of(2025, 1, 15, 9, 0, 0);
      LocalDateTime end = LocalDateTime.of(2025, 1, 15, 18, 0, 0);
      LocalDateTime current = LocalDateTime.of(2025, 1, 15, 8, 59, 59);

      Competition.Status result = CompetitionStatusServiceImpl.doComputeStatus(start, end, current);

      assertThat(result).isEqualTo(Competition.Status.INACTIVE);
    }

    @Test
    @DisplayName("竞赛开始时刻应返回 ACTIVE 状态")
    void exactlyAtStart_returnsActive() {
      LocalDateTime start = LocalDateTime.of(2025, 1, 15, 9, 0, 0);
      LocalDateTime end = LocalDateTime.of(2025, 1, 15, 18, 0, 0);
      LocalDateTime current = LocalDateTime.of(2025, 1, 15, 9, 0, 0);

      Competition.Status result = CompetitionStatusServiceImpl.doComputeStatus(start, end, current);

      assertThat(result).isEqualTo(Competition.Status.ACTIVE);
    }

    @Test
    @DisplayName("竞赛进行中应返回 ACTIVE 状态")
    void duringCompetition_returnsActive() {
      LocalDateTime start = LocalDateTime.of(2025, 1, 15, 9, 0);
      LocalDateTime end = LocalDateTime.of(2025, 1, 15, 18, 0);
      LocalDateTime current = LocalDateTime.of(2025, 1, 15, 14, 30);

      Competition.Status result = CompetitionStatusServiceImpl.doComputeStatus(start, end, current);

      assertThat(result).isEqualTo(Competition.Status.ACTIVE);
    }

    @Test
    @DisplayName("竞赛结束前一秒应返回 ACTIVE 状态")
    void oneSecondBeforeEnd_returnsActive() {
      LocalDateTime start = LocalDateTime.of(2025, 1, 15, 9, 0, 0);
      LocalDateTime end = LocalDateTime.of(2025, 1, 15, 18, 0, 0);
      LocalDateTime current = LocalDateTime.of(2025, 1, 15, 17, 59, 59);

      Competition.Status result = CompetitionStatusServiceImpl.doComputeStatus(start, end, current);

      assertThat(result).isEqualTo(Competition.Status.ACTIVE);
    }

    @Test
    @DisplayName("竞赛结束时刻应返回 ENDING 状态")
    void exactlyAtEnd_returnsEnding() {
      LocalDateTime start = LocalDateTime.of(2025, 1, 15, 9, 0, 0);
      LocalDateTime end = LocalDateTime.of(2025, 1, 15, 18, 0, 0);
      LocalDateTime current = LocalDateTime.of(2025, 1, 15, 18, 0, 0);

      Competition.Status result = CompetitionStatusServiceImpl.doComputeStatus(start, end, current);

      assertThat(result).isEqualTo(Competition.Status.ENDING);
    }

    @Test
    @DisplayName("竞赛结束后应返回 ENDING 状态")
    void afterEnd_returnsEnding() {
      LocalDateTime start = LocalDateTime.of(2025, 1, 15, 9, 0);
      LocalDateTime end = LocalDateTime.of(2025, 1, 15, 18, 0);
      LocalDateTime current = LocalDateTime.of(2025, 1, 20, 10, 0);

      Competition.Status result = CompetitionStatusServiceImpl.doComputeStatus(start, end, current);

      assertThat(result).isEqualTo(Competition.Status.ENDING);
    }

    @Test
    @DisplayName("长时间结束后仍应返回 ENDING 状态")
    void longAfterEnd_returnsEnding() {
      LocalDateTime start = LocalDateTime.of(2025, 1, 15, 9, 0);
      LocalDateTime end = LocalDateTime.of(2025, 1, 15, 18, 0);
      LocalDateTime current = LocalDateTime.of(2026, 6, 1, 10, 0);  // 一年后

      Competition.Status result = CompetitionStatusServiceImpl.doComputeStatus(start, end, current);

      assertThat(result).isEqualTo(Competition.Status.ENDING);
    }
  }

  @Nested
  @DisplayName("边界条件测试")
  class BoundaryConditionTest {

    @Test
    @DisplayName("跨天竞赛状态计算正确")
    void multiDayCompetition_statusCorrect() {
      LocalDateTime start = LocalDateTime.of(2025, 1, 15, 9, 0);
      LocalDateTime end = LocalDateTime.of(2025, 1, 17, 18, 0);  // 跨越3天

      // 第一天结束时
      Competition.Status day1End = CompetitionStatusServiceImpl.doComputeStatus(
          start, end, LocalDateTime.of(2025, 1, 15, 23, 59));
      assertThat(day1End).isEqualTo(Competition.Status.ACTIVE);

      // 第二天中间
      Competition.Status day2Middle = CompetitionStatusServiceImpl.doComputeStatus(
          start, end, LocalDateTime.of(2025, 1, 16, 12, 0));
      assertThat(day2Middle).isEqualTo(Competition.Status.ACTIVE);

      // 第三天结束后
      Competition.Status day3After = CompetitionStatusServiceImpl.doComputeStatus(
          start, end, LocalDateTime.of(2025, 1, 17, 18, 0, 1));
      assertThat(day3After).isEqualTo(Competition.Status.ENDING);
    }

    @Test
    @DisplayName("短时间竞赛（1小时）状态计算正确")
    void shortCompetition_statusCorrect() {
      LocalDateTime start = LocalDateTime.of(2025, 1, 15, 14, 0);
      LocalDateTime end = LocalDateTime.of(2025, 1, 15, 15, 0);  // 只有1小时

      Competition.Status beforeStart = CompetitionStatusServiceImpl.doComputeStatus(
          start, end, LocalDateTime.of(2025, 1, 15, 13, 59));
      assertThat(beforeStart).isEqualTo(Competition.Status.INACTIVE);

      Competition.Status duringFirst = CompetitionStatusServiceImpl.doComputeStatus(
          start, end, LocalDateTime.of(2025, 1, 15, 14, 30));
      assertThat(duringFirst).isEqualTo(Competition.Status.ACTIVE);

      Competition.Status afterEnd = CompetitionStatusServiceImpl.doComputeStatus(
          start, end, LocalDateTime.of(2025, 1, 15, 15, 0, 1));
      assertThat(afterEnd).isEqualTo(Competition.Status.ENDING);
    }
  }

  @Nested
  @DisplayName("状态枚举测试")
  class StatusEnumTest {

    @Test
    @DisplayName("INACTIVE 状态值正确")
    void inactiveStatus_valueCorrect() {
      assertThat(Competition.Status.INACTIVE.getValue()).isEqualTo("inactive");
    }

    @Test
    @DisplayName("ACTIVE 状态值正确")
    void activeStatus_valueCorrect() {
      assertThat(Competition.Status.ACTIVE.getValue()).isEqualTo("active");
    }

    @Test
    @DisplayName("ENDING 状态值正确")
    void endingStatus_valueCorrect() {
      assertThat(Competition.Status.ENDING.getValue()).isEqualTo("ending");
    }
  }
}
