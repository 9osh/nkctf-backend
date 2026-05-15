package cn.edu.ndky.nkctf.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 管理员题目详情响应 DTO
 */
@Data
@Builder
public class AdminChallengeDetailResponse {

  private Long id;

  private String title;

  private String description;

  /**
   * Markdown 内容
   */
  private String content;

  private String category;

  private String difficulty;

  private Integer points;

  // ========== 动态积分配置 ==========

  /**
   * 计分类型: STATIC, DYNAMIC
   */
  private String scoringType;

  /**
   * 动态积分最大值 (初始分值)
   */
  private Integer maxPoints;

  /**
   * 动态积分最小值 (下限)
   */
  private Integer minPoints;

  /**
   * 衰减参数 (达到最小值所需的解题数)
   */
  private Integer decay;

  private String author;

  /**
   * Flag（对管理员可见）
   */
  private String flag;

  private Boolean isDynamic;

  private String dockerImage;

  /**
   * 容器内服务端口；null 表示使用平台默认
   */
  private Integer dockerPort;

  private String attachmentUrl;

  private String attachmentName;

  private Boolean enabled;

  /**
   * 解题数
   */
  private Integer solves;

  /**
   * 提示列表
   */
  private List<AdminHintResponse> hints;

  private String createTime;

  private String updateTime;
}
