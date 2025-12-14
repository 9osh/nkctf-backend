package cn.edu.ndky.nkctf.controller;

import cn.edu.ndky.nkctf.dto.Result;
import cn.edu.ndky.nkctf.dto.response.LeaderboardResponse;
import cn.edu.ndky.nkctf.service.LeaderboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 排行榜控制器
 */
@Tag(name = "排行榜", description = "排行榜相关接口")
@RestController
@RequestMapping("/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

  private final LeaderboardService leaderboardService;

  @Operation(summary = "获取排行榜", description = "分页获取用户排行榜，每页 50 条")
  @GetMapping
  public Result<LeaderboardResponse> getLeaderboard(
      @Parameter(description = "页码（从 1 开始）")
      @RequestParam(required = false, defaultValue = "1") Integer page) {
    return Result.success(leaderboardService.getLeaderboard(page));
  }
}
