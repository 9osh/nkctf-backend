package cn.edu.ndky.nkctf.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学习指南文章实体
 */
@Data
@TableName("article")
public class Article {

  @TableId(type = IdType.AUTO)
  private Long id;

  /**
   * 文章标题
   */
  private String title;

  /**
   * 文章摘要
   */
  private String summary;

  /**
   * Markdown 内容
   */
  private String content;

  /**
   * 作者ID
   */
  private Long authorId;

  /**
   * 文章状态: DRAFT, PENDING, PUBLISHED, REJECTED
   */
  private String status;

  /**
   * 审核人ID
   */
  private Long reviewerId;

  /**
   * 审核意见（拒绝时填写）
   */
  private String reviewComment;

  /**
   * 审核时间
   */
  private LocalDateTime reviewTime;

  /**
   * 发布时间
   */
  private LocalDateTime publishTime;

  /**
   * 浏览量
   */
  private Integer viewCount;

  @TableLogic
  private Integer deleted;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createTime;

  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updateTime;
}
