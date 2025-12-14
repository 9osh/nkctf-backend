package cn.edu.ndky.nkctf.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 队伍参赛实体
 */
@Data
@TableName("competition_team")
public class CompetitionTeam {

  @TableId(type = IdType.AUTO)
  private Long id;

  /**
   * 竞赛 ID
   */
  private Long competitionId;

  /**
   * 队伍 ID (UUID)
   */
  private UUID teamId;

  /**
   * 队伍在该竞赛中的得分
   */
  private Integer score;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createTime;
}
