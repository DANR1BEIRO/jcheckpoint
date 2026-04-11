package com.jcheckpoint;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class JcheckpointApplication {
    static void main(String[] args) {
        SpringApplication.run(JcheckpointApplication.class, args);
    }
}
