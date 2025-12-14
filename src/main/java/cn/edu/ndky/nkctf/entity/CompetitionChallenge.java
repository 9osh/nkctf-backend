package cn.edu.ndky.nkctf.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 竞赛题目实体
 */
@Data
@TableName("competition_challenge")
public class CompetitionChallenge {

  @TableId(type = IdType.AUTO)
  private Long id;

  /**
   * 竞赛 ID
   */
  private Long competitionId;

  /**
   * 题目 ID
   */
  private Long challengeId;

  /**
   * 排序顺序
   */
  private Integer sortOrder;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createTime;
}
