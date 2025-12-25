package cn.edu.ndky.nkctf.controller;

import cn.edu.ndky.nkctf.dto.Result;
import cn.edu.ndky.nkctf.dto.request.*;
import cn.edu.ndky.nkctf.dto.response.*;
import cn.edu.ndky.nkctf.service.AdminChallengeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 管理员题目管理控制器
 */
@Tag(name = "管理员-题目管理", description = "管理员题目管理接口")
@RestController
@RequestMapping("/admin/challenges")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminChallengeController {

  private final AdminChallengeService adminChallengeService;

  // ========== 题目 CRUD ==========

  @Operation(summary = "获取题目列表", description = "分页获取所有题目，支持分类、难度、状态、关键词过滤")
  @GetMapping
  public Result<PageResponse<AdminChallengeListItemResponse>> getChallengeList(
      @Parameter(description = "分类过滤") @RequestParam(required = false) String category,
      @Parameter(description = "难度过滤") @RequestParam(required = false) String difficulty,
      @Parameter(description = "启用状态过滤") @RequestParam(required = false) Boolean enabled,
      @Parameter(description = "关键词搜索") @RequestParam(required = false) String keyword,
      @Parameter(description = "页码") @RequestParam(required = false, defaultValue = "1") Integer page) {
    AdminChallengeQueryRequest request = new AdminChallengeQueryRequest();
    request.setCategory(category);
    request.setDifficulty(difficulty);
    request.setEnabled(enabled);
    request.setKeyword(keyword);
    request.setPage(page);
    return Result.success(adminChallengeService.getChallengeList(request));
  }

  @Operation(summary = "获取题目详情", description = "获取题目详细信息，包含 Flag 和提示")
  @GetMapping("/{id}")
  public Result<AdminChallengeDetailResponse> getChallengeDetail(
      @Parameter(description = "题目 ID") @PathVariable Long id) {
    return Result.success(adminChallengeService.getChallengeDetail(id));
  }

  @Operation(summary = "创建题目", description = "创建新题目")
  @PostMapping
  public Result<AdminChallengeDetailResponse> createChallenge(
      @Valid @RequestBody CreateChallengeRequest request) {
    return Result.success(adminChallengeService.createChallenge(request));
  }

  @Operation(summary = "更新题目", description = "更新题目信息")
  @PutMapping("/{id}")
  public Result<AdminChallengeDetailResponse> updateChallenge(
      @Parameter(description = "题目 ID") @PathVariable Long id,
      @Valid @RequestBody UpdateChallengeRequest request) {
    return Result.success(adminChallengeService.updateChallenge(id, request));
  }

  @Operation(summary = "删除题目", description = "软删除题目及其提示")
  @DeleteMapping("/{id}")
  public Result<Void> deleteChallenge(
      @Parameter(description = "题目 ID") @PathVariable Long id) {
    adminChallengeService.deleteChallenge(id);
    return Result.success("删除成功", null);
  }

  @Operation(summary = "切换题目启用状态", description = "启用或禁用题目")
  @PatchMapping("/{id}/enabled")
  public Result<Void> toggleEnabled(
      @Parameter(description = "题目 ID") @PathVariable Long id,
      @Valid @RequestBody ToggleEnabledRequest request) {
    adminChallengeService.toggleEnabled(id, request);
    String message = request.getEnabled() ? "题目已启用" : "题目已禁用";
    return Result.success(message, null);
  }

  // ========== 附件管理 ==========

  @Operation(summary = "上传附件", description = "为题目上传附件文件")
  @PostMapping("/{id}/attachment")
  public Result<AttachmentUploadResponse> uploadAttachment(
      @Parameter(description = "题目 ID") @PathVariable Long id,
      @Parameter(description = "附件文件") @RequestParam("file") MultipartFile file) {
    return Result.success(adminChallengeService.uploadAttachment(id, file));
  }

  @Operation(summary = "删除附件", description = "删除题目的附件")
  @DeleteMapping("/{id}/attachment")
  public Result<Void> deleteAttachment(
      @Parameter(description = "题目 ID") @PathVariable Long id) {
    adminChallengeService.deleteAttachment(id);
    return Result.success("附件删除成功", null);
  }

  // ========== 提示管理 ==========

  @Operation(summary = "获取提示列表", description = "获取题目的所有提示")
  @GetMapping("/{id}/hints")
  public Result<List<AdminHintResponse>> getHints(
      @Parameter(description = "题目 ID") @PathVariable Long id) {
    return Result.success(adminChallengeService.getHints(id));
  }

  @Operation(summary = "创建提示", description = "为题目创建新提示")
  @PostMapping("/{id}/hints")
  public Result<AdminHintResponse> createHint(
      @Parameter(description = "题目 ID") @PathVariable Long id,
      @Valid @RequestBody CreateHintRequest request) {
    return Result.success(adminChallengeService.createHint(id, request));
  }

  @Operation(summary = "更新提示", description = "更新题目的提示")
  @PutMapping("/{challengeId}/hints/{hintId}")
  public Result<AdminHintResponse> updateHint(
      @Parameter(description = "题目 ID") @PathVariable Long challengeId,
      @Parameter(description = "提示 ID") @PathVariable Long hintId,
      @Valid @RequestBody UpdateHintRequest request) {
    return Result.success(adminChallengeService.updateHint(challengeId, hintId, request));
  }

  @Operation(summary = "删除提示", description = "删除题目的提示")
  @DeleteMapping("/{challengeId}/hints/{hintId}")
  public Result<Void> deleteHint(
      @Parameter(description = "题目 ID") @PathVariable Long challengeId,
      @Parameter(description = "提示 ID") @PathVariable Long hintId) {
    adminChallengeService.deleteHint(challengeId, hintId);
    return Result.success("提示删除成功", null);
  }

  @Operation(summary = "重排序提示", description = "重新排列提示的顺序")
  @PutMapping("/{id}/hints/reorder")
  public Result<List<AdminHintResponse>> reorderHints(
      @Parameter(description = "题目 ID") @PathVariable Long id,
      @Valid @RequestBody ReorderHintsRequest request) {
    return Result.success(adminChallengeService.reorderHints(id, request));
  }
}
