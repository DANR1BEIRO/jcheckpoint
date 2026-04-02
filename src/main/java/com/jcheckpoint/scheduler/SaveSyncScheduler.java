package com.jcheckpoint.scheduler;

import com.jcheckpoint.model.SaveState;
import com.jcheckpoint.service.SaveService;
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

    private final SaveService saveService;
    private final SyncService syncService;

    @Value("${checkpoint.path.local}")
    private String localPath;

    @Value("${checkpoint.path.external}")
    private String externalPath;

    @Scheduled(fixedDelay = 5000)
    public void runSyncTask() {

        Path localDirectory = Paths.get(localPath);
        Path externalDirectory = Paths.get(externalPath);

        List<SaveState> localSaves = saveService.listAllSaves(localDirectory);
        List<SaveState> externalSaves = saveService.listAllSaves(externalDirectory);

        syncService.compareAndSync(localSaves, externalSaves, localPath, externalPath);
    }
}
