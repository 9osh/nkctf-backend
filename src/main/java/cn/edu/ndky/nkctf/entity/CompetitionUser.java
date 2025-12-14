package cn.edu.ndky.nkctf.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 个人参赛实体 (个人赛)
 */
@Data
@TableName("competition_user")
public class CompetitionUser {

  @TableId(type = IdType.AUTO)
  private Long id;

  /**
   * 竞赛 ID
   */
  private Long competitionId;

  /**
   * 用户 ID
   */
  private Long userId;

  /**
   * 竞赛得分
   */
  private Integer score;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createTime;
}
