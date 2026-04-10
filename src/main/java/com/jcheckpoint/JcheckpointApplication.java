package com.jcheckpoint;

import com.jcheckpoint.model.SaveState;
import com.jcheckpoint.service.StorageService;
import com.jcheckpoint.service.SyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;

@SpringBootApplication
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class JcheckpointApplication implements CommandLineRunner {

    private final StorageService storageService;
    private final SyncService syncService;

    static void main(String[] args) {
        SpringApplication.run(JcheckpointApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting test");

        try {

            List<SaveState> saveStates = storageService.listAllSaves(null);

            log.info("Total saves found: {}", saveStates.size());

            saveStates.forEach(save ->
                    log.info("File detected: {}", save.getFileName(), save.getAbsolutePath()));

        } catch (Exception e) {
            log.error("SOmething wrong happen: {}", e.getMessage());
        }
    }
}
