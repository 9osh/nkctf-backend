package cn.edu.ndky.nkctf.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ports;
import org.junit.jupiter.api.Test;

class ChallengeContainerHostConfigBuilderTest {

  @Test
  void build_disablesPublishAllPortsAndBindsSinglePort() {
    DockerProperties properties = new DockerProperties();
    properties.getContainer().setBindHost("127.0.0.1");

    HostConfig hostConfig = new ChallengeContainerHostConfigBuilder(properties).build(8080);

    assertFalse(Boolean.TRUE.equals(hostConfig.getPublishAllPorts()));
    Ports bindings = hostConfig.getPortBindings();
    assertNotNull(bindings);
  }

  @Test
  void resolveContainerPort_prefersChallengePort() {
    DockerProperties properties = new DockerProperties();
    properties.getContainer().setContainerPort(80);

    ChallengeContainerHostConfigBuilder builder =
        new ChallengeContainerHostConfigBuilder(properties);

    assertEquals(3000, builder.resolveContainerPort(3000));
    assertEquals(80, builder.resolveContainerPort(null));
  }

  @Test
  void parseMemoryLimit_supportsMegabytes() {
    assertEquals(256L * 1024 * 1024, ChallengeContainerHostConfigBuilder.parseMemoryLimit("256m"));
  }
}
