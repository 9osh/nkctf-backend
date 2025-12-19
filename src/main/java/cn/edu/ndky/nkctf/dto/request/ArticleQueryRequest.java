package cn.edu.ndky.nkctf.dto.request;

import lombok.Data;

/**
 * 文章列表查询请求 DTO
 */
@Data
public class ArticleQueryRequest {

  public static final int PAGE_SIZE = 20;

  /**
   * 标签 ID 过滤
   */
  private Long tagId;

  /**
   * 状态过滤（管理员/作者使用）
   */
  private String status;

  /**
   * 作者 ID 过滤（管理员使用）
   */
  private Long authorId;

  /**
   * 搜索关键词（标题）
   */
  private String keyword;

  /**
   * 排序方式: newest, most_views
   */
  private String sortBy;

  /**
   * 当前页码（从 1 开始）
   */
  private Integer page;

  public int getPageNum() {
    return page == null || page < 1 ? 1 : page;
  }

  public int getPageSize() {
    return PAGE_SIZE;
  }
}
