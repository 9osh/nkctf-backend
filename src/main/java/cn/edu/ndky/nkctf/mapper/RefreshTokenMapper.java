package cn.edu.ndky.nkctf.mapper;

import cn.edu.ndky.nkctf.entity.RefreshToken;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * Refresh Token Mapper
 */
@Mapper
public interface RefreshTokenMapper extends BaseMapper<RefreshToken> {

  /**
   * 撤销用户的所有 Refresh Token
   */
  @Update("""
      UPDATE refresh_token SET revoked = TRUE
      WHERE user_id = #{userId} AND revoked = FALSE
      """)
  int revokeAllByUserId(@Param("userId") Long userId);

  /**
   * 撤销指定的 Refresh Token
   */
  @Update("""
      UPDATE refresh_token SET revoked = TRUE
      WHERE token = #{token} AND revoked = FALSE
      """)
  int revokeByToken(@Param("token") String token);

  /**
   * 删除已过期或已撤销的 Refresh Token
   * @return 删除的记录数
   */
  @Delete("""
      DELETE FROM refresh_token
      WHERE revoked = TRUE OR expires_at < NOW()
      """)
  int deleteExpiredAndRevoked();
}
