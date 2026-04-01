package com.jcheckpoint.scheduler;

import com.jcheckpoint.model.SaveState;
import com.jcheckpoint.service.SaveService;
import com.jcheckpoint.service.SyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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

        List<SaveState> localSaves = saveService.listAllSaves(localPath);
        List<SaveState> externalSaves = saveService.listAllSaves(externalPath);

        syncService.compareAndSync(localSaves, externalSaves, localPath, externalPath);
    }
}
