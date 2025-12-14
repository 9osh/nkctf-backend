package cn.edu.ndky.nkctf.mapper;

import cn.edu.ndky.nkctf.entity.CompetitionChallenge;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 竞赛题目 Mapper
 */
@Mapper
public interface CompetitionChallengeMapper extends BaseMapper<CompetitionChallenge> {

  /**
   * 获取竞赛的所有题目 ID（按排序）
   */
  @Select("""
      SELECT challenge_id FROM competition_challenge
      WHERE competition_id = #{competitionId}
      ORDER BY sort_order ASC, id ASC
      """)
  List<Long> getCompetitionChallengeIds(@Param("competitionId") Long competitionId);

  /**
   * 检查题目是否属于指定竞赛
   */
  @Select("""
      SELECT COUNT(*) > 0 FROM competition_challenge
      WHERE competition_id = #{competitionId} AND challenge_id = #{challengeId}
      """)
  Boolean isChallengeInCompetition(@Param("competitionId") Long competitionId, @Param("challengeId") Long challengeId);
}
