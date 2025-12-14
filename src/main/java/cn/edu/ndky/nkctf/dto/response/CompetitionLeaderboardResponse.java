package cn.edu.ndky.nkctf.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 竞赛排行榜响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionLeaderboardResponse {

  /**
   * 竞赛 ID
   */
  private Long competitionId;

  /**
   * 竞赛名称
   */
  private String competitionName;

  /**
   * 是否为团队赛
   */
  private Boolean isTeamCompetition;

  /**
   * 排行榜条目（团队赛为队伍，个人赛为用户）
   */
  private List<LeaderboardEntry> entries;

  /**
   * 当前用户/队伍的排名信息
   */
  private LeaderboardEntry currentRank;

  /**
   * 排行榜条目
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LeaderboardEntry {
    /**
     * 排名
     */
    private Integer rank;

    /**
     * 队伍 ID（团队赛）或用户 ID（个人赛）
     */
    private String participantId;

    /**
     * 队伍名称（团队赛）或用户昵称（个人赛）
     */
    private String name;

    /**
     * 头像 URL（个人赛）
     */
    private String avatar;

    /**
     * 总分数
     */
    private Integer score;

    /**
     * 解题数量
     */
    private Integer solvedCount;

    /**
     * 最后提交时间（ISO 8601 格式）
     */
    private String lastSubmitTime;
  }
}
