package com.fileload.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.fileload")
@EntityScan(basePackages = "com.fileload.model.entity")
@EnableJpaRepositories(basePackages = "com.fileload.dao.repository")
public class FileLoadApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileLoadApiApplication.class, args);
    }
}

