package cn.edu.ndky.nkctf.mapper;

import cn.edu.ndky.nkctf.entity.CompetitionTeam;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.UUID;

/**
 * 队伍参赛 Mapper
 */
@Mapper
public interface CompetitionTeamMapper extends BaseMapper<CompetitionTeam> {

  /**
   * 检查队伍是否参加了指定竞赛
   */
  @Select("""
      SELECT COUNT(*) > 0 FROM competition_team
      WHERE competition_id = #{competitionId} AND team_id = #{teamId}
      """)
  Boolean isTeamInCompetition(@Param("competitionId") Long competitionId, @Param("teamId") UUID teamId);

  /**
   * 获取竞赛的所有参赛队伍 ID
   */
  @Select("""
      SELECT team_id FROM competition_team
      WHERE competition_id = #{competitionId}
      """)
  List<UUID> getCompetitionTeamIds(@Param("competitionId") Long competitionId);

  /**
   * 统计竞赛参赛队伍数
   */
  @Select("""
      SELECT COUNT(*) FROM competition_team
      WHERE competition_id = #{competitionId}
      """)
  Integer countTeamsByCompetition(@Param("competitionId") Long competitionId);

  /**
   * 获取队伍在竞赛中的分数
   */
  @Select("""
      SELECT score FROM competition_team
      WHERE competition_id = #{competitionId} AND team_id = #{teamId}
      """)
  Integer getTeamScore(@Param("competitionId") Long competitionId, @Param("teamId") UUID teamId);

  /**
   * 更新队伍在竞赛中的分数
   */
  @Update("""
      UPDATE competition_team
      SET score = score + #{points}
      WHERE competition_id = #{competitionId} AND team_id = #{teamId}
      """)
  void addTeamScore(@Param("competitionId") Long competitionId,
                    @Param("teamId") UUID teamId,
                    @Param("points") Integer points);

  /**
   * 获取队伍在竞赛中的排名
   */
  @Select("""
      SELECT COUNT(*) + 1 FROM competition_team ct1
      WHERE ct1.competition_id = #{competitionId}
        AND ct1.score > (
          SELECT ct2.score FROM competition_team ct2
          WHERE ct2.competition_id = #{competitionId} AND ct2.team_id = #{teamId}
        )
      """)
  Integer getTeamRank(@Param("competitionId") Long competitionId, @Param("teamId") UUID teamId);
}
