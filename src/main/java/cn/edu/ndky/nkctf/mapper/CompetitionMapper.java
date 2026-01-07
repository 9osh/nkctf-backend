package cn.edu.ndky.nkctf.mapper;

import cn.edu.ndky.nkctf.entity.Competition;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 竞赛 Mapper
 */
@Mapper
public interface CompetitionMapper extends BaseMapper<Competition> {

  /**
   * 查找准备开始的竞赛（inactive 状态且 start_time 已过）
   * 使用部分索引 idx_competition_pending_start
   *
   * @param currentTime 当前时间
   * @return 需要转为 active 状态的竞赛列表
   */
  @Select("""
      SELECT * FROM competition
      WHERE status = 'inactive'
        AND status_override IS NULL
        AND start_time <= #{currentTime}
        AND deleted = 0
      """)
  List<Competition> findReadyToStart(@Param("currentTime") LocalDateTime currentTime);

  /**
   * 查找准备结束的竞赛（active 状态且 end_time 已过）
   * 使用部分索引 idx_competition_pending_end
   *
   * @param currentTime 当前时间
   * @return 需要转为 ending 状态的竞赛列表
   */
  @Select("""
      SELECT * FROM competition
      WHERE status = 'active'
        AND status_override IS NULL
        AND end_time <= #{currentTime}
        AND deleted = 0
      """)
  List<Competition> findReadyToEnd(@Param("currentTime") LocalDateTime currentTime);

  /**
   * 更新竞赛状态
   *
   * @param id 竞赛 ID
   * @param newStatus 新状态
   * @param updatedAt 更新时间
   * @return 影响的行数
   */
  @Update("""
      UPDATE competition
      SET status = #{newStatus}, status_updated_at = #{updatedAt}
      WHERE id = #{id}
      """)
  int updateStatus(@Param("id") Long id,
                   @Param("newStatus") String newStatus,
                   @Param("updatedAt") LocalDateTime updatedAt);

  /**
   * 设置状态覆盖
   *
   * @param id 竞赛 ID
   * @param statusOverride 状态覆盖值（inactive/active/ending）
   * @param updatedAt 更新时间
   * @return 影响的行数
   */
  @Update("""
      UPDATE competition
      SET status = #{statusOverride},
          status_override = #{statusOverride},
          status_updated_at = #{updatedAt}
      WHERE id = #{id}
      """)
  int setStatusOverride(@Param("id") Long id,
                        @Param("statusOverride") String statusOverride,
                        @Param("updatedAt") LocalDateTime updatedAt);

  /**
   * 清除状态覆盖（恢复为自动计算）
   *
   * @param id 竞赛 ID
   * @param computedStatus 根据时间计算的状态
   * @param updatedAt 更新时间
   * @return 影响的行数
   */
  @Update("""
      UPDATE competition
      SET status = #{computedStatus},
          status_override = NULL,
          status_updated_at = #{updatedAt}
      WHERE id = #{id}
      """)
  int clearStatusOverride(@Param("id") Long id,
                          @Param("computedStatus") String computedStatus,
                          @Param("updatedAt") LocalDateTime updatedAt);
}
