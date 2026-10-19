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
// 扫整个 com.atp：atp-server 和 atp-agent 两个模块的 Mapper 都要收进来，
// annotationClass 只认带 @Mapper 的接口，不会误收别的
@MapperScan(basePackages = "com.atp", annotationClass = Mapper.class)
public class AtpApplication {
    /**
     * 启动入口
     * @param args
     */
    public static void main(String[] args) {
        SpringApplication.run(AtpApplication.class, args);
    }
}
