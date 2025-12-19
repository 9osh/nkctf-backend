package cn.edu.ndky.nkctf.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 文章列表项响应 DTO
 */
@Data
@Builder
public class ArticleListItemResponse {

  private Long id;

  private String title;

  private String summary;

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
   * 发布时间
   */
  private String publishTime;

  /**
   * 创建时间
   */
  private String createTime;

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
}
