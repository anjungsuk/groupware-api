package com.company.groupware;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = "com.company.groupware")
@EntityScan(basePackages = "com.company.groupware")
@EnableJpaRepositories(basePackages = "com.company.groupware")
@EnableAsync
public class GroupwareApplication {

    public static void main(String[] args) {
        SpringApplication.run(GroupwareApplication.class, args);
    }
}
