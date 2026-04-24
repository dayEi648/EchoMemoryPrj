package com.itdaie.common.security;

import com.itdaie.common.config.JwtService;
import com.itdaie.mapper.UserMapper;
import com.itdaie.pojo.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * JWT认证过滤器。
 * 从请求头中解析JWT并设置Spring Security上下文。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    @Qualifier("publicAuthEndpointsMatcher")
    private RequestMatcher publicAuthEndpointsMatcher;

    @Autowired
    private UserMapper userMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return publicAuthEndpointsMatcher.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            var payloadOpt = jwtService.parseToken(token);
            if (payloadOpt.isPresent()) {
                JwtPayload payload = payloadOpt.get();

                // 校验用户是否有效
                User user = userMapper.selectById(payload.userId());
                if (user == null || Boolean.TRUE.equals(user.getIsDeleted()) || user.getStatus() == null || user.getStatus() != 0) {
                    filterChain.doFilter(request, response);
                    return;
                }

                String authority = mapRoleToAuthority(payload.role());
                List<SimpleGrantedAuthority> authorities =
                        Collections.singletonList(new SimpleGrantedAuthority(authority));

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                payload.username(), null, authorities);
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                request.setAttribute("userId", payload.userId());
                request.setAttribute("username", payload.username());
                request.setAttribute("role", payload.role());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String mapRoleToAuthority(Integer role) {
        return switch (role) {
            case 1 -> "ROLE_VIP";
            case 2 -> "ROLE_ADMIN";
            case 3 -> "ROLE_SUPER_ADMIN";
            default -> "ROLE_USER";
        };
    }
}
