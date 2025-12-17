package cn.edu.ndky.nkctf.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 启动容器请求
 */
@Data
public class StartContainerRequest {

  @NotNull(message = "题目ID不能为空")
  private Long challengeId;

  /**
   * 竞赛ID（可选，竞赛模式时传入）
   */
  private Long competitionId;
}
