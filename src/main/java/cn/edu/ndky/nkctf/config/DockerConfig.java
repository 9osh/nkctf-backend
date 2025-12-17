package cn.edu.ndky.nkctf.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateNetworkResponse;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.net.URI;
import java.time.Duration;
import java.util.List;

/**
 * Docker 客户端配置
 */
@Slf4j
@Configuration
public class DockerConfig {

  @Value("${docker.host}")
  private String dockerHost;

  @Value("${docker.container.network:nkctf-challenge-network}")
  private String networkName;

  private DockerClient dockerClient;

  @Bean
  public DockerClient dockerClient() {
    log.info("初始化 Docker 客户端, dockerHost={}", dockerHost);

    URI dockerHostUri = URI.create(dockerHost);
    log.info("解析后的 Docker URI: {}", dockerHostUri);

    DockerClientConfig config = new DefaultDockerClientConfig.Builder()
        .withDockerHost(dockerHost)
        .build();

    log.info("DockerClientConfig 解析的 dockerHost: {}", config.getDockerHost());

    DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
        .dockerHost(dockerHostUri)
        .maxConnections(100)
        .connectionTimeout(Duration.ofSeconds(30))
        .responseTimeout(Duration.ofSeconds(45))
        .build();

    dockerClient = DockerClientImpl.getInstance(config, httpClient);
    log.info("Docker 客户端初始化完成");
    return dockerClient;
  }

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
}
