package cn.edu.ndky.nkctf.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Docker 配置属性
 *
 * <p>支持多种 Docker 端点连接方式：</p>
 * <ul>
 *   <li>Unix socket: unix:///var/run/docker.sock</li>
 *   <li>TCP (非 TLS): tcp://localhost:2375</li>
 *   <li>TCP with TLS: tcp://localhost:2376</li>
 * </ul>
 *
 * <h3>连接优先级</h3>
 * <ol>
 *   <li>环境变量 DOCKER_HOST（如果设置）</li>
 *   <li>配置文件中显式指定的 host</li>
 *   <li>自动检测：Unix socket → TCP 2375 → TCP 2376 (TLS)</li>
 * </ol>
 */
@Data
@ConfigurationProperties(prefix = "docker")
public class DockerProperties {

  /**
   * Docker daemon 地址
   * <p>支持格式：</p>
   * <ul>
   *   <li>unix:///var/run/docker.sock</li>
   *   <li>tcp://localhost:2375</li>
   *   <li>tcp://localhost:2376</li>
   * </ul>
   * <p>留空则启用自动检测</p>
   */
  private String host = "";

  /**
   * 是否启用自动检测 Docker 端点
   */
  private boolean autoDetect = true;

  /**
   * 连接超时时间（秒）
   */
  private int connectionTimeout = 30;

  /**
   * 响应超时时间（秒）
   */
  private int responseTimeout = 45;

  /**
   * 最大连接数
   */
  private int maxConnections = 100;

  /**
   * TLS 配置
   */
  private Tls tls = new Tls();

  /**
   * 容器网络名称
   */
  private Container container = new Container();

  /**
   * 镜像仓库配置
   */
  private Registry registry = new Registry();

  /**
   * 资源限制配置
   */
  private Limits limits = new Limits();

  @Data
  public static class Tls {
    /**
     * 是否启用 TLS
     */
    private boolean enabled = false;

    /**
     * 是否验证服务器证书
     */
    private boolean verify = true;

    /**
     * 证书目录路径（包含 ca.pem, cert.pem, key.pem）
     */
    private String certPath = "";
  }

  @Data
  public static class Container {
    /**
     * 容器网络名称
     */
    private String network = "nkctf-challenge-network";

    /**
     * 内存限制
     */
    private String memoryLimit = "256m";

    /**
     * CPU 限制
     */
    private double cpuLimit = 0.5;

    /**
     * 容器最大存活时间（秒）
     */
    private int timeout = 3600;

    /**
     * 返回给用户的访问地址
     */
    private String host = "localhost";
  }

  @Data
  public static class Registry {
    /**
     * 镜像仓库地址
     */
    private String url = "";

    /**
     * 用户名
     */
    private String username = "";

    /**
     * 密码
     */
    private String password = "";
  }

  @Data
  public static class Limits {
    /**
     * 普通用户最大容器数
     */
    private int userMaxContainers = 1;

    /**
     * 团队最大容器数
     */
    private int teamMaxContainers = 3;

    /**
     * 每个容器最大延时次数
     */
    private int maxExtendCount = 3;
  }
}
