package cn.edu.ndky.nkctf.mapper;

import cn.edu.ndky.nkctf.entity.CompetitionUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 用户参赛 Mapper (个人赛)
 */
@Mapper
public interface CompetitionUserMapper extends BaseMapper<CompetitionUser> {

  /**
   * 检查用户是否参加了指定竞赛 (个人赛)
   */
  @Select("""
      SELECT COUNT(*) > 0 FROM competition_user
      WHERE competition_id = #{competitionId} AND user_id = #{userId}
      """)
  Boolean isUserInCompetition(@Param("competitionId") Long competitionId,
                              @Param("userId") Long userId);

  /**
   * 获取竞赛的所有参赛用户 ID
   */
  @Select("""
      SELECT user_id FROM competition_user
      WHERE competition_id = #{competitionId}
      """)
  List<Long> getCompetitionUserIds(@Param("competitionId") Long competitionId);

  /**
   * 统计竞赛参赛人数
   */
  @Select("""
      SELECT COUNT(*) FROM competition_user
      WHERE competition_id = #{competitionId}
      """)
  Integer countUsersByCompetition(@Param("competitionId") Long competitionId);

  /**
   * 获取用户在竞赛中的分数
   */
  @Select("""
      SELECT score FROM competition_user
      WHERE competition_id = #{competitionId} AND user_id = #{userId}
      """)
  Integer getUserScore(@Param("competitionId") Long competitionId, @Param("userId") Long userId);

  /**
   * 更新用户在竞赛中的分数
   */
  @Update("""
      UPDATE competition_user
      SET score = score + #{points}
      WHERE competition_id = #{competitionId} AND user_id = #{userId}
      """)
  void addUserScore(@Param("competitionId") Long competitionId,
                    @Param("userId") Long userId,
                    @Param("points") Integer points);

  /**
   * 获取用户在竞赛中的排名
   */
  @Select("""
      SELECT COUNT(*) + 1 FROM competition_user cu1
      WHERE cu1.competition_id = #{competitionId}
        AND cu1.score > (
          SELECT cu2.score FROM competition_user cu2
          WHERE cu2.competition_id = #{competitionId} AND cu2.user_id = #{userId}
        )
      """)
  Integer getUserRank(@Param("competitionId") Long competitionId, @Param("userId") Long userId);
}
