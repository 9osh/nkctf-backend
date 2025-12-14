package cn.edu.ndky.nkctf.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题目实体
 */
@Data
@TableName("challenge")
public class Challenge {

  @TableId(type = IdType.AUTO)
  private Long id;

  private String title;

  /**
   * 简短描述（列表展示）
   */
  private String description;

  /**
   * 详细内容（详情展示）
   */
  private String content;

  private String category;

  private String difficulty;

  private Integer points;

  /**
   * 出题人
   */
  private String author;

  private String flag;

  private Boolean isDynamic;

  private String dockerImage;

  /**
   * 附件下载链接
   */
  private String attachmentUrl;

  /**
   * 附件文件名
   */
  private String attachmentName;

  private Boolean enabled;

  @TableLogic
  private Integer deleted;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createTime;

  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updateTime;
}
