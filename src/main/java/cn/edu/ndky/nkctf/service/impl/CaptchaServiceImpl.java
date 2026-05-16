package cn.edu.ndky.nkctf.service.impl;

import cn.edu.ndky.nkctf.dto.response.CaptchaResponse;
import cn.edu.ndky.nkctf.service.CaptchaService;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 验证码服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

  private static final String CAPTCHA_KEY = "nkctf:captcha:";
  private static final int CAPTCHA_EXPIRE_MINUTES = 5;

  private final StringRedisTemplate stringRedisTemplate;

  @Override
  public CaptchaResponse generate() {
    // 生成验证码（宽 130，高 48，4 位字符，干扰线 20 条）
    LineCaptcha captcha = CaptchaUtil.createLineCaptcha(130, 48, 4, 20);

    // 生成唯一 ID
    String captchaId = IdUtil.fastSimpleUUID();

    // 存储验证码到 Redis（5 分钟过期，忽略大小写存储小写）
    String code = captcha.getCode().toLowerCase();
    stringRedisTemplate.opsForValue().set(
        CAPTCHA_KEY + captchaId,
        code,
        CAPTCHA_EXPIRE_MINUTES,
        TimeUnit.MINUTES
    );

    log.debug("生成验证码: {} - {}", captchaId, code);

    return CaptchaResponse.builder()
        .captchaId(captchaId)
        .image(captcha.getImageBase64Data())
        .build();
  }

  @Override
  public boolean verify(String captchaId, String code) {
    if (captchaId == null || code == null) {
      return false;
    }

    String key = CAPTCHA_KEY + captchaId;
    String storedCode = stringRedisTemplate.opsForValue().get(key);

    if (storedCode == null) {
      log.warn("验证码不存在或已过期: {}", captchaId);
      return false;
    }

    // 验证后立即删除（一次性使用）
    stringRedisTemplate.delete(key);

    // 忽略大小写比较
    boolean result = code.toLowerCase().equals(storedCode);
    if (!result) {
      log.warn("验证码错误: {} - 期望: {}, 实际: {}", captchaId, storedCode, code);
    }

    return result;
  }
}
