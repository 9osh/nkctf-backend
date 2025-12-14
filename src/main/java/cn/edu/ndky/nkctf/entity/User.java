package cn.edu.ndky.nkctf.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户实体
 */
@Data
@TableName("sys_user")
public class User {

  @TableId(type = IdType.AUTO)
  private Long id;

  private String username;

  private String password;

  private String email;

  private String nickname;

  private String avatar;

  private String bio;

  /**
   * 角色: USER, ADMIN
   */
  private String role;

  /**
   * 是否启用
   */
  private Boolean enabled;

  /**
   * 得分
   */
  private Integer score;

  /**
   * 解题数量（冗余字段，用于排行榜优化）
   */
  private Integer solvedCount;

  /**
   * 最后正确提交时间（冗余字段，用于排行榜优化）
   */
  private LocalDateTime lastSubmitTime;

  /**
   * 团队ID (UUID)
   */
  private UUID teamId;

  /**
   * 逻辑删除
   */
  @TableLogic
  private Integer deleted;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createTime;

  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updateTime;
}
