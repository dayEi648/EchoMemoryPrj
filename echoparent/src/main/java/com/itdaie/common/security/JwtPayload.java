package com.itdaie.common.security;

/**
 * JWT解析后的负载数据。
 */
public record JwtPayload(Integer userId, String username, Integer role) {
}
