package cn.edu.ndky.nkctf.interceptor;

import cn.edu.ndky.nkctf.annotation.RateLimit;
import cn.edu.ndky.nkctf.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * IP 限流拦截器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

  private final RedisTemplate<String, Object> redisTemplate;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
      Object handler) {
    if (!(handler instanceof HandlerMethod handlerMethod)) {
      return true;
    }

    RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
    if (rateLimit == null) {
      return true;
    }

    String ip = getClientIp(request);
    String uri = request.getRequestURI();
    String key = rateLimit.prefix() + uri + ":" + ip;

    Long count = redisTemplate.opsForValue().increment(key);
    if (count == 1) {
      redisTemplate.expire(key, rateLimit.window(), TimeUnit.SECONDS);
    }

    if (count > rateLimit.maxRequests()) {
      log.warn("IP 限流触发: {} - {} - 请求次数: {}", ip, uri, count);
      throw new BusinessException(429, rateLimit.message());
    }

    return true;
  }

  /**
   * 获取客户端真实 IP
   */
  private String getClientIp(HttpServletRequest request) {
    String[] headers = {
        "X-Forwarded-For",
        "X-Real-IP",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP"
    };

    for (String header : headers) {
      String ip = request.getHeader(header);
      if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
        // X-Forwarded-For 可能包含多个 IP，取第一个
        int index = ip.indexOf(',');
        if (index != -1) {
          ip = ip.substring(0, index).trim();
        }
        return ip;
      }
    }

    return request.getRemoteAddr();
  }
}
