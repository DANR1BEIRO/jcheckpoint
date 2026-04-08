package com.jcheckpoint;

import com.jcheckpoint.service.SyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class JcheckpointApplication implements CommandLineRunner {

    private final SyncService syncService;

    public static void main(String[] args) {
        SpringApplication.run(JcheckpointApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting test");

        try {
            log.info("Test Successful!");
        } catch (Exception e) {
            log.error("SOmething wrong happen: {}", e.getMessage());
        }
    }
}
