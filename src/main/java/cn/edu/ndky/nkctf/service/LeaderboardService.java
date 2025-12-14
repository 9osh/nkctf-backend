package cn.edu.ndky.nkctf.service;

import cn.edu.ndky.nkctf.dto.response.LeaderboardResponse;

/**
 * 排行榜服务接口
 */
public interface LeaderboardService {

  /**
   * 获取排行榜（分页）
   * @param page 页码（从 1 开始）
   * @return 排行榜响应
   */
  LeaderboardResponse getLeaderboard(int page);

  /**
   * 清除排行榜缓存
   */
  void invalidateCache();

  /**
   * 用户解题成功后更新排行榜相关数据
   * 包括：更新冗余字段、清除缓存
   * @param userId 用户 ID
   */
  void onUserSolved(Long userId);
}
