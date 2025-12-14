package cn.edu.ndky.nkctf.dto.request;

import lombok.Data;

/**
 * 挑战列表查询请求 DTO
 */
@Data
public class ChallengeQueryRequest {

  /**
   * 每页大小（固定值）
   */
  public static final int PAGE_SIZE = 30;

  /**
   * 分类过滤 (web, pwn, crypto, reverse, misc, blockchain)
   */
  private String category;

  /**
   * 难度过滤 (EASY, MEDIUM, HARD)
   */
  private String difficulty;

  /**
   * 解决状态过滤 (true: 已解决, false: 未解决, null: 全部)
   */
  private Boolean solved;

  /**
   * 排序方式 (newest, most_solves, least_solves, highest_points)
   * 默认: newest
   */
  private String sortBy;

  /**
   * 当前页码（从 1 开始）
   * 默认: 1
   */
  private Integer page;

  /**
   * 获取规范化的页码（确保至少为 1）
   */
  public int getPageNum() {
    return page == null || page < 1 ? 1 : page;
  }

  /**
   * 获取每页大小（固定 30）
   */
  public int getPageSize() {
    return PAGE_SIZE;
  }
}
