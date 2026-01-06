package cn.edu.ndky.nkctf.util;

import cn.edu.ndky.nkctf.config.CookieProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

/**
 * Cookie 工具类
 * 用于管理 Refresh Token 的 HttpOnly Cookie
 */
@Component
@RequiredArgsConstructor
public class CookieUtil {

    private final CookieProperties cookieProperties;

    /**
     * 设置 Refresh Token Cookie
     *
     * @param response HTTP 响应
     * @param token    Refresh Token 值
     */
    public void setRefreshTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie
                .from(cookieProperties.getName(), token)
                .path(cookieProperties.getPath())
                .maxAge(cookieProperties.getMaxAge())
                .httpOnly(cookieProperties.isHttpOnly())
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite());

        // 仅当域名非空时设置
        if (cookieProperties.getDomain() != null && !cookieProperties.getDomain().isEmpty()) {
            builder.domain(cookieProperties.getDomain());
        }

        response.addHeader("Set-Cookie", builder.build().toString());
    }

    /**
     * 清除 Refresh Token Cookie (登出时调用)
     *
     * @param response HTTP 响应
     */
    public void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie
                .from(cookieProperties.getName(), "")
                .path(cookieProperties.getPath())
                .maxAge(0) // 立即过期
                .httpOnly(cookieProperties.isHttpOnly())
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite());

        if (cookieProperties.getDomain() != null && !cookieProperties.getDomain().isEmpty()) {
            builder.domain(cookieProperties.getDomain());
        }

        response.addHeader("Set-Cookie", builder.build().toString());
    }

    /**
     * 从请求中获取 Refresh Token
     *
     * @param request HTTP 请求
     * @return Optional 包装的 Token 值
     */
    public Optional<String> getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> cookieProperties.getName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isEmpty())
                .findFirst();
    }
}
