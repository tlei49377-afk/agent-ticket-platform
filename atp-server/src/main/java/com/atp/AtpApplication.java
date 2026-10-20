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
// 只扫两个模块真正的 Mapper 包：atp-server 和 atp-agent 的 Mapper 都要收进来。
// 不要写成根包 com.atp：扫描范围过大会让 IDEA 把 service 等接口也当成 MyBatis Bean，
// 注入 TicketMessageService 时会误报“存在多个同类型 Bean”。
@MapperScan(basePackages = {"com.atp.server.mapper", "com.atp.agent.mapper"},
        annotationClass = Mapper.class)
public class AtpApplication {
    /**
     * 启动入口
     * @param args
     */
    public static void main(String[] args) {
        SpringApplication.run(AtpApplication.class, args);
    }
}
