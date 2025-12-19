package cn.edu.ndky.nkctf.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章-标签关联实体
 */
@Data
@TableName("article_tag_relation")
public class ArticleTagRelation {

  @TableId(type = IdType.AUTO)
  private Long id;

  /**
   * 文章ID
   */
  private Long articleId;

  /**
   * 标签ID
   */
  private Long tagId;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createTime;
}
