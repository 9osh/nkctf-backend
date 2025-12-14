package cn.edu.ndky.nkctf.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 用户资料响应 DTO
 */
@Data
@Builder
public class UserProfileResponse {

  private Long id;

  private String username;

  private String nickname;

  private String avatar;

  private String bio;

  private Integer points;

  private Integer rank;

  private Integer solvedCount;

  private UserTeam team;

  private Map<String, Integer> solveStats;

  /**
   * 各分类题目总数
   */
  private Map<String, Integer> categoryTotals;

  private List<ActivityData> activityData;

  private String joinedAt;

  /**
   * 用户团队信息
   */
  @Data
  @Builder
  public static class UserTeam {
    private String id;
    private String name;
    /**
     * 队伍角色：CAPTAIN（队长）或 MEMBER（成员）
     */
    private String role;
  }

  /**
   * 活动数据（热力图）
   */
  @Data
  @Builder
  public static class ActivityData {
    private String date;
    private Integer count;
  }
}
