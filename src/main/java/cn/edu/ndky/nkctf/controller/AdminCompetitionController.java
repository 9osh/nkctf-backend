package cn.edu.ndky.nkctf.controller;

import cn.edu.ndky.nkctf.dto.Result;
import cn.edu.ndky.nkctf.dto.request.*;
import cn.edu.ndky.nkctf.dto.response.*;
import cn.edu.ndky.nkctf.service.AdminCompetitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员竞赛管理控制器
 */
@Tag(name = "管理员-竞赛管理", description = "管理员竞赛管理接口")
@RestController
@RequestMapping("/admin/competitions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCompetitionController {

  private final AdminCompetitionService adminCompetitionService;

  // ========== 竞赛 CRUD ==========

  @Operation(summary = "获取竞赛列表", description = "分页获取所有竞赛，支持状态、关键词过滤")
  @GetMapping
  public Result<PageResponse<AdminCompetitionListItemResponse>> getCompetitionList(
      @Parameter(description = "状态过滤 (inactive, active, ending)")
          @RequestParam(required = false) String status,
      @Parameter(description = "关键词搜索")
          @RequestParam(required = false) String keyword,
      @Parameter(description = "页码")
          @RequestParam(required = false, defaultValue = "1") Integer page) {
    AdminCompetitionQueryRequest request = new AdminCompetitionQueryRequest();
    request.setStatus(status);
    request.setKeyword(keyword);
    request.setPage(page);
    return Result.success(adminCompetitionService.getCompetitionList(request));
  }

  @Operation(summary = "获取竞赛详情", description = "获取竞赛详细信息，包含题目列表")
  @GetMapping("/{id}")
  public Result<AdminCompetitionDetailResponse> getCompetitionDetail(
      @Parameter(description = "竞赛 ID") @PathVariable Long id) {
    return Result.success(adminCompetitionService.getCompetitionDetail(id));
  }

  @Operation(summary = "创建竞赛", description = "创建新竞赛")
  @PostMapping
  public Result<AdminCompetitionDetailResponse> createCompetition(
      @Valid @RequestBody CreateCompetitionRequest request) {
    return Result.success(adminCompetitionService.createCompetition(request));
  }

  @Operation(summary = "更新竞赛", description = "更新竞赛信息")
  @PutMapping("/{id}")
  public Result<AdminCompetitionDetailResponse> updateCompetition(
      @Parameter(description = "竞赛 ID") @PathVariable Long id,
      @Valid @RequestBody UpdateCompetitionRequest request) {
    return Result.success(adminCompetitionService.updateCompetition(id, request));
  }

  @Operation(summary = "删除竞赛", description = "软删除竞赛及其关联数据")
  @DeleteMapping("/{id}")
  public Result<Void> deleteCompetition(
      @Parameter(description = "竞赛 ID") @PathVariable Long id) {
    adminCompetitionService.deleteCompetition(id);
    return Result.success("删除成功", null);
  }

  @Operation(summary = "更改竞赛状态", description = "更改竞赛状态为 inactive/active/ending")
  @PatchMapping("/{id}/status")
  public Result<Void> changeStatus(
      @Parameter(description = "竞赛 ID") @PathVariable Long id,
      @Valid @RequestBody ChangeStatusRequest request) {
    adminCompetitionService.changeStatus(id, request);
    return Result.success("状态更改成功", null);
  }

  // ========== 题目管理 ==========

  @Operation(summary = "获取竞赛题目列表", description = "获取竞赛中的所有题目")
  @GetMapping("/{id}/challenges")
  public Result<List<AdminCompetitionChallengeResponse>> getChallenges(
      @Parameter(description = "竞赛 ID") @PathVariable Long id) {
    return Result.success(adminCompetitionService.getChallenges(id));
  }

  @Operation(summary = "添加题目到竞赛", description = "批量添加题目到竞赛")
  @PostMapping("/{id}/challenges")
  public Result<List<AdminCompetitionChallengeResponse>> addChallenges(
      @Parameter(description = "竞赛 ID") @PathVariable Long id,
      @Valid @RequestBody AddChallengesRequest request) {
    return Result.success(adminCompetitionService.addChallenges(id, request));
  }

  @Operation(summary = "从竞赛移除题目", description = "从竞赛中移除指定题目")
  @DeleteMapping("/{competitionId}/challenges/{challengeId}")
  public Result<Void> removeChallenge(
      @Parameter(description = "竞赛 ID") @PathVariable Long competitionId,
      @Parameter(description = "题目 ID") @PathVariable Long challengeId) {
    adminCompetitionService.removeChallenge(competitionId, challengeId);
    return Result.success("题目移除成功", null);
  }

  @Operation(summary = "重排序竞赛题目", description = "重新排列竞赛中题目的顺序")
  @PutMapping("/{id}/challenges/reorder")
  public Result<List<AdminCompetitionChallengeResponse>> reorderChallenges(
      @Parameter(description = "竞赛 ID") @PathVariable Long id,
      @Valid @RequestBody ReorderChallengesRequest request) {
    return Result.success(adminCompetitionService.reorderChallenges(id, request));
  }

  // ========== 参赛者管理 ==========

  @Operation(summary = "获取参赛者列表", description = "分页获取竞赛参赛者（个人或团队）")
  @GetMapping("/{id}/participants")
  public Result<PageResponse<AdminParticipantResponse>> getParticipants(
      @Parameter(description = "竞赛 ID") @PathVariable Long id,
      @Parameter(description = "关键词搜索") @RequestParam(required = false) String keyword,
      @Parameter(description = "页码") @RequestParam(required = false, defaultValue = "1") Integer page) {
    AdminParticipantQueryRequest request = new AdminParticipantQueryRequest();
    request.setKeyword(keyword);
    request.setPage(page);
    return Result.success(adminCompetitionService.getParticipants(id, request));
  }
}
