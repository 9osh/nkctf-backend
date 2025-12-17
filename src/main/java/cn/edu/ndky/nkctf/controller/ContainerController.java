package cn.edu.ndky.nkctf.controller;

import cn.edu.ndky.nkctf.annotation.RateLimit;
import cn.edu.ndky.nkctf.dto.Result;
import cn.edu.ndky.nkctf.dto.request.ContainerActionRequest;
import cn.edu.ndky.nkctf.dto.request.StartContainerRequest;
import cn.edu.ndky.nkctf.dto.response.ContainerInfoResponse;
import cn.edu.ndky.nkctf.service.ContainerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 容器管理控制器
 *
 * <h2>安全设计说明</h2>
 * <ul>
 *   <li>用户身份: 从 JWT Token 提取，不接受请求参数中的用户 ID</li>
 *   <li>容器所有权: 服务层验证容器所有者与当前用户匹配</li>
 *   <li>限流保护: 使用 @RateLimit 注解防止 DDoS 攻击</li>
 *   <li>竞赛验证: 服务层验证用户的参赛资格</li>
 * </ul>
 */
@RestController
@RequestMapping("/containers")
@RequiredArgsConstructor
@Tag(name = "容器管理", description = "动态题目容器管理接口")
public class ContainerController {

  private final ContainerService containerService;

  /**
   * 启动容器
   *
   * <p>为指定题目启动一个 Docker 容器，容器将在 1 小时后自动销毁。</p>
   *
   * <h3>限制规则</h3>
   * <ul>
   *   <li>普通练习: 每个用户最多 1 个容器</li>
   *   <li>团队赛: 队伍总共最多 3 个容器</li>
   *   <li>个人赛: 每个用户最多 1 个容器</li>
   * </ul>
   *
   * @param request 启动请求（challengeId 必填，competitionId 可选）
   * @return 容器信息（含访问地址和端口）
   */
  @PostMapping("/start")
  @Operation(summary = "启动容器", description = "为指定题目启动动态容器，返回访问地址")
  @RateLimit(window = 60, maxRequests = 1, message = "启动容器过于频繁，请稍后再试")
  public Result<ContainerInfoResponse> startContainer(
      @Valid @RequestBody StartContainerRequest request) {
    ContainerInfoResponse response = containerService.startContainer(request);
    return Result.success(response);
  }

  /**
   * 销毁容器
   *
   * <p>销毁指定的容器。只有容器的启动者才能销毁该容器（谁启动谁销毁原则）。</p>
   *
   * @param request 容器操作请求（containerId 必填）
   * @return 操作结果
   */
  @PostMapping("/stop")
  @Operation(summary = "销毁容器", description = "销毁指定容器，只有启动者可以销毁")
  @RateLimit(window = 60, maxRequests = 1, message = "操作过于频繁，请稍后再试")
  public Result<Void> stopContainer(@Valid @RequestBody ContainerActionRequest request) {
    containerService.stopContainer(request.getContainerId());
    return Result.success();
  }

  /**
   * 延长容器时间
   *
   * <p>重置容器的生命周期为 1 小时。只有容器的所有者才能延时。</p>
   *
   * @param request 容器操作请求（containerId 必填）
   * @return 更新后的容器信息
   */
  @PostMapping("/extend")
  @Operation(summary = "延长容器时间", description = "重置容器生命周期为 1 小时")
  @RateLimit(window = 60, maxRequests = 1, message = "延时操作过于频繁，请稍后再试")
  public Result<ContainerInfoResponse> extendContainer(
      @Valid @RequestBody ContainerActionRequest request) {
    ContainerInfoResponse response = containerService.extendContainer(request.getContainerId());
    return Result.success(response);
  }

  /**
   * 获取我的容器列表
   *
   * <p>获取当前用户启动的所有运行中的容器列表。</p>
   *
   * @return 容器列表
   */
  @GetMapping
  @Operation(summary = "获取我的容器列表", description = "获取当前用户的所有运行中容器")
  public Result<List<ContainerInfoResponse>> listContainers() {
    List<ContainerInfoResponse> containers = containerService.listContainers();
    return Result.success(containers);
  }
}
