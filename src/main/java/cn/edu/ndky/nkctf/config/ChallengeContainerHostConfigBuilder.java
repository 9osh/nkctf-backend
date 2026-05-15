package cn.edu.ndky.nkctf.config;

import com.github.dockerjava.api.model.Capability;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.RestartPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 构建 CTF 挑战容器的 {@link HostConfig}：禁止 {@code publishAllPorts}，仅绑定配置的单个容器端口。
 */
@Component
@RequiredArgsConstructor
public class ChallengeContainerHostConfigBuilder {

  private final DockerProperties dockerProperties;

  /**
   * @param containerPort 题目配置的容器端口，或平台默认端口
   */
  public HostConfig build(int containerPort) {
    DockerProperties.Container container = dockerProperties.getContainer();
    long memoryBytes = parseMemoryLimit(container.getMemoryLimit());
    long cpuQuota = (long) (container.getCpuLimit() * 100_000L);

    HostConfig hostConfig =
        HostConfig.newHostConfig()
            .withMemory(memoryBytes)
            .withMemorySwap(memoryBytes)
            .withCpuQuota(cpuQuota)
            .withCpuPeriod(100_000L)
            .withPidsLimit(container.getPidsLimit())
            .withNetworkMode(container.getNetwork())
            .withPrivileged(false)
            .withPublishAllPorts(false)
            .withRestartPolicy(RestartPolicy.noRestart());

    if (container.isNoNewPrivileges()) {
      hostConfig.withSecurityOpts(List.of("no-new-privileges:true"));
    }
    if (container.isDropAllCapabilities()) {
      hostConfig.withCapDrop(Capability.ALL);
    }
    if (container.isReadonlyRootfs()) {
      hostConfig.withReadonlyRootfs(true);
    }

    if (!container.isPublishAllPorts()) {
      hostConfig.withPortBindings(buildPortBindings(container, containerPort));
    } else {
      hostConfig.withPublishAllPorts(true);
    }

    return hostConfig;
  }

  /**
   * 题目端口优先，否则使用 {@code docker.container.container-port} 全局默认值。
   */
  public int resolveContainerPort(Integer challengeDockerPort) {
    if (challengeDockerPort != null && isValidPort(challengeDockerPort)) {
      return challengeDockerPort;
    }
    return getDefaultContainerPort();
  }

  public int getDefaultContainerPort() {
    return dockerProperties.getContainer().getContainerPort();
  }

  public static boolean isValidPort(int port) {
    return port >= 1 && port <= 65535;
  }

  private Ports buildPortBindings(DockerProperties.Container container, int containerPort) {
    String bindHost = StringUtils.hasText(container.getBindHost())
        ? container.getBindHost()
        : "127.0.0.1";

    Ports ports = new Ports();
    ports.bind(
        ExposedPort.tcp(containerPort),
        Ports.Binding.bindIpAndPort(bindHost, 0));
    return ports;
  }

  static long parseMemoryLimit(String limit) {
    String normalized = limit.toLowerCase().trim();
    if (normalized.endsWith("g")) {
      return Long.parseLong(normalized.substring(0, normalized.length() - 1)) * 1024L * 1024L * 1024L;
    }
    if (normalized.endsWith("m")) {
      return Long.parseLong(normalized.substring(0, normalized.length() - 1)) * 1024L * 1024L;
    }
    if (normalized.endsWith("k")) {
      return Long.parseLong(normalized.substring(0, normalized.length() - 1)) * 1024L;
    }
    return Long.parseLong(normalized);
  }
}
