package cn.edu.ndky.nkctf.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Docker Host 解析器
 *
 * <p>负责解析和检测可用的 Docker daemon 端点。</p>
 *
 * <h3>检测优先级</h3>
 * <ol>
 *   <li>环境变量 DOCKER_HOST</li>
 *   <li>配置文件显式指定的 host</li>
 *   <li>Unix socket: /var/run/docker.sock（如果可访问）</li>
 *   <li>TCP with TLS: tcp://localhost:2376（如果配置了证书）</li>
 *   <li>TCP: tcp://localhost:2375（仅开发环境）</li>
 * </ol>
 *
 * <h3>安全考虑</h3>
 * <ul>
 *   <li>TCP 2375 端口无加密，仅适用于本地开发</li>
 *   <li>生产环境应使用 Unix socket 或 TLS (2376)</li>
 *   <li>TLS 需要正确配置证书</li>
 * </ul>
 */
@Slf4j
@Component
public class DockerHostResolver {

  private static final String ENV_DOCKER_HOST = "DOCKER_HOST";
  private static final String ENV_DOCKER_TLS_VERIFY = "DOCKER_TLS_VERIFY";
  private static final String ENV_DOCKER_CERT_PATH = "DOCKER_CERT_PATH";

  private static final String UNIX_SOCKET_PATH = "/var/run/docker.sock";
  private static final String UNIX_SOCKET_URI = "unix://" + UNIX_SOCKET_PATH;
  private static final String TCP_HOST = "localhost";
  private static final int TCP_PORT = 2375;
  private static final int TCP_TLS_PORT = 2376;
  private static final int CONNECT_TIMEOUT_MS = 3000;

  /**
   * 解析结果，包含 Docker host URI 和 TLS 配置
   */
  public record ResolvedDockerHost(
      String host,
      boolean tlsEnabled,
      boolean tlsVerify,
      String certPath,
      String source
  ) {
    public URI toUri() {
      return URI.create(host);
    }

    public boolean isTcp() {
      return host.startsWith("tcp://");
    }

    public boolean isUnixSocket() {
      return host.startsWith("unix://");
    }
  }

  /**
   * 解析 Docker host 配置
   *
   * @param properties Docker 配置属性
   * @return 解析后的 Docker host 信息
   * @throws DockerHostResolutionException 如果无法找到可用的 Docker 端点
   */
  public ResolvedDockerHost resolve(DockerProperties properties) {
    // 1. 优先检查环境变量
    ResolvedDockerHost envHost = resolveFromEnvironment();
    if (envHost != null) {
      log.info("使用环境变量 DOCKER_HOST: {}", envHost.host());
      return envHost;
    }

    // 2. 检查配置文件显式指定的 host
    if (StringUtils.hasText(properties.getHost())) {
      log.info("使用配置文件指定的 Docker host: {}", properties.getHost());
      return new ResolvedDockerHost(
          properties.getHost(),
          properties.getTls().isEnabled(),
          properties.getTls().isVerify(),
          properties.getTls().getCertPath(),
          "configuration"
      );
    }

    // 3. 自动检测（如果启用）
    if (properties.isAutoDetect()) {
      ResolvedDockerHost detected = autoDetect(properties);
      if (detected != null) {
        return detected;
      }
    }

    throw new DockerHostResolutionException(
        "无法找到可用的 Docker daemon。请检查：\n" +
            "  1. Docker daemon 是否正在运行\n" +
            "  2. 当前用户是否有权限访问 Docker socket\n" +
            "  3. 是否正确配置了 DOCKER_HOST 环境变量或 docker.host 属性"
    );
  }

  /**
   * 从环境变量解析 Docker host
   */
  private ResolvedDockerHost resolveFromEnvironment() {
    String dockerHost = System.getenv(ENV_DOCKER_HOST);
    if (!StringUtils.hasText(dockerHost)) {
      return null;
    }

    String tlsVerifyEnv = System.getenv(ENV_DOCKER_TLS_VERIFY);
    boolean tlsVerify = "1".equals(tlsVerifyEnv) || "true".equalsIgnoreCase(tlsVerifyEnv);

    String certPath = System.getenv(ENV_DOCKER_CERT_PATH);
    boolean tlsEnabled = tlsVerify || StringUtils.hasText(certPath);

    return new ResolvedDockerHost(
        dockerHost,
        tlsEnabled,
        tlsVerify,
        certPath != null ? certPath : "",
        "environment"
    );
  }

  /**
   * 自动检测可用的 Docker 端点
   */
  private ResolvedDockerHost autoDetect(DockerProperties properties) {
    log.info("开始自动检测 Docker 端点...");

    // 3.1 检查 Unix socket
    if (isUnixSocketAccessible()) {
      log.info("检测到可用的 Unix socket: {}", UNIX_SOCKET_PATH);
      return new ResolvedDockerHost(
          UNIX_SOCKET_URI,
          false,
          false,
          "",
          "auto-detect:unix-socket"
      );
    }

    // 3.2 检查 TCP with TLS (2376)
    if (properties.getTls().isEnabled() && StringUtils.hasText(properties.getTls().getCertPath())) {
      if (isTcpPortReachable(TCP_HOST, TCP_TLS_PORT)) {
        log.info("检测到可用的 TCP TLS 端点: tcp://{}:{}", TCP_HOST, TCP_TLS_PORT);
        return new ResolvedDockerHost(
            "tcp://" + TCP_HOST + ":" + TCP_TLS_PORT,
            true,
            properties.getTls().isVerify(),
            properties.getTls().getCertPath(),
            "auto-detect:tcp-tls"
        );
      }
    }

    // 3.3 检查 TCP (2375) - 仅开发环境
    if (isTcpPortReachable(TCP_HOST, TCP_PORT)) {
      log.warn("检测到 TCP 端点 tcp://{}:{}，此端点无 TLS 加密，仅适用于开发环境！", TCP_HOST, TCP_PORT);
      return new ResolvedDockerHost(
          "tcp://" + TCP_HOST + ":" + TCP_PORT,
          false,
          false,
          "",
          "auto-detect:tcp-plain"
      );
    }

    log.warn("自动检测未找到可用的 Docker 端点");
    return null;
  }

  /**
   * 检查 Unix socket 是否可访问
   */
  private boolean isUnixSocketAccessible() {
    Path socketPath = Path.of(UNIX_SOCKET_PATH);
    if (!Files.exists(socketPath)) {
      log.debug("Unix socket 不存在: {}", UNIX_SOCKET_PATH);
      return false;
    }

    // 检查是否可读写（需要 socket 文件的读写权限）
    if (!Files.isReadable(socketPath) || !Files.isWritable(socketPath)) {
      log.debug("Unix socket 权限不足: {}", UNIX_SOCKET_PATH);
      return false;
    }

    log.debug("Unix socket 可访问: {}", UNIX_SOCKET_PATH);
    return true;
  }

  /**
   * 检查 TCP 端口是否可达
   */
  private boolean isTcpPortReachable(String host, int port) {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
      log.debug("TCP 端口可达: {}:{}", host, port);
      return true;
    } catch (IOException e) {
      log.debug("TCP 端口不可达: {}:{} - {}", host, port, e.getMessage());
      return false;
    }
  }

  /**
   * Docker host 解析异常
   */
  public static class DockerHostResolutionException extends RuntimeException {
    public DockerHostResolutionException(String message) {
      super(message);
    }

    public DockerHostResolutionException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
