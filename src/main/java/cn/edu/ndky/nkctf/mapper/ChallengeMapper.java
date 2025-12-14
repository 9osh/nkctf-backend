package cn.edu.ndky.nkctf.mapper;

import cn.edu.ndky.nkctf.entity.Challenge;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 题目 Mapper
 */
@Mapper
public interface ChallengeMapper extends BaseMapper<Challenge> {

  /**
   * 统计各分类的题目数量
   */
  @Select("""
      SELECT category, COUNT(*) as count
      FROM challenge
      WHERE deleted = 0 AND enabled = true
      GROUP BY category
      """)
  List<Map<String, Object>> countByCategory();
}
