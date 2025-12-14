package cn.edu.ndky.nkctf.controller;

import cn.edu.ndky.nkctf.dto.Result;
import cn.edu.ndky.nkctf.dto.request.CreateTeamRequest;
import cn.edu.ndky.nkctf.dto.request.JoinTeamRequest;
import cn.edu.ndky.nkctf.dto.request.TransferCaptainRequest;
import cn.edu.ndky.nkctf.dto.response.InviteTokenResponse;
import cn.edu.ndky.nkctf.dto.response.RefreshTokenResponse;
import cn.edu.ndky.nkctf.dto.response.TeamResponse;
import cn.edu.ndky.nkctf.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 队伍控制器
 *
 * <h2>安全设计</h2>
 * <p>
 * 本控制器所有接口都不接受用户 ID 作为操作者身份参数。
 * 操作者身份通过 JWT Token 从 SecurityContext 获取，确保无法伪造。
 * </p>
 *
 * <h3>队长权限接口</h3>
 * <ul>
 *   <li>获取邀请令牌: 仅队长可查看</li>
 *   <li>刷新邀请令牌: 仅队长可操作</li>
 *   <li>移除成员: 仅队长可操作</li>
 *   <li>转让队长: 仅队长可操作</li>
 *   <li>解散队伍: 仅队长可操作</li>
 * </ul>
 *
 * <h3>安全验证流程</h3>
 * <ol>
 *   <li>从 JWT Token 中提取用户名</li>
 *   <li>从数据库查询用户信息，获取用户 ID 和队伍 ID</li>
 *   <li>从数据库查询队伍信息，获取队长 ID</li>
 *   <li>比对当前用户 ID 与队长 ID，验证权限</li>
 * </ol>
 */
@Tag(name = "队伍", description = "队伍相关接口")
@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
public class TeamController {

  private final TeamService teamService;

  @Operation(summary = "创建队伍", description = "创建一个新队伍，当前用户成为队长")
  @PostMapping
  public Result<TeamResponse> createTeam(@Valid @RequestBody CreateTeamRequest request) {
    return Result.success(teamService.createTeam(request));
  }

  @Operation(summary = "获取队伍详情", description = "根据队伍 ID 获取队伍详情（不包含邀请令牌）")
  @GetMapping("/{teamId}")
  public Result<TeamResponse> getTeam(
      @Parameter(description = "队伍 ID") @PathVariable String teamId) {
    return Result.success(teamService.getTeam(teamId));
  }

  @Operation(summary = "获取我的队伍", description = "获取当前用户所在队伍的详情（不包含邀请令牌）")
  @GetMapping("/my")
  public Result<TeamResponse> getMyTeam() {
    return Result.success(teamService.getMyTeam());
  }

  @Operation(summary = "加入队伍", description = "通过邀请令牌加入队伍")
  @PostMapping("/join")
  public Result<TeamResponse> joinTeam(@Valid @RequestBody JoinTeamRequest request) {
    return Result.success(teamService.joinTeam(request));
  }

  @Operation(summary = "退出队伍", description = "退出当前所在队伍（队长无法退出，只能解散或转让）")
  @PostMapping("/leave")
  public Result<Void> leaveTeam() {
    teamService.leaveTeam();
    return Result.success();
  }

  @Operation(summary = "获取邀请令牌",
      description = """
          获取队伍的邀请令牌。

          安全要求：
          - 用户必须已登录（JWT Token）
          - 用户必须是队伍队长

          验证逻辑：
          - 用户身份从 JWT Token 中提取，无法伪造
          - 服务端比对用户 ID 与数据库中的队长 ID
          """)
  @GetMapping("/invite-token")
  public Result<InviteTokenResponse> getInviteToken() {
    return Result.success(teamService.getInviteToken());
  }

  @Operation(summary = "刷新邀请令牌",
      description = """
          刷新队伍的邀请令牌，旧令牌立即失效。

          安全要求：
          - 用户必须已登录（JWT Token）
          - 用户必须是队伍队长
          """)
  @PostMapping("/refresh-token")
  public Result<RefreshTokenResponse> refreshInviteToken() {
    return Result.success(teamService.refreshInviteToken());
  }

  @Operation(summary = "移除成员",
      description = """
          将指定成员移出队伍。

          安全要求：
          - 用户必须已登录（JWT Token）
          - 用户必须是队伍队长
          - 队长不能移除自己

          安全说明：
          - 操作者身份从 JWT Token 中提取，无法通过修改 URL 参数冒充
          - URL 中的 userId 仅用于标识被移除的成员，不代表操作者身份
          """)
  @DeleteMapping("/members/{userId}")
  public Result<Void> removeMember(
      @Parameter(description = "要移除的成员用户 ID") @PathVariable Long userId) {
    teamService.removeMember(userId);
    return Result.success();
  }

  @Operation(summary = "转让队长",
      description = """
          将队长权限转让给指定成员。

          安全要求：
          - 用户必须已登录（JWT Token）
          - 用户必须是当前队长
          - 新队长必须是队伍成员

          转让后：
          - 原队长变为普通成员
          - 新队长获得队长权限
          """)
  @PostMapping("/transfer-captain")
  public Result<Void> transferCaptain(@Valid @RequestBody TransferCaptainRequest request) {
    teamService.transferCaptain(request);
    return Result.success();
  }

  @Operation(summary = "解散队伍",
      description = """
          解散队伍，所有成员将被移出。

          安全要求：
          - 用户必须已登录（JWT Token）
          - 用户必须是队伍队长
          """)
  @DeleteMapping
  public Result<Void> disbandTeam() {
    teamService.disbandTeam();
    return Result.success();
  }
}
