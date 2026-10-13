package com.atp;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@EnableScheduling
@MapperScan(basePackages = "com.atp.server.mapper", annotationClass = Mapper.class)
public class AtpApplication {
    /**
     * 启动入口
     * @param args
     */
    public static void main(String[] args) {
        SpringApplication.run(AtpApplication.class, args);
    }
}
