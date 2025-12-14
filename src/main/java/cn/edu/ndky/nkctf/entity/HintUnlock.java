package cn.edu.ndky.nkctf.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提示获取记录实体
 */
@Data
@TableName("hint_unlock")
public class HintUnlock {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long userId;

  private Long hintId;

  /**
   * 解锁时花费的积分
   */
  private Integer cost;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createTime;
}
