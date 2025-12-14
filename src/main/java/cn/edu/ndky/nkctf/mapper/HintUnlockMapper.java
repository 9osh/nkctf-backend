package cn.edu.ndky.nkctf.mapper;

import cn.edu.ndky.nkctf.entity.HintUnlock;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 提示获取记录 Mapper
 */
@Mapper
public interface HintUnlockMapper extends BaseMapper<HintUnlock> {

  /**
   * 获取用户已解锁的提示 ID 列表
   */
  @Select("""
      SELECT hint_id FROM hint_unlock
      WHERE user_id = #{userId}
      """)
  List<Long> getUnlockedHintIds(@Param("userId") Long userId);

  /**
   * 检查用户是否已解锁某提示
   */
  @Select("""
      SELECT COUNT(*) > 0 FROM hint_unlock
      WHERE user_id = #{userId} AND hint_id = #{hintId}
      """)
  Boolean hasUserUnlocked(@Param("userId") Long userId, @Param("hintId") Long hintId);
}
