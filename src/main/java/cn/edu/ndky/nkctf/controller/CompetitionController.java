package cn.edu.ndky.nkctf.controller;

import cn.edu.ndky.nkctf.annotation.RateLimit;
import cn.edu.ndky.nkctf.dto.Result;
import cn.edu.ndky.nkctf.dto.request.CompetitionSubmitFlagRequest;
import cn.edu.ndky.nkctf.dto.response.CompetitionChallengeDetailResponse;
import cn.edu.ndky.nkctf.dto.response.CompetitionDetailResponse;
import cn.edu.ndky.nkctf.dto.response.CompetitionLeaderboardResponse;
import cn.edu.ndky.nkctf.dto.response.CompetitionListItemResponse;
import cn.edu.ndky.nkctf.dto.response.CompetitionSubmitFlagResponse;
import cn.edu.ndky.nkctf.service.CompetitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 竞赛控制器
 *
 * <h2>安全设计</h2>
 * <p>
 * 本控制器所有接口都不接受用户 ID 或队伍 ID 作为参数。
 * 用户身份通过 JWT Token 从 SecurityContext 获取，确保无法伪造。
 * </p>
 *
 * <h3>题目访问权限</h3>
 * <ul>
 *   <li>团队赛: 只有参赛队伍成员可查看题目</li>
 *   <li>个人赛: 只有报名用户可查看题目</li>
 *   <li>竞赛状态必须为 active 或 ending</li>
 * </ul>
 */
@Tag(name = "竞赛", description = "竞赛相关接口")
@RestController
@RequestMapping("/competitions")
@RequiredArgsConstructor
public class CompetitionController {

  private final CompetitionService competitionService;

  @Operation(summary = "获取竞赛列表", description = "获取所有竞赛的基本信息")
  @GetMapping
  public Result<List<CompetitionListItemResponse>> getCompetitionList() {
    return Result.success(competitionService.getCompetitionList());
  }

  @Operation(summary = "获取竞赛详情",
      description = "获取竞赛详情，如已报名且竞赛已开始则包含题目列表")
  @GetMapping("/{competitionId}")
  public Result<CompetitionDetailResponse> getCompetitionDetail(
      @Parameter(description = "竞赛 ID") @PathVariable Long competitionId) {
    return Result.success(competitionService.getCompetitionDetail(competitionId));
  }

  @Operation(summary = "获取竞赛题目详情",
      description = """
          获取竞赛中某道题目的详细信息。

          安全要求：
          - 用户必须已登录（JWT Token）
          - 团队赛：用户所属队伍必须已报名该竞赛
          - 个人赛：用户必须已报名该竞赛
          - 竞赛状态必须为 active 或 ending

          所有身份验证均在服务端完成，不信任任何请求参数中的用户/队伍 ID。
          """)
  @GetMapping("/{competitionId}/challenges/{challengeId}")
  public Result<CompetitionChallengeDetailResponse> getCompetitionChallengeDetail(
      @Parameter(description = "竞赛 ID") @PathVariable Long competitionId,
      @Parameter(description = "题目 ID") @PathVariable Long challengeId) {
    return Result.success(
        competitionService.getCompetitionChallengeDetail(competitionId, challengeId));
  }

  @Operation(summary = "报名竞赛",
      description = """
          报名参加竞赛。
          - 团队赛：以当前用户所在队伍报名
          - 个人赛：以当前用户身份报名
          - 只有未开始的竞赛可以报名
          """)
  @PostMapping("/{competitionId}/register")
  public Result<Void> registerCompetition(
      @Parameter(description = "竞赛 ID") @PathVariable Long competitionId) {
    competitionService.registerCompetition(competitionId);
    return Result.success();
  }

  @Operation(summary = "获取竞赛排行榜",
      description = """
          获取竞赛排行榜。

          安全要求：
          - 用户必须已登录（JWT Token）
          - 团队赛：用户所属队伍必须已报名该竞赛
          - 个人赛：用户必须已报名该竞赛
          - 竞赛状态必须为 active 或 ending

          所有身份验证均在服务端完成，不信任任何请求参数中的用户/队伍 ID。
          """)
  @GetMapping("/{competitionId}/leaderboard")
  public Result<CompetitionLeaderboardResponse> getCompetitionLeaderboard(
      @Parameter(description = "竞赛 ID") @PathVariable Long competitionId) {
    return Result.success(competitionService.getCompetitionLeaderboard(competitionId));
  }

  @Operation(summary = "提交竞赛 Flag",
      description = """
          提交竞赛题目的 Flag。

          安全要求：
          - 用户必须已登录（JWT Token）
          - 团队赛：用户所属队伍必须已报名该竞赛，得分记入队伍
          - 个人赛：用户必须已报名该竞赛
          - 竞赛状态为 active 时才计分，ending 状态可提交但不计分

          请求参数信任策略：
          - competitionId, challengeId, flag: 资源标识符和用户输入，可接受
          - userId, teamId: 绝不从请求接受，从 JWT Token 中提取

          所有身份验证和参赛权限验证均在服务端完成。
          """)
  @PostMapping("/submit")
  @RateLimit(window = 60, maxRequests = 10, message = "提交过于频繁，请稍后再试")
  public Result<CompetitionSubmitFlagResponse> submitCompetitionFlag(
      @Valid @RequestBody CompetitionSubmitFlagRequest request) {
    return Result.success(competitionService.submitCompetitionFlag(request));
  }
}
