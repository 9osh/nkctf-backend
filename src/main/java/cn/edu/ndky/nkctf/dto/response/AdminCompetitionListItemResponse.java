package cn.edu.ndky.nkctf.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 管理员竞赛列表项响应 DTO
 */
@Data
@Builder
public class AdminCompetitionListItemResponse {

  private Long id;

  private String name;

  private String description;

  private Boolean isTeamCompetition;

  private String status;

  private String startTime;

  private String endTime;

  /**
   * 参赛人数/团队数
   */
  private Integer participantCount;

  /**
   * 题目数量
   */
  private Integer challengeCount;

  private String createTime;
}
