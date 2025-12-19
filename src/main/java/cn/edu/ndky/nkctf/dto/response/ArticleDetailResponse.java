package cn.edu.ndky.nkctf.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 文章详情响应 DTO
 */
@Data
@Builder
public class ArticleDetailResponse {

  private Long id;

  private String title;

  private String summary;

  /**
   * Markdown 内容
   */
  private String content;

  /**
   * 作者信息
   */
  private AuthorInfo author;

  /**
   * 文章状态
   */
  private String status;

  /**
   * 标签列表
   */
  private List<TagInfo> tags;

  /**
   * 浏览量
   */
  private Integer viewCount;

  /**
   * 审核意见（被拒绝时显示）
   */
  private String reviewComment;

  /**
   * 审核人信息
   */
  private ReviewerInfo reviewer;

  /**
   * 审核时间
   */
  private String reviewTime;

  /**
   * 发布时间
   */
  private String publishTime;

  /**
   * 创建时间
   */
  private String createTime;

  /**
   * 更新时间
   */
  private String updateTime;

  @Data
  @Builder
  public static class AuthorInfo {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
  }

  @Data
  @Builder
  public static class TagInfo {
    private Long id;
    private String name;
    private String color;
  }

  @Data
  @Builder
  public static class ReviewerInfo {
    private Long id;
    private String username;
    private String nickname;
  }
}
