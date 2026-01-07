package cn.edu.ndky.nkctf.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 配置
 *
 * 使用 STOMP 协议进行消息传递，支持：
 * - 竞赛状态变更广播
 * - 实时排行榜更新（未来扩展）
 *
 * 客户端连接示例：
 * ```javascript
 * const socket = new SockJS('/ws');
 * const stompClient = Stomp.over(socket);
 * stompClient.connect({}, () => {
 *   // 订阅特定竞赛的状态变更
 *   stompClient.subscribe('/topic/competition/1/status', (message) => {
 *     console.log('Status changed:', JSON.parse(message.body));
 *   });
 *
 *   // 订阅全局竞赛状态变更
 *   stompClient.subscribe('/topic/competitions/status', (message) => {
 *     console.log('Competition status changed:', JSON.parse(message.body));
 *   });
 * });
 * ```
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  /**
   * 允许的 WebSocket 来源（与 SecurityConfig 中的 CORS 配置一致）
   */
  private static final String[] ALLOWED_ORIGINS = {
      "http://localhost:3000",     // Nuxt 开发环境
      "http://localhost:5173",     // Vite 开发环境
      "http://127.0.0.1:3000",
      "http://127.0.0.1:5173",
      "https://www.nkctf.cn",      // 生产前端
      "https://nkctf.cn"           // 生产前端备用
  };

  /**
   * 配置消息代理
   *
   * - /topic: 用于广播消息（一对多）
   * - /queue: 用于点对点消息（一对一，未来扩展）
   * - /app: 客户端发送消息的前缀
   */
  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    // 启用简单的内存消息代理
    // 订阅 /topic/* 的客户端可以接收广播消息
    config.enableSimpleBroker("/topic", "/queue");

    // 客户端发送消息到服务器时的前缀
    config.setApplicationDestinationPrefixes("/app");
  }

  /**
   * 注册 STOMP 端点
   *
   * 客户端通过 /ws 端点建立 WebSocket 连接
   * 支持 SockJS 回退（对于不支持 WebSocket 的浏览器）
   */
  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws")
        .setAllowedOrigins(ALLOWED_ORIGINS)
        .withSockJS();  // 启用 SockJS 回退
  }
}
