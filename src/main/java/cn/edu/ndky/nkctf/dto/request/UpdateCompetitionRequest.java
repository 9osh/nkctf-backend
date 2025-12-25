package cn.edu.ndky.nkctf.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 更新竞赛请求 DTO
 */
@Data
public class UpdateCompetitionRequest {

  @Size(max = 200, message = "名称长度不能超过 200 字符")
  private String name;

  private String description;

  /**
   * 竞赛类型（仅在 INACTIVE 状态下可修改）
   */
  private Boolean isTeamCompetition;

  private LocalDateTime startTime;

  private LocalDateTime endTime;
}
