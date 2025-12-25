package cn.edu.ndky.nkctf.service;

import cn.edu.ndky.nkctf.dto.request.*;
import cn.edu.ndky.nkctf.dto.response.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 管理员题目管理服务接口
 */
public interface AdminChallengeService {

  // ========== 题目 CRUD ==========

  /**
   * 获取题目列表（分页）
   */
  PageResponse<AdminChallengeListItemResponse> getChallengeList(AdminChallengeQueryRequest request);

  /**
   * 获取题目详情
   */
  AdminChallengeDetailResponse getChallengeDetail(Long challengeId);

  /**
   * 创建题目
   */
  AdminChallengeDetailResponse createChallenge(CreateChallengeRequest request);

  /**
   * 更新题目
   */
  AdminChallengeDetailResponse updateChallenge(Long challengeId, UpdateChallengeRequest request);

  /**
   * 删除题目（软删除）
   */
  void deleteChallenge(Long challengeId);

  /**
   * 切换题目启用状态
   */
  void toggleEnabled(Long challengeId, ToggleEnabledRequest request);

  // ========== 附件管理 ==========

  /**
   * 上传附件
   */
  AttachmentUploadResponse uploadAttachment(Long challengeId, MultipartFile file);

  /**
   * 删除附件
   */
  void deleteAttachment(Long challengeId);

  // ========== 提示管理 ==========

  /**
   * 获取题目提示列表
   */
  List<AdminHintResponse> getHints(Long challengeId);

  /**
   * 创建提示
   */
  AdminHintResponse createHint(Long challengeId, CreateHintRequest request);

  /**
   * 更新提示
   */
  AdminHintResponse updateHint(Long challengeId, Long hintId, UpdateHintRequest request);

  /**
   * 删除提示
   */
  void deleteHint(Long challengeId, Long hintId);

  /**
   * 重排序提示
   */
  List<AdminHintResponse> reorderHints(Long challengeId, ReorderHintsRequest request);
}
