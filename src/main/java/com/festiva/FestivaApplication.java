package com.festiva;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableMongoRepositories(basePackages = "com.festiva.friend.repository")
public class FestivaApplication {

    public static void main(String[] args) {
        SpringApplication.run(FestivaApplication.class, args);
    }
}
