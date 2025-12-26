package cn.edu.ndky.nkctf.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 竞赛题目详情响应 DTO
 */
@Data
@Builder
public class CompetitionChallengeDetailResponse {

  private Long competitionId;

  private Long challengeId;

  private String title;

  private String description;

  private String category;

  private String difficulty;

  private Integer points;

  /**
   * 计分类型: STATIC-固定分值, DYNAMIC-动态积分
   */
  private String scoringType;

  /**
   * 动态积分当前值（根据解题数计算）
   */
  private Integer currentPoints;

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

  /**
   * 解题人数（竞赛内）
   */
  private Integer solves;

  /**
   * 当前用户/队伍是否已解决
   */
  private Boolean solved;

  private String author;

  /**
   * 详细内容
   */
  private String content;

  /**
   * 提示列表
   */
  private List<HintResponse> hints;

  /**
   * 附件列表
   */
  private List<AttachmentResponse> attachments;

  /**
   * 是否支持 Docker 容器 (用于前端渲染启动容器按钮)
   */
  private Boolean hasDocker;

  @Data
  @Builder
  public static class HintResponse {
    private Long id;
    private Integer cost;
    private Boolean unlocked;
    private String content;
  }

  @Data
  @Builder
  public static class AttachmentResponse {
    private String name;
    private String url;
  }
}
