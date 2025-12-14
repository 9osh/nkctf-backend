package cn.edu.ndky.nkctf.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 转让队长请求 DTO
 *
 * <h2>安全说明</h2>
 * <ul>
 *   <li>newCaptainId: 新队长的用户 ID，需要验证该用户是当前队伍的成员</li>
 *   <li>当前用户身份从 JWT Token 中提取，绝不信任请求参数</li>
 *   <li>操作前会验证当前用户是队长且新队长是队伍成员</li>
 * </ul>
 */
@Data
public class TransferCaptainRequest {

  @NotNull(message = "新队长 ID 不能为空")
  private Long newCaptainId;
}
