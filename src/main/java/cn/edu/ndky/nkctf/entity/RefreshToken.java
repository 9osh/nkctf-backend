package cn.edu.ndky.nkctf.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Refresh Token 实体
 * 注意：数据库中存储的是 Token 的 SHA-256 哈希值，而非明文
 */
@Data
@TableName("refresh_token")
public class RefreshToken {

  @TableId(type = IdType.AUTO)
  private Long id;

  /**
   * Token 哈希值 (SHA-256, 64 字符十六进制)
   * 数据库中不存储明文 Token，提高安全性
   */
  private String tokenHash;

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
