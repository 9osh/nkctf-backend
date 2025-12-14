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

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createTime;
}
