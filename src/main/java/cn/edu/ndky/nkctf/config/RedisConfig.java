package cn.edu.ndky.nkctf.config;

import cn.edu.ndky.nkctf.dto.response.LeaderboardResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 序列化配置：不使用 {@code activateDefaultTyping}，由代码指定具体类型。
 *
 * <ul>
 *   <li>字符串、计数、黑名单等：使用 Spring 自带的 {@link org.springframework.data.redis.core.StringRedisTemplate}</li>
 *   <li>排行榜缓存：{@link LeaderboardResponse} 专用 {@link RedisTemplate}</li>
 * </ul>
 */
@Configuration
@Profile("!test")
public class RedisConfig {

  @Bean
  public ObjectMapper redisObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    return mapper;
  }

  @Bean
  public RedisTemplate<String, LeaderboardResponse> leaderboardRedisTemplate(
      RedisConnectionFactory connectionFactory, ObjectMapper redisObjectMapper) {
    RedisTemplate<String, LeaderboardResponse> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);

    StringRedisSerializer stringSerializer = new StringRedisSerializer();
    template.setKeySerializer(stringSerializer);
    template.setHashKeySerializer(stringSerializer);

    Jackson2JsonRedisSerializer<LeaderboardResponse> valueSerializer =
        new Jackson2JsonRedisSerializer<>(redisObjectMapper, LeaderboardResponse.class);
    template.setValueSerializer(valueSerializer);
    template.setHashValueSerializer(valueSerializer);

    template.afterPropertiesSet();
    return template;
  }
}
