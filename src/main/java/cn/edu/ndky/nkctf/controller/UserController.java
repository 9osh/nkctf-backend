package cn.edu.ndky.nkctf.controller;

import cn.edu.ndky.nkctf.dto.Result;
import cn.edu.ndky.nkctf.dto.request.UpdateBioRequest;
import cn.edu.ndky.nkctf.dto.response.UserProfileResponse;
import cn.edu.ndky.nkctf.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户控制器
 *
 * <h2>安全设计</h2>
 * <p>
 * 本控制器提供用户资料的查询和更新功能。
 * </p>
 *
 * <h3>数据一致性保证</h3>
 * <ul>
 *   <li>GET /profile 和 GET /profile/{userId} 使用相同的数据构建逻辑</li>
 *   <li>当用户 ID 为 2 时，/profile 和 /profile/2 返回完全相同的数据</li>
 *   <li>两个接口都调用同一个 buildUserProfile() 方法，确保返回结构一致</li>
 * </ul>
 *
 * <h3>身份验证</h3>
 * <ul>
 *   <li>GET /profile: 从 JWT Token 中提取当前用户身份，无法伪造</li>
 *   <li>GET /profile/{userId}: 任何已登录用户可查看其他用户的公开资料</li>
 *   <li>PUT /bio: 从 JWT Token 中提取当前用户身份，只能修改自己的签名</li>
 * </ul>
 *
 * <h3>防篡改设计</h3>
 * <p>
 * 所有涉及"当前用户"的操作（如更新签名）都从 JWT Token 中提取用户身份，
 * 黑客无法通过修改请求参数来冒充其他用户执行操作。
 * </p>
 */
@Tag(name = "用户", description = "用户相关接口")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @Operation(summary = "获取当前用户资料",
      description = """
          获取当前登录用户的详细资料。

          安全说明：
          - 用户身份从 JWT Token 中提取，无法通过修改请求参数伪造
          - 返回的数据与 GET /profile/{userId} 完全一致（当 userId 为当前用户 ID 时）

          验证流程：
          1. 从 JWT Token 中提取用户名
          2. 从数据库查询用户 ID
          3. 使用该 ID 查询并构建用户资料
          """)
  @GetMapping("/profile")
  public Result<UserProfileResponse> getCurrentUserProfile() {
    return Result.success(userService.getCurrentUserProfile());
  }

  @Operation(summary = "获取指定用户资料",
      description = """
          获取指定用户的公开资料。

          说明：
          - 此接口用于查看其他用户的公开资料（如排行榜点击用户名查看详情）
          - 返回的数据结构与 GET /profile 完全一致
          - userId 参数是资源标识符，用于指定要查询的用户

          安全说明：
          - 此接口仅提供只读查询功能，不涉及任何修改操作
          - 所有返回的数据都是公开信息，不包含敏感数据
          - URL 中的 userId 不代表操作者身份，仅代表被查询的用户
          """)
  @GetMapping("/profile/{userId}")
  public Result<UserProfileResponse> getUserProfile(
      @Parameter(description = "用户 ID") @PathVariable Long userId) {
    return Result.success(userService.getUserProfile(userId));
  }

  @Operation(summary = "更新个性签名",
      description = """
          更新当前用户的个性签名。

          安全说明：
          - 用户身份从 JWT Token 中提取，只能修改自己的签名
          - 黑客无法通过修改请求参数来修改其他用户的签名
          - 请求体中不接受 userId 参数，完全依赖 JWT Token 确定身份

          验证流程：
          1. 从 JWT Token 中提取用户名
          2. 从数据库查询用户 ID
          3. 更新该用户的个性签名
          """)
  @PutMapping("/bio")
  public Result<Void> updateBio(@Valid @RequestBody UpdateBioRequest request) {
    userService.updateBio(request.getBio());
    return Result.success("个性签名更新成功", null);
  }
}
