package cn.edu.ndky.nkctf.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户队伍关联实体
 */
@Data
@TableName("team_member")
public class TeamMember {

  @TableId(type = IdType.AUTO)
  private Long id;

  /**
   * 队伍 ID (UUID)
   */
  private UUID teamId;

  /**
   * 用户 ID
   */
  private Long userId;

  /**
   * 角色: CAPTAIN(队长), MEMBER(成员)
   */
  private String role;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createTime;

  /**
   * 成员角色枚举
   */
  public enum Role {
    CAPTAIN("CAPTAIN"),
    MEMBER("MEMBER");

    private final String value;

    Role(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }
}
