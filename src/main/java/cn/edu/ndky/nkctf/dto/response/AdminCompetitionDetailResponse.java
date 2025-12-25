package cn.edu.ndky.nkctf.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 管理员竞赛详情响应 DTO
 */
@Data
@Builder
public class AdminCompetitionDetailResponse {

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
   * 竞赛题目列表
   */
  private List<AdminCompetitionChallengeResponse> challenges;

  private String createTime;

  private String updateTime;
}
