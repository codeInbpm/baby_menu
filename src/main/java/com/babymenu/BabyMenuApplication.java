package com.babymenu;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.babymenu.mapper")
public class BabyMenuApplication {
    public static void main(String[] args) {
        SpringApplication.run(BabyMenuApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("  宝贝的专属菜单后端启动成功 :)");
        System.out.println("  http://localhost:8080/api");
        System.out.println("========================================\n");
    }
}
