package cn.edu.ndky.nkctf.service;

import cn.edu.ndky.nkctf.dto.request.SubmitFlagRequest;
import cn.edu.ndky.nkctf.dto.response.SubmitFlagResponse;

/**
 * 提交服务接口
 */
public interface SubmissionService {

  /**
   * 提交 Flag
   * @param request 提交请求
   * @return 提交结果
   */
  SubmitFlagResponse submitFlag(SubmitFlagRequest request);
}
