package com.itdaie.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT配置属性。
 * 从配置文件中读取jwt相关配置。
 */
@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;
    private Long expiration = 86400000L;
}
