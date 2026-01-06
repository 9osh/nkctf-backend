package cn.edu.ndky.nkctf.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Refresh Token Cookie 配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "cookie.refresh-token")
public class CookieProperties {

    /**
     * Cookie 名称
     */
    private String name = "nkctf_refresh_token";

    /**
     * Cookie 路径 (仅发送到认证端点)
     */
    private String path = "/api/auth";

    /**
     * Cookie 最大存活时间 (秒)
     */
    private int maxAge = 604800; // 7 days

    /**
     * 是否 HttpOnly (JS 不可访问)
     */
    private boolean httpOnly = true;

    /**
     * 是否仅 HTTPS
     */
    private boolean secure = true;

    /**
     * SameSite 策略 (Strict, Lax, None)
     */
    private String sameSite = "Lax";

    /**
     * Cookie 域名 (留空则为当前域名)
     */
    private String domain = "";
}
