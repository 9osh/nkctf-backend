package cn.edu.ndky.nkctf.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 竞赛列表项响应 DTO
 */
@Data
@Builder
public class CompetitionListItemResponse {

  private Long id;

  private String name;

  private String description;

  /**
   * 是否为团队赛
   */
  private Boolean isTeamCompetition;

  /**
   * 状态: inactive, active, ending
   */
  private String status;

  private String startTime;

  private String endTime;

  /**
   * 参赛队伍/选手数量
   */
  private Integer participantCount;

  /**
   * 当前用户/队伍是否已报名
   */
  private Boolean registered;
}
