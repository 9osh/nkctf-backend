package cn.edu.ndky.nkctf.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 解题记录实体
 */
@Data
@TableName("submission")
public class Submission {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long userId;

  private Long challengeId;

  /**
   * 竞赛 ID（竞赛提交时关联）
   */
  private Long competitionId;

  /**
   * 队伍 ID (UUID)（竞赛提交时关联）
   */
  private UUID teamId;

  private String flag;

  private Boolean isCorrect;

  private Integer pointsAwarded;

  /**
   * 解题排名 (1=一血, 2=二血, 3=三血, NULL=其他)
   * 仅竞赛模式有效
   */
  private Integer firstBloodRank;

  /**
   * 一血额外奖励积分
   */
  private Integer firstBloodBonus;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createTime;
}
