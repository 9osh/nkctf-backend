package cn.edu.ndky.nkctf.config;

import cn.edu.ndky.nkctf.config.DockerHostResolver.ResolvedDockerHost;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateNetworkResponse;
import com.github.dockerjava.api.command.PingCmd;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * Docker 客户端配置
 *
 * <p>支持多种 Docker daemon 连接方式，具备自动检测和安全回退机制：</p>
 *
 * <h3>支持的端点类型</h3>
 * <ul>
 *   <li><b>Unix socket</b>: unix:///var/run/docker.sock（推荐，最安全）</li>
 *   <li><b>TCP with TLS</b>: tcp://localhost:2376（生产环境推荐）</li>
 *   <li><b>TCP</b>: tcp://localhost:2375（仅开发环境）</li>
 * </ul>
 *
 * <h3>连接优先级</h3>
 * <ol>
 *   <li>环境变量 DOCKER_HOST（如果设置）</li>
 *   <li>配置文件 docker.host（如果配置）</li>
 *   <li>自动检测：Unix socket → TCP TLS → TCP</li>
 * </ol>
 *
 * <h3>安全注意事项</h3>
 * <ul>
 *   <li>TCP 2375 端口无加密，切勿在生产环境使用</li>
 *   <li>生产环境应使用 Unix socket 或配置 TLS</li>
 *   <li>确保 Docker socket 权限正确配置</li>
 * </ul>
 *
 * @see DockerProperties
 * @see DockerHostResolver
 */
@Slf4j
@Configuration
@Profile("!test")
@RequiredArgsConstructor
@EnableConfigurationProperties(DockerProperties.class)
public class DockerConfig {

  private final DockerProperties properties;
  private final DockerHostResolver hostResolver;

  private DockerClient dockerClient;
  private DockerHttpClient httpClient;

  /**
   * 创建 Docker 客户端 Bean
   *
   * @return 配置好的 DockerClient 实例
   * @throws DockerHostResolver.DockerHostResolutionException 如果无法连接 Docker daemon
   */
  @Bean
  public DockerClient dockerClient() {
    log.info("========== Docker 客户端初始化 ==========");

    // 1. 解析 Docker host
    ResolvedDockerHost resolved = hostResolver.resolve(properties);
    logResolvedHost(resolved);

    // 2. 构建配置
    DockerClientConfig config = buildDockerClientConfig(resolved);

    // 3. 创建 HTTP 客户端（传入 config 以获取 TLS 配置）
    httpClient = buildHttpClient(resolved, config);

    // 4. 创建 Docker 客户端
    dockerClient = DockerClientImpl.getInstance(config, httpClient);

    // 5. 验证连接与安全策略
    verifyConnection(resolved);
    enforceConnectionSecurity(resolved);

    log.info("========== Docker 客户端初始化完成 ==========");
    return dockerClient;
  }

  /**
   * 构建 Docker 客户端配置
   */
  private DockerClientConfig buildDockerClientConfig(ResolvedDockerHost resolved) {
    DefaultDockerClientConfig.Builder builder = DefaultDockerClientConfig.createDefaultConfigBuilder()
        .withDockerHost(resolved.host());

    // 配置 TLS
    if (resolved.tlsEnabled()) {
      log.info("启用 TLS 加密连接");
      builder.withDockerTlsVerify(resolved.tlsVerify());

      if (StringUtils.hasText(resolved.certPath())) {
        builder.withDockerCertPath(resolved.certPath());
        log.info("TLS 证书路径: {}", resolved.certPath());
      }
    }

    // 配置镜像仓库认证（如果有）
    if (StringUtils.hasText(properties.getRegistry().getUrl())) {
      builder.withRegistryUrl(properties.getRegistry().getUrl());
      if (StringUtils.hasText(properties.getRegistry().getUsername())) {
        builder.withRegistryUsername(properties.getRegistry().getUsername());
        builder.withRegistryPassword(properties.getRegistry().getPassword());
      }
    }

    return builder.build();
  }

  /**
   * 构建 HTTP 客户端
   *
   * <p>使用 ZerodepDockerHttpClient，内置 Unix socket 支持，无需额外依赖。</p>
   * <p>TLS 配置由 DockerClientConfig 处理，无需在 HTTP 客户端层面单独配置。</p>
   */
  private DockerHttpClient buildHttpClient(ResolvedDockerHost resolved, DockerClientConfig config) {
    ZerodepDockerHttpClient.Builder builder = new ZerodepDockerHttpClient.Builder()
        .dockerHost(config.getDockerHost())
        .maxConnections(properties.getMaxConnections())
        .connectionTimeout(Duration.ofSeconds(properties.getConnectionTimeout()))
        .responseTimeout(Duration.ofSeconds(properties.getResponseTimeout()));

    // 如果启用了 TLS，使用 config 中的 SSL 配置
    if (resolved.tlsEnabled() && config.getSSLConfig() != null) {
      builder.sslConfig(config.getSSLConfig());
    }

    return builder.build();
  }

