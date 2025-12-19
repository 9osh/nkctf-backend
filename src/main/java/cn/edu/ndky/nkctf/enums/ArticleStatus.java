package cn.edu.ndky.nkctf.enums;

/**
 * 文章状态枚举
 */
public enum ArticleStatus {
  DRAFT("草稿"),
  PENDING("待审核"),
  PUBLISHED("已发布"),
  REJECTED("已拒绝");

  private final String description;

  ArticleStatus(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
