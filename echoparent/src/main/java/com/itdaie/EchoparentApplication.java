package com.itdaie;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.itdaie.mapper")
@SpringBootApplication
public class EchoparentApplication {

    public static void main(String[] args) {
        SpringApplication.run(EchoparentApplication.class, args);
    }

}
