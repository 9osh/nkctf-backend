package cn.edu.ndky.nkctf.controller;

import cn.edu.ndky.nkctf.dto.Result;
import cn.edu.ndky.nkctf.dto.request.AdminUserQueryRequest;
import cn.edu.ndky.nkctf.dto.request.ChangeRoleRequest;
import cn.edu.ndky.nkctf.dto.request.ToggleEnabledRequest;
import cn.edu.ndky.nkctf.dto.request.UpdateUserByAdminRequest;
import cn.edu.ndky.nkctf.dto.response.AdminUserDetailResponse;
import cn.edu.ndky.nkctf.dto.response.AdminUserListItemResponse;
import cn.edu.ndky.nkctf.dto.response.PageResponse;
import cn.edu.ndky.nkctf.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员用户管理控制器
 */
@Tag(name = "管理员-用户管理", description = "管理员用户管理接口")
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

  private final AdminUserService adminUserService;

  @Operation(summary = "获取用户列表", description = "分页获取所有用户，支持角色、状态、关键词过滤")
  @GetMapping
  public Result<PageResponse<AdminUserListItemResponse>> getUserList(
      @Parameter(description = "角色过滤 (USER, ADMIN)") @RequestParam(required = false) String role,
      @Parameter(description = "启用状态过滤") @RequestParam(required = false) Boolean enabled,
      @Parameter(description = "关键词搜索（用户名或邮箱）") @RequestParam(required = false) String keyword,
      @Parameter(description = "页码") @RequestParam(required = false, defaultValue = "1") Integer page) {
    AdminUserQueryRequest request = new AdminUserQueryRequest();
    request.setRole(role);
    request.setEnabled(enabled);
    request.setKeyword(keyword);
    request.setPage(page);
    return Result.success(adminUserService.getUserList(request));
  }

  @Operation(summary = "获取用户详情", description = "获取指定用户的详细信息")
  @GetMapping("/{id}")
  public Result<AdminUserDetailResponse> getUserDetail(
      @Parameter(description = "用户 ID") @PathVariable Long id) {
    return Result.success(adminUserService.getUserDetail(id));
  }

  @Operation(summary = "更新用户信息", description = "更新用户的昵称、签名、角色、状态等")
  @PutMapping("/{id}")
  public Result<AdminUserDetailResponse> updateUser(
      @Parameter(description = "用户 ID") @PathVariable Long id,
      @Valid @RequestBody UpdateUserByAdminRequest request) {
    return Result.success(adminUserService.updateUser(id, request));
  }

  @Operation(summary = "删除用户", description = "软删除指定用户")
  @DeleteMapping("/{id}")
  public Result<Void> deleteUser(
      @Parameter(description = "用户 ID") @PathVariable Long id) {
    adminUserService.deleteUser(id);
    return Result.success("删除成功", null);
  }

  @Operation(summary = "切换用户启用状态", description = "启用或禁用指定用户")
  @PatchMapping("/{id}/enabled")
  public Result<Void> toggleEnabled(
      @Parameter(description = "用户 ID") @PathVariable Long id,
      @Valid @RequestBody ToggleEnabledRequest request) {
    adminUserService.toggleEnabled(id, request);
    String message = request.getEnabled() ? "用户已启用" : "用户已禁用";
    return Result.success(message, null);
  }

  @Operation(summary = "更改用户角色", description = "更改用户角色为 USER 或 ADMIN")
  @PatchMapping("/{id}/role")
  public Result<Void> changeRole(
      @Parameter(description = "用户 ID") @PathVariable Long id,
      @Valid @RequestBody ChangeRoleRequest request) {
    adminUserService.changeRole(id, request);
    return Result.success("角色更改成功", null);
  }
}
