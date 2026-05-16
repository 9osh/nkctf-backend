package cn.edu.ndky.nkctf.config;

import com.github.dockerjava.api.DockerClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import cn.edu.ndky.nkctf.dto.response.LeaderboardResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.Mockito.mock;

/**
 * Test doubles for infrastructure that is not available in CI / local unit runs.
 */
@TestConfiguration
@Profile("test")
public class TestInfrastructureConfig {

  @Bean
  @Primary
  public DockerClient dockerClient() {
    return mock(DockerClient.class);
  }

  @Bean
  @Primary
  public RedisConnectionFactory redisConnectionFactory() {
    return mock(RedisConnectionFactory.class);
  }

  @Bean
  @Primary
  @SuppressWarnings("unchecked")
  public RedisTemplate<String, LeaderboardResponse> leaderboardRedisTemplate() {
    return mock(RedisTemplate.class);
  }

  @Bean
  @Primary
  public StringRedisTemplate stringRedisTemplate() {
    return mock(StringRedisTemplate.class);
  }
}
