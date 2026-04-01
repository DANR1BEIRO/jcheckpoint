package com.jcheckpoint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JcheckpointApplication {

    public static void main(String[] args) {
        SpringApplication.run(JcheckpointApplication.class, args);
    }
}
