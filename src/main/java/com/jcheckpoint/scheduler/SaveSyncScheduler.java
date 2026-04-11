package com.jcheckpoint.scheduler;

import com.jcheckpoint.model.SaveState;
import com.jcheckpoint.service.StorageService;
import com.jcheckpoint.service.SyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SaveSyncScheduler {

    private final StorageService storageService;
    private final SyncService syncService;

    @Value("${app.storage.local.path}")
    private String localPath;

    @Value("${app.trimui.save-path}")
    private String externalPath;

    @Scheduled(fixedDelay = 30000)
    public void runSyncTask() {

        Path localDirectory = Paths.get(localPath);
        Path externalDirectory = Paths.get(externalPath);

        List<SaveState> localSaves = storageService.listAllSaves(localDirectory);
        List<SaveState> externalSaves = storageService.listAllSaves(externalDirectory);

        syncService.compareAndSync(localSaves, externalSaves, localPath, externalPath);
    }
}
