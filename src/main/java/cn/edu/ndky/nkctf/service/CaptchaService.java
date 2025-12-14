package cn.edu.ndky.nkctf.service;

import cn.edu.ndky.nkctf.dto.response.CaptchaResponse;

/**
 * 验证码服务接口
 */
public interface CaptchaService {

  /**
   * 生成验证码
   * @return 验证码响应（包含 captchaId 和 Base64 图片）
   */
  CaptchaResponse generate();

  /**
   * 验证验证码
   * @param captchaId 验证码 ID
   * @param code 用户输入的验证码
   * @return 是否验证通过
   */
  boolean verify(String captchaId, String code);
}
