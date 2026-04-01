package com.jcheckpoint.service;

import com.jcheckpoint.model.SaveState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class SyncService {

    private final SaveService service;

    public void compareAndSync(List<SaveState> localSave, List<SaveState> externalSave, String localPath, String externalPath) {
        if (localSave.isEmpty() && externalSave.isEmpty()) {
            log.info("save not found.");
            return;
        }

        // creates the external "index"
        Map<String, SaveState> externalMap = externalSave.stream()
                .collect(Collectors.toMap(
                        fileName -> fileName.getFileName(),
                        save -> save));

        localSave.forEach(localFile -> {
            SaveState remoteSave = externalMap.get(localFile.getFileName());

            if (remoteSave != null) {
                if (localFile.getLastModified().isAfter(remoteSave.getLastModified())) {
                    log.info("pc version is newer: {}", localFile.getFileName());
                    service.replaceFile(Paths.get(localFile.getAbsolutePath()), Paths.get(remoteSave.getAbsolutePath()));
                }

                if (remoteSave.getLastModified().isAfter(localFile.getLastModified())) {
                    log.info("Trimui version is newer: {}", remoteSave.getFileName());
                    service.replaceFile(Paths.get(remoteSave.getAbsolutePath()), Paths.get(localFile.getAbsolutePath()));
                }
            } else {
                log.info("New save found on PC: {}. Sending to Trimui", localFile.getFileName());
                Path destination = Paths.get(externalPath, localFile.getFileName());
                service.replaceFile(Paths.get(localFile.getAbsolutePath()), destination);
            }
        });

        Set<String> localFileNames = localSave.stream()
                .map(s -> s.getFileName())
                .collect(Collectors.toSet());

        externalSave.forEach(remoteFile -> {
            if (!localFileNames.contains(remoteFile.getFileName())) {
                log.info("New save found on trimui: {}. Copying to PC", remoteFile.getFileName());
                Path destination = Paths.get(localPath, remoteFile.getFileName());
                service.replaceFile(Paths.get(remoteFile.getAbsolutePath()), destination);
            }
        });
    }
}


