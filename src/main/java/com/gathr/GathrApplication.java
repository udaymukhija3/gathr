package com.gathr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GathrApplication {
    public static void main(String[] args) {
        SpringApplication.run(GathrApplication.class, args);
    }
}

