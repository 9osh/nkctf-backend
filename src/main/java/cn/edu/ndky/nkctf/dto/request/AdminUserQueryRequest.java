package cn.edu.ndky.nkctf.dto.request;

import lombok.Data;

/**
 * 管理员用户列表查询请求 DTO
 */
@Data
public class AdminUserQueryRequest {

  /**
   * 每页大小（固定值）
   */
  public static final int PAGE_SIZE = 30;

  /**
   * 角色过滤 (USER, ADMIN)
   */
  private String role;

  /**
   * 启用状态过滤
   */
  private Boolean enabled;

  /**
   * 关键词搜索（用户名或邮箱）
   */
  private String keyword;

  /**
   * 当前页码（从 1 开始）
   */
  private Integer page;

  /**
   * 获取规范化的页码（确保至少为 1）
   */
  public int getPageNum() {
    return page == null || page < 1 ? 1 : page;
  }

  /**
   * 获取每页大小
   */
  public int getPageSize() {
    return PAGE_SIZE;
  }
}
