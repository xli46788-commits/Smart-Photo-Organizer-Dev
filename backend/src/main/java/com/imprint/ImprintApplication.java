package com.imprint;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.imprint.mapper")
public class ImprintApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImprintApplication.class, args);
    }
}
