package com.zjx.cointool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CointoolApplication {

    public static void main(String[] args) {
        SpringApplication.run(CointoolApplication.class, args);
    }

}
