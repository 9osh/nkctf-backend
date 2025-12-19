package cn.edu.ndky.nkctf.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章标签实体
 */
@Data
@TableName("article_tag")
public class ArticleTag {

  @TableId(type = IdType.AUTO)
  private Long id;

  /**
   * 标签名称
   */
  private String name;

  /**
   * 标签描述
   */
  private String description;

  /**
   * 标签颜色（HEX格式）
   */
  private String color;

  /**
   * 排序顺序
   */
  private Integer sortOrder;

  @TableLogic
  private Integer deleted;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createTime;

  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updateTime;
}
