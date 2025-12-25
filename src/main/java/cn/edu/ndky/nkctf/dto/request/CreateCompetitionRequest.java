package cn.edu.ndky.nkctf.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建竞赛请求 DTO
 */
@Data
public class CreateCompetitionRequest {

  @NotBlank(message = "竞赛名称不能为空")
  @Size(max = 200, message = "名称长度不能超过 200 字符")
  private String name;

  private String description;

  @NotNull(message = "竞赛类型不能为空")
  private Boolean isTeamCompetition;

  @NotNull(message = "开始时间不能为空")
  private LocalDateTime startTime;

  @NotNull(message = "结束时间不能为空")
  private LocalDateTime endTime;
}
