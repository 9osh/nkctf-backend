package cn.edu.ndky.nkctf.service;

import cn.edu.ndky.nkctf.entity.Competition;

import java.time.LocalDateTime;

/**
 * 竞赛状态服务
 *
 * 提供竞赛状态的计算、同步和管理功能：
 * - 根据时间计算状态（纯函数）
 * - 同步数据库中的状态（供调度器调用）
 * - 管理员状态覆盖
 */
public interface CompetitionStatusService {

  /**
   * 同步所有竞赛的状态
   *
   * 扫描数据库中需要更新状态的竞赛，并更新它们的状态。
   * 只更新没有 status_override 的竞赛。
   *
   * @param currentTime 当前时间（用于计算状态）
   * @return 更新的竞赛数量
   */
  int synchronizeStatuses(LocalDateTime currentTime);

  /**
   * 计算竞赛的当前状态
   *
   * 根据竞赛的 start_time 和 end_time 计算应该处于的状态。
   * 如果设置了 status_override，则返回覆盖值。
   *
   * @param competition 竞赛实体
   * @return 计算出的状态
   */
  Competition.Status computeCurrentStatus(Competition competition);

  /**
   * 纯函数：根据时间计算状态
   *
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @param currentTime 当前时间
   * @return 计算出的状态
   */
  Competition.Status computeStatus(LocalDateTime startTime,
                                    LocalDateTime endTime,
                                    LocalDateTime currentTime);

  /**
   * 获取有效状态（考虑覆盖）
   *
   * 如果设置了 status_override，返回覆盖值；
   * 否则返回根据时间计算的状态。
   *
   * @param competition 竞赛实体
   * @return 有效状态
   */
  Competition.Status getEffectiveStatus(Competition competition);

  /**
   * 设置状态覆盖
   *
   * 管理员可以手动设置竞赛状态，覆盖自动计算的值。
   *
   * @param competitionId 竞赛 ID
   * @param status 要设置的状态
   */
  void setStatusOverride(Long competitionId, Competition.Status status);

  /**
   * 清除状态覆盖
   *
   * 清除管理员设置的状态覆盖，恢复为自动计算。
   *
   * @param competitionId 竞赛 ID
   */
  void clearStatusOverride(Long competitionId);

  /**
   * 检查竞赛是否可查看（active 或 ending）
   *
   * @param competition 竞赛实体
   * @return 是否可查看
   */
  boolean isCompetitionViewable(Competition competition);

  /**
   * 检查竞赛是否可计分（仅 active）
   *
   * @param competition 竞赛实体
   * @return 是否可计分
   */
  boolean isCompetitionScorable(Competition competition);
}
