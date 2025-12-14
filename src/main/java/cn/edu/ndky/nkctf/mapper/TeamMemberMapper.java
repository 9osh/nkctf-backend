package cn.edu.ndky.nkctf.mapper;

import cn.edu.ndky.nkctf.entity.TeamMember;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.UUID;

/**
 * 用户队伍关联 Mapper
 */
@Mapper
public interface TeamMemberMapper extends BaseMapper<TeamMember> {

  /**
   * 检查用户是否是指定队伍的成员
   */
  @Select("""
      SELECT COUNT(*) > 0 FROM team_member
      WHERE team_id = #{teamId} AND user_id = #{userId}
      """)
  Boolean isTeamMember(@Param("teamId") UUID teamId, @Param("userId") Long userId);

  /**
   * 获取队伍的所有成员用户 ID
   */
  @Select("""
      SELECT user_id FROM team_member
      WHERE team_id = #{teamId}
      """)
  List<Long> getTeamMemberIds(@Param("teamId") UUID teamId);
}
