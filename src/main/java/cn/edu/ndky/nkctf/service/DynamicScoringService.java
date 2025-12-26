package cn.edu.ndky.nkctf.service;

import cn.edu.ndky.nkctf.entity.Challenge;

/**
 * 动态积分服务接口
 *
 * <p>使用 CTFd 风格的抛物线动态积分公式：</p>
 * <pre>
 * currentPoints = max(minPoints, ((minPoints - maxPoints) / decay²) × solves² + maxPoints)
 * </pre>
 *
 * <p>一血奖励：</p>
 * <ul>
 *   <li>1st solve: +10% bonus</li>
 *   <li>2nd solve: +5% bonus</li>
 *   <li>3rd solve: +3% bonus</li>
 * </ul>
 */
public interface DynamicScoringService {

  /**
   * 计分类型常量
   */
  String SCORING_TYPE_STATIC = "STATIC";
  String SCORING_TYPE_DYNAMIC = "DYNAMIC";

  /**
   * 计算题目当前分值
   *
   * @param challenge  题目实体
   * @param solveCount 当前解题人数（新解答之前）
   * @return 当前分值
   */
  int calculateCurrentPoints(Challenge challenge, int solveCount);

  /**
   * 计算一血奖励
   *
   * @param basePoints 基础分值
   * @param solveRank  解题排名 (1=一血, 2=二血, 3=三血)
   * @return 一血奖励分值 (不包含在基础分值中)
   */
  int calculateFirstBloodBonus(int basePoints, int solveRank);

  /**
   * 获取下一个解题排名
   *
   * @param competitionId     竞赛 ID
   * @param challengeId       题目 ID
   * @param isTeamCompetition 是否为团队赛
   * @return 下一个解题排名 (1, 2, 3, ...)
   */
  int getNextSolveRank(Long competitionId, Long challengeId, boolean isTeamCompetition);

  /**
   * 重新计算竞赛中某题目的所有积分
   *
   * <p>当新的解题出现时，需要重新计算所有已解决该题目的参赛者的积分</p>
   *
   * @param competitionId     竞赛 ID
   * @param challengeId       题目 ID
   * @param isTeamCompetition 是否为团队赛
   */
  void recalculateChallengeScores(Long competitionId, Long challengeId, boolean isTeamCompetition);

  /**
   * 重新计算整个竞赛的所有积分
   *
   * @param competitionId 竞赛 ID
   */
  void recalculateCompetitionScores(Long competitionId);

  /**
   * 判断题目是否使用动态积分
   *
   * @param challenge 题目实体
   * @return true 如果使用动态积分
   */
  default boolean isDynamicScoring(Challenge challenge) {
    return SCORING_TYPE_DYNAMIC.equals(challenge.getScoringType());
  }
}
