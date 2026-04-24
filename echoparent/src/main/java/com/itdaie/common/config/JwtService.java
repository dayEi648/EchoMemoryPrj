package com.itdaie.common.config;

import com.itdaie.common.security.JwtPayload;
import com.itdaie.pojo.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * JWT服务。
 * 负责生成和解析JWT令牌。
 */
@Service
public class JwtService {

    @Autowired
    private JwtProperties jwtProperties;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 为用户生成JWT令牌。
     *
     * @param user 用户实体
     * @return JWT字符串
     */
    public String generateToken(User user) {
        return Jwts.builder()
                .claim("userId", user.getId())
                .claim("username", user.getUsername())
                .claim("role", user.getRole())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusMillis(jwtProperties.getExpiration())))
                .signWith(getKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 解析JWT令牌。
     *
     * @param token JWT字符串
     * @return 解析成功返回Optional包含JwtPayload，失败或过期返回Optional.empty()
     */
    public Optional<JwtPayload> parseToken(String token) {
        try {
            var claims = Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Integer userId = claims.get("userId", Integer.class);
            String username = claims.get("username", String.class);
            Integer role = claims.get("role", Integer.class);
            return Optional.of(new JwtPayload(userId, username, role));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
