package cn.edu.ndky.nkctf.mapper;

import cn.edu.ndky.nkctf.entity.Submission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 解题记录 Mapper
 */
@Mapper
public interface SubmissionMapper extends BaseMapper<Submission> {

  /**
   * 统计用户各分类的解题数量
   */
  @Select("""
      SELECT c.category, COUNT(*) as count
      FROM submission s
      JOIN challenge c ON s.challenge_id = c.id
      WHERE s.user_id = #{userId} AND s.is_correct = true
      GROUP BY c.category
      """)
  List<Map<String, Object>> countSolvesByCategory(@Param("userId") Long userId);

  /**
   * 统计用户每日解题数量（最近365天）
   */
  @Select("""
      SELECT DATE(s.create_time) as date, COUNT(*) as count
      FROM submission s
      WHERE s.user_id = #{userId}
        AND s.is_correct = true
        AND s.create_time >= CURRENT_DATE - INTERVAL '365 days'
      GROUP BY DATE(s.create_time)
      ORDER BY date
      """)
  List<Map<String, Object>> countDailySolves(@Param("userId") Long userId);

  /**
   * 统计用户正确解题数量
   */
  @Select("""
      SELECT COUNT(DISTINCT challenge_id)
      FROM submission
      WHERE user_id = #{userId} AND is_correct = true
      """)
  Integer countCorrectSolves(@Param("userId") Long userId);

  /**
   * 统计题目解题人数
   */
  @Select("""
      SELECT COUNT(DISTINCT user_id)
      FROM submission
      WHERE challenge_id = #{challengeId} AND is_correct = true
      """)
  Integer countSolvesByChallengeId(@Param("challengeId") Long challengeId);

  /**
   * 检查用户是否已在练习模式解决某题目
   * 注意：只检查 competition_id IS NULL 的记录，不包含竞赛提交
   */
  @Select("""
      SELECT COUNT(*) > 0
      FROM submission
      WHERE user_id = #{userId}
        AND challenge_id = #{challengeId}
        AND is_correct = true
        AND competition_id IS NULL
      """)
  Boolean hasUserSolved(@Param("userId") Long userId, @Param("challengeId") Long challengeId);

  /**
   * 获取用户在练习模式已解决的题目 ID 列表
   * 注意：只返回 competition_id IS NULL 的记录，不包含竞赛提交
   */
  @Select("""
      SELECT DISTINCT challenge_id
      FROM submission
      WHERE user_id = #{userId}
        AND is_correct = true
        AND competition_id IS NULL
      """)
  List<Long> getSolvedChallengeIds(@Param("userId") Long userId);

  /**
   * 统计竞赛中题目的解题队伍数（团队赛）
   */
  @Select("""
      SELECT COUNT(DISTINCT team_id)
      FROM submission
      WHERE competition_id = #{competitionId}
        AND challenge_id = #{challengeId}
        AND is_correct = true
      """)
  Integer countCompetitionChallengeSolvesByTeam(
      @Param("competitionId") Long competitionId,
      @Param("challengeId") Long challengeId);

  /**
   * 统计竞赛中题目的解题人数（个人赛）
   */
  @Select("""
      SELECT COUNT(DISTINCT user_id)
      FROM submission
      WHERE competition_id = #{competitionId}
        AND challenge_id = #{challengeId}
        AND is_correct = true
      """)
  Integer countCompetitionChallengeSolvesByUser(
      @Param("competitionId") Long competitionId,
      @Param("challengeId") Long challengeId);

  /**
   * 检查队伍是否已解决竞赛中的某题目
   */
  @Select("""
      SELECT COUNT(*) > 0
      FROM submission
      WHERE competition_id = #{competitionId}
        AND team_id = #{teamId}
        AND challenge_id = #{challengeId}
        AND is_correct = true
      """)
  Boolean hasTeamSolvedInCompetition(
      @Param("competitionId") Long competitionId,
      @Param("teamId") UUID teamId,
      @Param("challengeId") Long challengeId);

  /**
   * 检查用户是否已解决竞赛中的某题目（个人赛）
   */
  @Select("""
      SELECT COUNT(*) > 0
      FROM submission
      WHERE competition_id = #{competitionId}
        AND user_id = #{userId}
        AND challenge_id = #{challengeId}
        AND is_correct = true
      """)
  Boolean hasUserSolvedInCompetition(
      @Param("competitionId") Long competitionId,
      @Param("userId") Long userId,
      @Param("challengeId") Long challengeId);

  /**
   * 获取队伍在竞赛中已解决的题目 ID 列表
   */
  @Select("""
      SELECT DISTINCT challenge_id
      FROM submission
      WHERE competition_id = #{competitionId}
        AND team_id = #{teamId}
        AND is_correct = true
      """)
  List<Long> getTeamSolvedChallengeIdsInCompetition(
      @Param("competitionId") Long competitionId,
      @Param("teamId") UUID teamId);

  /**
   * 获取用户在竞赛中已解决的题目 ID 列表（个人赛）
   */
  @Select("""
      SELECT DISTINCT challenge_id
      FROM submission
      WHERE competition_id = #{competitionId}
        AND user_id = #{userId}
        AND is_correct = true
      """)
  List<Long> getUserSolvedChallengeIdsInCompetition(
      @Param("competitionId") Long competitionId,
      @Param("userId") Long userId);

  /**
   * 获取竞赛中某题目的所有正确提交记录
   * 用于动态积分重新计算（无需排序，因为 first_blood_rank 已存储在数据库中）
   */
  @Select("""
      SELECT *
      FROM submission
      WHERE competition_id = #{competitionId}
        AND challenge_id = #{challengeId}
        AND is_correct = true
      """)
  List<Submission> getCorrectSubmissionsForChallenge(
      @Param("competitionId") Long competitionId,
      @Param("challengeId") Long challengeId);

  /**
   * 更新提交记录的积分（动态积分重新计算时使用）
   */
  @Update("""
      UPDATE submission
      SET points_awarded = #{pointsAwarded},
          first_blood_rank = #{firstBloodRank},
          first_blood_bonus = #{firstBloodBonus}
      WHERE id = #{submissionId}
      """)
  void updatePointsAwarded(
      @Param("submissionId") Long submissionId,
      @Param("pointsAwarded") Integer pointsAwarded,
      @Param("firstBloodRank") Integer firstBloodRank,
      @Param("firstBloodBonus") Integer firstBloodBonus);

  /**
   * 获取练习模式下某题目的下一个解题排名
   * 用于计算首次解题（一血）奖励
   */
  @Select("""
      SELECT COUNT(DISTINCT user_id) + 1
      FROM submission
      WHERE challenge_id = #{challengeId}
        AND is_correct = true
        AND competition_id IS NULL
      """)
  Integer getNextPracticeSolveRank(@Param("challengeId") Long challengeId);

  /**
   * 获取练习模式下某题目的所有正确提交记录
   * 用于题目分值变更时重新计算用户积分
   */
  @Select("""
      SELECT *
      FROM submission
      WHERE challenge_id = #{challengeId}
        AND is_correct = true
        AND competition_id IS NULL
      """)
  List<Submission> getPracticeSubmissionsForChallenge(@Param("challengeId") Long challengeId);
}
