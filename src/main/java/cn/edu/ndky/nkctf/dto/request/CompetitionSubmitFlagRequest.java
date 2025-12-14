package cn.edu.ndky.nkctf.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 竞赛 Flag 提交请求 DTO
 *
 * <p>安全说明：</p>
 * <ul>
 *   <li>competitionId: 资源标识符，可从请求接受</li>
 *   <li>challengeId: 资源标识符，可从请求接受</li>
 *   <li>flag: 用户提交的答案，可从请求接受</li>
 *   <li>userId/teamId: <b>绝不</b>从请求接受，必须从 JWT Token 中提取</li>
 * </ul>
 */
@Data
public class CompetitionSubmitFlagRequest {

  @NotNull(message = "竞赛 ID 不能为空")
  private Long competitionId;

  @NotNull(message = "题目 ID 不能为空")
  private Long challengeId;

  @NotBlank(message = "Flag 不能为空")
  @Size(max = 255, message = "Flag 长度不能超过 255 字符")
  private String flag;
}
