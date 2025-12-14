package cn.edu.ndky.nkctf.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题目提示实体
 */
@Data
@TableName("hint")
public class Hint {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long challengeId;

  /**
   * 提示内容
   */
  private String content;

  /**
   * 解锁所需积分
   */
  private Integer cost;

  /**
   * 排序顺序
   */
  private Integer sortOrder;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createTime;

  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updateTime;
}
