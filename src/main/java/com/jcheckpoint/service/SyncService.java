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

    /**
     * Executes the core bidirectional synchronization logic between the local system and an external device.
     * <p>
     * This method evaluates the {@link SaveState#getLastModified()} timestamps of both local and external files
     * to determine the correct data flow direction. It operates under the following synchronization matrix:
     * <ul>
     * <li><b>Local is newer:</b> Initiates an upload to the external device.</li>
     * <li><b>External is newer:</b> Initiates a download to the local system.</li>
     * <li><b>Timestamps match:</b> No action is taken (already in sync).</li>
     * <li><b>File exists only on Local:</b> Uploads the new save to the external device.</li>
     * <li><b>File exists only on External:</b> Downloads the new save to the local system.</li>
     * </ul>
     *
     * @param localSaves    The list of parsed {@link SaveState} objects representing files on the local machine (PC).
     * @param externalSaves The list of parsed {@link SaveState} objects representing files on the external device (Trimui).
     * @param localPath     The base root directory path for the local saves, used to construct destination paths for downloads.
     * @param externalPath  The base root directory path for the external saves, used to construct destination paths for uploads.
     */
    public void compareAndSync(List<SaveState> localSaves, List<SaveState> externalSaves, String localPath, String externalPath) {
        if (localSaves.isEmpty() && externalSaves.isEmpty()) {
            log.info("save not found.");
            return;
        }

        // creates the external "index"
        Map<String, SaveState> externalMap = externalSaves.stream()
                .collect(Collectors.toMap(
                        SaveState::getFileName,
                        save -> save,
                        (existing, replacement) -> {
                            return existing.getLastModified().isAfter(replacement.getLastModified()) ? existing : replacement;
                        }
                ));


        localSaves.forEach(localSave -> {
            SaveState remoteSave = externalMap.get(localSave.getFileName());

            if (remoteSave != null) {

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

    /**
     * Helper method to encapsulate the upload process, adhering to the DRY principle.
     * <p>
     * It handles the logging of the operation using a dynamic contextual message and delegates
     * the actual file transfer to the {@link StorageService}.
     *
     * @param localFile   The {@link SaveState} object representing the source file on the local machine.
     * @param destination The absolute target {@link Path} on the external device.
     * @param logMessage  A contextual string explaining the reason for the upload (e.g., "pc version is newer"),
     *                    used to generate an accurate and descriptive console log.
     */
    private void executeUpload(SaveState localFile, Path destination, String logMessage) {
        log.info("{}: {} starting upload...", logMessage, localFile.getFileName());
        storageService.uploadFile(Paths.get(localFile.getAbsolutePath()), destination);
    }

    /**
     * Helper method to encapsulate the download process, adhering to the DRY principle.
     * <p>
     * It handles the logging of the operation using a dynamic contextual message and delegates
     * the actual file transfer to the {@link StorageService}.
     *
     * @param remoteFile  The {@link SaveState} object representing the source file on the external device.
     * @param destination The absolute target {@link Path} on the local machine.
     * @param logMessage  A contextual string explaining the reason for the download (e.g., "Trimui version is newer"),
     *                    used to generate an accurate and descriptive console log.
     */
    private void executeDownload(SaveState remoteFile, Path destination, String logMessage) {
        log.info("{}: {} starting download...", logMessage, remoteFile.getFileName());
        storageService.downloadFile(Paths.get(remoteFile.getAbsolutePath()), destination);
    }
}


