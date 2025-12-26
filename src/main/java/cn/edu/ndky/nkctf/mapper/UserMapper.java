package cn.edu.ndky.nkctf.mapper;

import cn.edu.ndky.nkctf.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

  /**
   * 查询用户排名（按分数降序）
   */
  @Select("""
      SELECT ranking FROM (
        SELECT id, RANK() OVER (ORDER BY score DESC, create_time ASC) as ranking
        FROM sys_user
        WHERE deleted = 0
      ) ranked
      WHERE id = #{userId}
      """)
  Integer getUserRank(@Param("userId") Long userId);

  /**
   * 获取排行榜数据（带分页）- 使用冗余字段优化
   * 排序规则：分数降序 -> 解题数量升序 -> 最后提交时间升序
   */
  @Select("""
      WITH ranked_users AS (
        SELECT
          id as user_id,
          nickname,
          avatar,
          COALESCE(score, 0) as points,
          COALESCE(solved_count, 0) as solved_count,
          last_submit_time,
          RANK() OVER (
            ORDER BY COALESCE(score, 0) DESC,
                     COALESCE(solved_count, 0) ASC,
                     COALESCE(last_submit_time, '9999-12-31'::timestamp) ASC
          ) as rank
        FROM sys_user
        WHERE deleted = 0 AND enabled = true
      )
      SELECT * FROM ranked_users
      ORDER BY rank
      LIMIT #{limit} OFFSET #{offset}
      """)
  List<Map<String, Object>> getLeaderboardPage(@Param("limit") int limit, @Param("offset") int offset);

  /**
   * 获取排行榜总人数
   */
  @Select("""
      SELECT COUNT(*)
      FROM sys_user
      WHERE deleted = 0 AND enabled = true
      """)
  Long countLeaderboardUsers();

  /**
   * 获取指定用户的排名信息 - 使用冗余字段优化
   * 排序规则：分数降序 -> 解题数量升序 -> 最后提交时间升序
   */
  @Select("""
      WITH ranked_users AS (
        SELECT
          id as user_id,
          nickname,
          avatar,
          COALESCE(score, 0) as points,
          COALESCE(solved_count, 0) as solved_count,
          last_submit_time,
          RANK() OVER (
            ORDER BY COALESCE(score, 0) DESC,
                     COALESCE(solved_count, 0) ASC,
                     COALESCE(last_submit_time, '9999-12-31'::timestamp) ASC
          ) as rank
        FROM sys_user
        WHERE deleted = 0 AND enabled = true
      )
      SELECT * FROM ranked_users
      WHERE user_id = #{userId}
      """)
  Map<String, Object> getUserLeaderboardEntry(@Param("userId") Long userId);

  /**
   * 更新用户排行榜冗余字段（解题成功后调用）
   */
  @Update("""
      UPDATE sys_user SET
        solved_count = COALESCE((
          SELECT COUNT(DISTINCT challenge_id)
          FROM submission
          WHERE user_id = #{userId} AND is_correct = TRUE
        ), 0),
        last_submit_time = (
          SELECT MAX(create_time)
          FROM submission
          WHERE user_id = #{userId} AND is_correct = TRUE
        )
      WHERE id = #{userId}
      """)
  void updateLeaderboardFields(@Param("userId") Long userId);

  /**
   * 增加/减少用户积分（练习模式题目分值变更时使用）
   */
  @Update("""
      UPDATE sys_user
      SET score = COALESCE(score, 0) + #{pointsDiff}
      WHERE id = #{userId}
      """)
  void addUserScore(@Param("userId") Long userId, @Param("pointsDiff") int pointsDiff);
}
