package cn.edu.ndky.nkctf.service;

import cn.edu.ndky.nkctf.dto.request.ChallengeQueryRequest;
import cn.edu.ndky.nkctf.dto.response.ChallengeDetailResponse;
import cn.edu.ndky.nkctf.dto.response.ChallengeListItemResponse;
import cn.edu.ndky.nkctf.dto.response.PageResponse;

/**
 * 挑战服务接口
 */
public interface ChallengeService {

  /**
   * 获取挑战列表（分页）
   * @param request 查询请求（包含分类、难度、解决状态过滤、排序方式和分页参数）
   * @return 分页挑战列表
   */
  PageResponse<ChallengeListItemResponse> getChallengeList(ChallengeQueryRequest request);

  /**
   * 获取挑战详情
   * @param challengeId 挑战 ID
   * @return 挑战详情
   */
  ChallengeDetailResponse getChallengeDetail(Long challengeId);
}
