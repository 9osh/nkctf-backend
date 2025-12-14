package cn.edu.ndky.nkctf.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 竞赛详情响应 DTO
 */
@Data
@Builder
public class CompetitionDetailResponse {

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

  /**
   * 题目列表（仅已报名且比赛进行中/已结束时返回）
   */
  private List<CompetitionChallengeItem> challenges;

  /**
   * 竞赛题目项
   */
  @Data
  @Builder
  public static class CompetitionChallengeItem {
    private Long id;
    private String title;
    private String description;
    private String category;
    private String difficulty;
    private Integer points;
    private Integer solves;
    private Boolean solved;
  }
}
