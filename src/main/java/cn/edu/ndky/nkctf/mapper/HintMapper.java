package cn.edu.ndky.nkctf.mapper;

import cn.edu.ndky.nkctf.entity.Hint;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 题目提示 Mapper
 */
@Mapper
public interface HintMapper extends BaseMapper<Hint> {

  /**
   * 根据题目 ID 获取提示列表（按排序顺序）
   */
  @Select("""
      SELECT * FROM hint
      WHERE challenge_id = #{challengeId}
      ORDER BY sort_order ASC, id ASC
      """)
  List<Hint> findByChallengeId(@Param("challengeId") Long challengeId);
}
