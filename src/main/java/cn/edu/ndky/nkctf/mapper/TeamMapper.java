package cn.edu.ndky.nkctf.mapper;

import cn.edu.ndky.nkctf.entity.Team;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 团队 Mapper
 */
@Mapper
public interface TeamMapper extends BaseMapper<Team> {

  /**
   * 检查队伍名称是否存在（包括已删除的队伍）
   */
  @Select("SELECT COUNT(*) > 0 FROM team WHERE name = #{name}")
  Boolean existsByNameIncludeDeleted(@Param("name") String name);

  /**
   * 物理删除已逻辑删除的队伍（用于释放队伍名称）
   */
  @Delete("DELETE FROM team WHERE name = #{name} AND deleted = 1")
  int physicalDeleteByName(@Param("name") String name);
}
