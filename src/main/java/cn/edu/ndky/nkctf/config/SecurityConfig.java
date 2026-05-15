package cn.edu.ndky.nkctf.config;

import cn.edu.ndky.nkctf.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 配置
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${springdoc.swagger-ui.enabled:true}")
    private boolean swaggerUiEnabled;

    private static final String[] PUBLIC_PATHS = {
            "/auth/login",
            "/auth/register",
            "/auth/refresh",
            "/auth/captcha",
            "/attachments/download",
            "/actuator/health",
            "/articles/published/**",
            "/articles/tags",
            "/ws/**"
    };

    private static final String[] SWAGGER_PATHS = {
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    private String[] permitAllPaths() {
        if (!swaggerUiEnabled) {
            return PUBLIC_PATHS;
        }
        var paths = new ArrayList<String>(PUBLIC_PATHS.length + SWAGGER_PATHS.length);
        paths.addAll(Arrays.asList(PUBLIC_PATHS));
        paths.addAll(Arrays.asList(SWAGGER_PATHS));
        return paths.toArray(String[]::new);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF (使用 JWT 无需 CSRF)
                .csrf(AbstractHttpConfigurer::disable)
                // 禁用表单登录
                .formLogin(AbstractHttpConfigurer::disable)
                // 禁用 HTTP Basic
                .httpBasic(AbstractHttpConfigurer::disable)
                // 配置 CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 配置 Session 管理 (无状态)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 配置路径权限
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(permitAllPaths()).permitAll()
                        .anyRequest().authenticated()
                )
                // 添加 JWT 过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 显式指定允许的源 (不能使用通配符 "*" 与 credentials 一起使用)
        // 生产环境应配置为实际域名
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",     // Nuxt 开发环境
                "http://localhost:5173",     // Vite 开发环境
                "http://127.0.0.1:3000",
                "http://127.0.0.1:5173",
                "https://www.nkctf.cn",      // 生产前端
                "https://nkctf.cn"           // 生产前端备用
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        // 允许发送 Cookie (跨域 Cookie 必须设为 true)
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
