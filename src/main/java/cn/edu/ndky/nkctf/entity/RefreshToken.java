package cn.edu.ndky.nkctf.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Refresh Token 实体
 */
@Data
@TableName("refresh_token")
public class RefreshToken {

  @TableId(type = IdType.AUTO)
  private Long id;

  /**
   * Token 值 (高熵随机 UUID)
   */
  private String token;

  /**
   * 关联用户 ID
   */
  private Long userId;

  /**
   * 客户端 User-Agent
   */
  private String userAgent;

  /**
   * 客户端 IP 地址
   */
  private String ipAddress;

  /**
   * 过期时间
   */
  private LocalDateTime expiresAt;

  /**
   * 是否已撤销
   */
  private Boolean revoked;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createTime;
}
