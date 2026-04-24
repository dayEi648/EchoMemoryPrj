package com.itdaie.common.config;

import com.itdaie.common.security.PublicAuthEndpointPaths;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * 公开认证端点 {@link RequestMatcher}，独立配置类以避免与 {@link SecurityConfig}、
 * {@link com.itdaie.common.security.JwtAuthenticationFilter} 形成循环依赖。
 */
@Configuration
public class PublicAuthMatcherConfig {

    @Bean
    public RequestMatcher publicAuthEndpointsMatcher() {
        return PublicAuthEndpointPaths::matches;
    }
}
