package cn.edu.ndky.nkctf.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 队伍实体
 */
@Data
@TableName("team")
public class Team {

  /**
   * 队伍 ID (UUID)
   */
  @TableId(type = IdType.INPUT)
  private UUID id;

  /**
   * 队伍名称（唯一）
   */
  private String name;

  /**
   * 队伍描述
   */
  private String description;

  /**
   * 邀请令牌 (UUID)，队长可刷新
   */
  private UUID inviteToken;

  /**
   * 队长用户 ID
   */
  private Long captainId;

  @TableLogic
  private Integer deleted;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createTime;

  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updateTime;
}
