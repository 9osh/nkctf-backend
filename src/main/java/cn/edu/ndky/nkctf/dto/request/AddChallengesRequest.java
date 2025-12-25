package cn.edu.ndky.nkctf.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 添加题目到竞赛请求 DTO
 */
@Data
public class AddChallengesRequest {

  @NotNull(message = "题目 ID 列表不能为空")
  @Size(min = 1, message = "至少添加一道题目")
  private List<Long> challengeIds;
}
