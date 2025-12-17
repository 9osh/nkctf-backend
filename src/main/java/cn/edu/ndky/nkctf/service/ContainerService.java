package cn.edu.ndky.nkctf.service;

import cn.edu.ndky.nkctf.dto.request.StartContainerRequest;
import cn.edu.ndky.nkctf.dto.response.ContainerInfoResponse;

import java.util.List;

/**
 * 容器管理服务接口
 */
public interface ContainerService {

  /**
   * 启动容器
   *
   * @param request 包含 challengeId 和可选的 competitionId
   * @return 容器信息（含访问地址和 Flag）
   */
  ContainerInfoResponse startContainer(StartContainerRequest request);

  /**
   * 销毁容器
   *
   * @param containerId 容器ID
   */
  void stopContainer(String containerId);

  /**
   * 延长容器生命周期
   *
   * @param containerId 容器ID
   * @return 更新后的容器信息
   */
  ContainerInfoResponse extendContainer(String containerId);

  /**
   * 获取当前用户的容器列表
   *
   * @return 容器列表
   */
  List<ContainerInfoResponse> listContainers();
}
