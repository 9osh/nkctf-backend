package cn.edu.ndky.nkctf.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 容器信息响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContainerInfoResponse {

  /**
   * 容器ID
   */
  private String containerId;

  /**
   * 题目ID
   */
  private Long challengeId;

  /**
   * 题目标题
   */
  private String challengeTitle;

  /**
   * 访问地址
   */
  private String host;

  /**
   * 访问端口
   */
  private Integer port;

  /**
   * 创建时间戳（毫秒）
   */
  private Long createTime;

  /**
   * 过期时间戳（毫秒）
   */
  private Long expireTime;

  /**
   * 剩余时间（秒）
   */
  private Long remainingSeconds;
}
