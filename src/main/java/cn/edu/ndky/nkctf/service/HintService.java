package cn.edu.ndky.nkctf.service;

import cn.edu.ndky.nkctf.dto.request.UnlockHintRequest;
import cn.edu.ndky.nkctf.dto.response.UnlockHintResponse;

/**
 * 提示服务接口
 */
public interface HintService {

  /**
   * 解锁提示
   * @param request 解锁请求
   * @return 解锁结果
   */
  UnlockHintResponse unlockHint(UnlockHintRequest request);
}
