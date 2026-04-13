package com.jcheckpoint.service;

import com.jcheckpoint.model.SaveState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SyncService {

    private final StorageService storageService;

    public SyncService(@Qualifier("sftpStorageService") StorageService storageService) {
        this.storageService = storageService;
    }

    public void compareAndSync(List<SaveState> localSaves, List<SaveState> externalSaves, String localPath, String externalPath) {
        if (localSaves.isEmpty() && externalSaves.isEmpty()) {
            log.info("save not found.");
            return;
        }

        List<SaveState> sortedLocalSaves = localSaves.stream()
                .sorted((save1, save2) -> save1.getFileName().compareToIgnoreCase(save2.getFileName()))
                .toList();

        // creates the external "index"
        Map<String, SaveState> externalMap = externalSaves.stream()
                .collect(Collectors.toMap(
                        SaveState::getFileName,
                        save -> save,
                        (existing, replacement) -> existing.getLastModified().isAfter(replacement.getLastModified()) ? existing : replacement
                ));


        sortedLocalSaves.forEach(localSave -> {
            SaveState remoteSave = externalMap.get(localSave.getFileName());

            if (remoteSave != null) {
                log.info("Comparing {}: PC[{}] vs Trimui[{}]",
                        localSave.getFileName(),
                        localSave.getLastModified(),
                        remoteSave.getLastModified());


                if (localSave.getLastModified().isAfter(remoteSave.getLastModified())) {
                    executeUpload(localSave, Paths.get(remoteSave.getAbsolutePath()), "PC version is newer");

                } else if (remoteSave.getLastModified().isAfter(localSave.getLastModified())) {
                    executeDownload(remoteSave, Paths.get(localSave.getAbsolutePath()), "Trimui version is newer");
                }

            } else {
                Path relativePath = getRelativePath(localPath, localSave.getAbsolutePath());
                Path destination = Paths.get(externalPath).resolve(relativePath);
                executeUpload(localSave, destination, "PC new save found");
            }
        });

        Set<String> localFilesNames = localSaves.stream()
                .map(s -> s.getFileName())
                .collect(Collectors.toSet());

        externalSaves.forEach(remoteSave -> {
            if (!localFilesNames.contains(remoteSave.getFileName())) {

                Path relativePath = getRelativePath(externalPath, remoteSave.getAbsolutePath());

                Path destination = Paths.get(localPath).resolve(relativePath);

                executeDownload(remoteSave, destination, "Trimui new save found");
            }
        });
    }

    private Path getRelativePath(String rootPath, String fullPath) {
        Path root = Paths.get(rootPath);
        Path full = Paths.get(fullPath);
        return root.relativize(full);
    }

    private void executeUpload(SaveState localFile, Path destination, String logMessage) {
        log.info("{}: {} starting upload...", logMessage, localFile.getFileName());
        storageService.uploadFile(Paths.get(localFile.getAbsolutePath()), destination);
    }


    private void executeDownload(SaveState remoteFile, Path destination, String logMessage) {
        log.info("{}: {} starting download...", logMessage, remoteFile.getFileName());
        storageService.downloadFile(Paths.get(remoteFile.getAbsolutePath()), destination);
    }
}


