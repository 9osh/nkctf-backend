package cn.edu.ndky.nkctf.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 竞赛实体
 */
@Data
@TableName("competition")
public class Competition {

  @TableId(type = IdType.AUTO)
  private Long id;

  /**
   * 竞赛名称（唯一）
   */
  private String name;

  /**
   * 竞赛描述
   */
  private String description;

  /**
   * 是否为团队赛 (true: 团队赛, false: 个人赛)
   */
  private Boolean isTeamCompetition;

  /**
   * 竞赛状态: inactive(未开始), active(进行中), ending(已结束)
   */
  private String status;

  /**
   * 开始时间
   */
  private LocalDateTime startTime;

  /**
   * 结束时间
   */
  private LocalDateTime endTime;

  @TableLogic
  private Integer deleted;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createTime;

  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updateTime;

  /**
   * 竞赛状态枚举
   */
  public enum Status {
    INACTIVE("inactive"),
    ACTIVE("active"),
    ENDING("ending");

    private final String value;

    Status(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }
}
