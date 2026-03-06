package com.festiva;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FestivaApplication {

    public static void main(String[] args) {
        SpringApplication.run(FestivaApplication.class, args);
    }
}
