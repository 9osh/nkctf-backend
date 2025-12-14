package cn.edu.ndky.nkctf.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 加入队伍请求 DTO
 */
@Data
public class JoinTeamRequest {

  @NotBlank(message = "邀请码不能为空")
  private String inviteToken;
}
