package cn.edu.ndky.nkctf.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 挑战详情响应 DTO
 */
@Data
@Builder
public class ChallengeDetailResponse {

  private Long id;

  private String title;

  private String description;

  private String category;

  private String difficulty;

  private Integer points;

  /**
   * 解题人数
   */
  private Integer solves;

  /**
   * 当前用户是否已解决
   */
  private Boolean solved;

  /**
   * 发布时间
   */
  private String releaseDate;

  /**
   * 出题人
   */
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
   * 提示响应
   */
  @Data
  @Builder
  public static class HintResponse {
    private Long id;
    private Integer cost;
    private Boolean unlocked;
    /**
     * 仅当 unlocked=true 时返回
     */
    private String content;
  }

  /**
   * 附件响应
   */
  @Data
  @Builder
  public static class AttachmentResponse {
    private String name;
    private String url;
  }
}
