package cn.edu.ndky.nkctf.controller;

import cn.edu.ndky.nkctf.annotation.RateLimit;
import cn.edu.ndky.nkctf.dto.Result;
import cn.edu.ndky.nkctf.dto.request.ChallengeQueryRequest;
import cn.edu.ndky.nkctf.dto.request.SubmitFlagRequest;
import cn.edu.ndky.nkctf.dto.request.UnlockHintRequest;
import cn.edu.ndky.nkctf.dto.response.ChallengeDetailResponse;
import cn.edu.ndky.nkctf.dto.response.ChallengeListItemResponse;
import cn.edu.ndky.nkctf.dto.response.PageResponse;
import cn.edu.ndky.nkctf.dto.response.SubmitFlagResponse;
import cn.edu.ndky.nkctf.dto.response.UnlockHintResponse;
import cn.edu.ndky.nkctf.service.ChallengeService;
import cn.edu.ndky.nkctf.service.HintService;
import cn.edu.ndky.nkctf.service.SubmissionService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 挑战控制器
 */
@Tag(name = "挑战", description = "挑战相关接口")
@RestController
@RequestMapping("/challenges")
@RequiredArgsConstructor
public class ChallengeController {

  private final ChallengeService challengeService;
  private final SubmissionService submissionService;
  private final HintService hintService;

  @Operation(summary = "获取挑战列表", description = "分页获取所有可见挑战的基本信息，支持过滤和排序")
  @GetMapping
  public Result<PageResponse<ChallengeListItemResponse>> getChallengeList(
      @Parameter(description = "分类过滤 (web, pwn, crypto, reverse, misc, blockchain)")
      @RequestParam(required = false) String category,
      @Parameter(description = "难度过滤 (EASY, MEDIUM, HARD)")
      @RequestParam(required = false) String difficulty,
      @Parameter(description = "解决状态过滤 (true: 已解决, false: 未解决)")
      @RequestParam(required = false) Boolean solved,
      @Parameter(description = "排序方式 (newest, most_solves, least_solves, highest_points)")
      @RequestParam(required = false, defaultValue = "newest") String sortBy,
      @Parameter(description = "页码（从 1 开始）")
      @RequestParam(required = false, defaultValue = "1") Integer page) {
    ChallengeQueryRequest request = new ChallengeQueryRequest();
    request.setCategory(category);
    request.setDifficulty(difficulty);
    request.setSolved(solved);
    request.setSortBy(sortBy);
    request.setPage(page);
    return Result.success(challengeService.getChallengeList(request));
  }

  @Operation(summary = "获取挑战详情", description = "获取挑战的完整详情，包括提示和附件")
  @GetMapping("/{id}")
  public Result<ChallengeDetailResponse> getChallengeDetail(
      @Parameter(description = "挑战 ID") @PathVariable Long id) {
    return Result.success(challengeService.getChallengeDetail(id));
  }

  @Operation(summary = "提交 Flag", description = "提交 Flag 进行验证，正确则获得积分")
  @RateLimit(window = 60, maxRequests = 3, message = "提交过于频繁，请稍后再试")
  @PostMapping("/submit")
  public Result<SubmitFlagResponse> submitFlag(@Valid @RequestBody SubmitFlagRequest request) {
    return Result.success(submissionService.submitFlag(request));
  }

  @Operation(summary = "解锁提示", description = "花费积分解锁题目提示")
  @RateLimit(window = 60, maxRequests = 3, message = "请求过于频繁，请稍后再试")
  @PostMapping("/hints/unlock")
  public Result<UnlockHintResponse> unlockHint(@Valid @RequestBody UnlockHintRequest request) {
    return Result.success(hintService.unlockHint(request));
  }
}
