package cn.edu.ndky.nkctf.service;

import cn.edu.ndky.nkctf.dto.request.CompetitionSubmitFlagRequest;
import cn.edu.ndky.nkctf.dto.response.CompetitionChallengeDetailResponse;
import cn.edu.ndky.nkctf.dto.response.CompetitionDetailResponse;
import cn.edu.ndky.nkctf.dto.response.CompetitionLeaderboardResponse;
import cn.edu.ndky.nkctf.dto.response.CompetitionListItemResponse;
import cn.edu.ndky.nkctf.dto.response.CompetitionSubmitFlagResponse;

import java.util.List;

/**
 * 竞赛服务接口
 */
public interface CompetitionService {

  /**
   * 获取竞赛列表
   * @return 竞赛列表
   */
  List<CompetitionListItemResponse> getCompetitionList();

  /**
   * 获取竞赛详情
   * @param competitionId 竞赛 ID
   * @return 竞赛详情（包含题目列表，需授权）
   */
  CompetitionDetailResponse getCompetitionDetail(Long competitionId);

  /**
   * 获取竞赛题目详情
   *
   * 安全说明：
   * - 用户身份从 JWT Token 中提取，不信任任何请求参数中的用户 ID
   * - 团队赛：验证用户所属队伍（从数据库获取）是否已报名该竞赛
   * - 个人赛：验证用户是否已报名该竞赛
   * - 竞赛状态必须为 active 或 ending
   *
   * @param competitionId 竞赛 ID
   * @param challengeId 题目 ID
   * @return 题目详情
   */
  CompetitionChallengeDetailResponse getCompetitionChallengeDetail(
      Long competitionId, Long challengeId);

  /**
   * 报名竞赛
   * @param competitionId 竞赛 ID
   */
  void registerCompetition(Long competitionId);

  /**
   * 获取竞赛排行榜
   *
   * 安全说明：
   * - 用户身份从 JWT Token 中提取，不信任任何请求参数
   * - 团队赛：只有参赛队伍成员可查看
   * - 个人赛：只有参赛用户可查看
   * - 竞赛状态必须为 active 或 ending
   *
   * @param competitionId 竞赛 ID
   * @return 排行榜
   */
  CompetitionLeaderboardResponse getCompetitionLeaderboard(Long competitionId);

  /**
   * 提交竞赛 Flag
   *
   * 安全说明：
   * - 用户身份从 JWT Token 中提取，不信任任何请求参数中的用户/队伍 ID
   * - 团队赛：只有参赛队伍成员可提交，得分记入队伍
   * - 个人赛：只有参赛用户可提交
   * - 只有 active 状态才计分，ending 状态可提交但不计分
   * - 提交记录使用服务端获取的 userId/teamId
   *
   * @param request 提交请求
   * @return 提交结果
   */
  CompetitionSubmitFlagResponse submitCompetitionFlag(CompetitionSubmitFlagRequest request);
}
