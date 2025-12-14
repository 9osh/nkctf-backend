package cn.edu.ndky.nkctf.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * IP 限流注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

  /**
   * 时间窗口（秒）
   */
  int window() default 60;

  /**
   * 最大请求次数
   */
  int maxRequests() default 5;

  /**
   * 限流 key 前缀
   */
  String prefix() default "nkctf:rate:";

  /**
   * 限流提示消息
   */
  String message() default "请求过于频繁，请稍后再试";
}