  /**
   * 生产环境禁止明文 TCP Docker API（2375）
   */
  private void enforceConnectionSecurity(ResolvedDockerHost resolved) {
    if (!properties.getSecurity().isForbidPlainTcp()) {
      return;
    }
    if (resolved.isTcp() && !resolved.tlsEnabled()) {
      throw new DockerHostResolver.DockerHostResolutionException(
          "已启用 docker.security.forbid-plain-tcp：不得使用未加密的 TCP Docker API。"
              + "请使用 unix:///var/run/docker.sock 或配置 TLS（tcp://host:2376）。");
    }
  }

  /**
   * 验证 Docker 连接
   */
  private void verifyConnection(ResolvedDockerHost resolved) {
    try {
      log.info("验证 Docker 连接...");
      PingCmd pingCmd = dockerClient.pingCmd();
      pingCmd.exec();
      log.info("Docker daemon 连接成功！");

      // 获取 Docker 版本信息
      var info = dockerClient.infoCmd().exec();
      log.info("Docker 版本: {}, 操作系统: {}, 架构: {}",
          info.getServerVersion(),
          info.getOsType(),
          info.getArchitecture());

    } catch (Exception e) {
      String errorMsg = String.format(
          "无法连接到 Docker daemon [%s]，来源: %s。错误: %s",
          resolved.host(), resolved.source(), e.getMessage()
      );

      // 提供更详细的错误提示
      if (resolved.isUnixSocket()) {
        errorMsg += "\n\n可能的原因：" +
            "\n  1. Docker daemon 未运行 (尝试: sudo systemctl start docker)" +
            "\n  2. 当前用户不在 docker 组 (尝试: sudo usermod -aG docker $USER)" +
            "\n  3. 需要重新登录以使组权限生效";
      } else if (resolved.isTcp() && !resolved.tlsEnabled()) {
        errorMsg += "\n\n可能的原因：" +
            "\n  1. Docker daemon 未配置 TCP 监听" +
            "\n  2. 防火墙阻止了连接" +
            "\n  3. TCP 监听地址配置错误";
      } else if (resolved.tlsEnabled()) {
        errorMsg += "\n\n可能的原因：" +
            "\n  1. TLS 证书配置错误" +
            "\n  2. 证书已过期或不匹配" +
            "\n  3. Docker daemon 未配置 TLS";
      }

      throw new DockerHostResolver.DockerHostResolutionException(errorMsg, e);
    }
  }

  /**
   * 记录解析后的 Docker host 信息
   */
  private void logResolvedHost(ResolvedDockerHost resolved) {
    log.info("Docker host: {}", resolved.host());
    log.info("连接来源: {}", switch (resolved.source()) {
      case "environment" -> "环境变量 DOCKER_HOST";
      case "configuration" -> "配置文件 docker.host";
      case "auto-detect:unix-socket" -> "自动检测 (Unix socket)";
      case "auto-detect:tcp-tls" -> "自动检测 (TCP with TLS)";
      case "auto-detect:tcp-plain" -> "自动检测 (TCP，警告：无加密)";
      default -> resolved.source();
    });

    if (resolved.tlsEnabled()) {
      log.info("TLS: 已启用, 验证: {}", resolved.tlsVerify() ? "是" : "否");
    } else if (resolved.isTcp()) {
      log.warn("安全警告：TCP 连接未启用 TLS，数据将以明文传输！");
    }
  }

  /**
   * 应用启动后确保网络存在
   */
  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    ensureNetworkExists();
  }

  /**
   * 确保 Docker 网络存在，如果不存在则创建
   */
  private void ensureNetworkExists() {
    if (dockerClient == null) {
      log.warn("DockerClient 未初始化，跳过网络检查");
      return;
    }

    String networkName = properties.getContainer().getNetwork();

    try {
      List<Network> networks = dockerClient.listNetworksCmd()
          .withNameFilter(networkName)
          .exec();

      if (networks.isEmpty()) {
        log.info("创建 Docker 网络: {}", networkName);
        CreateNetworkResponse response = dockerClient.createNetworkCmd()
            .withName(networkName)
            .withDriver("bridge")
            .exec();
        log.info("Docker 网络创建成功: id={}", response.getId());
      } else {
        log.info("Docker 网络已存在: {}", networkName);
      }
    } catch (Exception e) {
      log.error("初始化 Docker 网络失败: {}", e.getMessage(), e);
    }
  }

  /**
   * 应用关闭时清理资源
   */
  @PreDestroy
  public void cleanup() {
    if (httpClient != null) {
      try {
        httpClient.close();
        log.info("Docker HTTP 客户端已关闭");
      } catch (IOException e) {
        log.warn("关闭 Docker HTTP 客户端时发生错误: {}", e.getMessage());
      }
    }

    if (dockerClient != null) {
      try {
        dockerClient.close();
        log.info("Docker 客户端已关闭");
      } catch (IOException e) {
        log.warn("关闭 Docker 客户端时发生错误: {}", e.getMessage());
      }
    }
  }
}
