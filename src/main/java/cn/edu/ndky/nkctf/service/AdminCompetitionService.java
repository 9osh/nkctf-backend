package cn.edu.ndky.nkctf.service;

import cn.edu.ndky.nkctf.dto.request.*;
import cn.edu.ndky.nkctf.dto.response.*;

import java.util.List;

/**
 * 管理员竞赛管理服务接口
 */
public interface AdminCompetitionService {

  // ========== 竞赛 CRUD ==========

  /**
   * 获取竞赛列表（分页）
   */
  PageResponse<AdminCompetitionListItemResponse> getCompetitionList(
      AdminCompetitionQueryRequest request);

  /**
   * 获取竞赛详情
   */
  AdminCompetitionDetailResponse getCompetitionDetail(Long competitionId);

  /**
   * 创建竞赛
   */
  AdminCompetitionDetailResponse createCompetition(CreateCompetitionRequest request);

  /**
   * 更新竞赛
   */
  AdminCompetitionDetailResponse updateCompetition(Long competitionId,
      UpdateCompetitionRequest request);

  /**
   * 删除竞赛（软删除）
   */
  void deleteCompetition(Long competitionId);

  /**
   * 更改竞赛状态
   */
  void changeStatus(Long competitionId, ChangeStatusRequest request);

  // ========== 题目管理 ==========

  /**
   * 获取竞赛的题目列表
   */
  List<AdminCompetitionChallengeResponse> getChallenges(Long competitionId);

  /**
   * 添加题目到竞赛
   */
  List<AdminCompetitionChallengeResponse> addChallenges(Long competitionId,
      AddChallengesRequest request);

  /**
   * 从竞赛移除题目
   */
  void removeChallenge(Long competitionId, Long challengeId);

  /**
   * 重排序竞赛题目
   */
  List<AdminCompetitionChallengeResponse> reorderChallenges(Long competitionId,
      ReorderChallengesRequest request);

  // ========== 参赛者管理 ==========

  /**
   * 获取参赛者列表（分页）
   */
  PageResponse<AdminParticipantResponse> getParticipants(Long competitionId,
      AdminParticipantQueryRequest request);
}
