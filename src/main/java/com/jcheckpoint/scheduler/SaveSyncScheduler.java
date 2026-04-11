package com.jcheckpoint.scheduler;

import com.jcheckpoint.model.SaveState;
import com.jcheckpoint.service.LocalStorageService;
import com.jcheckpoint.service.SftpStorageService;
import com.jcheckpoint.service.SyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SaveSyncScheduler {

    private final LocalStorageService localStorageService;
    private final SftpStorageService sftpStorageService;
    private final SyncService syncService;

    @Value("${app.storage.local.path}")
    private String localPath;

    @Value("${app.trimui.save-path}")
    private String externalPath;

    @Scheduled(fixedDelay = 3000)
    public void runSyncTask() {

        Path localDirectory = Paths.get(localPath);
        Path externalDirectory = Paths.get(externalPath);

        List<SaveState> localSaves = localStorageService.listAllSaves(localDirectory);
        List<SaveState> externalSaves = sftpStorageService.listAllSaves(externalDirectory);

        log.info("Local saves found: {}", localSaves.size());
        log.info("External (TrimUI) saves found: {}", externalSaves.size());

        syncService.compareAndSync(localSaves, externalSaves, localPath, externalPath);
    }
}
