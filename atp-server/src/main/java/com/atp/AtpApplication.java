package com.atp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@EnableScheduling
@MapperScan("com.atp.**.mapper")
public class AtpApplication {
    public static void main(String[] args) {
        SpringApplication.run(AtpApplication.class, args);
    }
}
