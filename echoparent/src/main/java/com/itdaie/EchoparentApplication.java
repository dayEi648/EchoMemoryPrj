package com.itdaie;

import com.itdaie.common.config.JwtProperties;
import com.itdaie.common.config.OssConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@MapperScan("com.itdaie.mapper")
@EnableConfigurationProperties({JwtProperties.class, OssConfig.class})
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class EchoparentApplication {

    public static void main(String[] args) {
        SpringApplication.run(EchoparentApplication.class, args);
    }

}
