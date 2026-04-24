package com.itdaie.common.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 登录、注册等无需 JWT 的公开端点路径判断（与 {@link com.itdaie.common.config.SecurityConfig} 一致）。
 */
public final class PublicAuthEndpointPaths {

    public static final String LOGIN = "/api/users/login";
    public static final String REGISTER = "/api/users/register";

    private PublicAuthEndpointPaths() {
    }

    /**
     * 去掉末尾 {@code /}（长度大于 1 时），使 {@code /api/users/login/} 与 {@code /api/users/login} 等价。
     */
    public static String normalizePath(String uri) {
        if (uri == null || uri.length() <= 1) {
            return uri == null ? "" : uri;
        }
        return uri.endsWith("/") ? uri.substring(0, uri.length() - 1) : uri;
    }

    /**
     * POST 或 OPTIONS 且路径为登录/注册（规范化后）时视为公开认证端点。
     */
    public static boolean matches(HttpServletRequest request) {
        String method = request.getMethod();
        if (!"POST".equalsIgnoreCase(method) && !"OPTIONS".equalsIgnoreCase(method)) {
            return false;
        }
        String path = normalizePath(request.getRequestURI());
        return LOGIN.equals(path) || REGISTER.equals(path);
    }
}
