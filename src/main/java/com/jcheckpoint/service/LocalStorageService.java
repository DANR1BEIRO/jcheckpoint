package com.jcheckpoint.service;

import com.jcheckpoint.exception.SaveSyncException;
import com.jcheckpoint.model.SaveState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    @Value("#{'${app.trimui.save-extensions}'.split(',')}")
    List<String> validExtensions;

    public List<SaveState> listAllSaves(Path path) {

        if (!Files.exists(path) || !Files.isDirectory(path)) {
            throw new SaveSyncException("Directory does not exist or is inaccessible: " + path);
        }

        try (Stream<Path> stream = Files.list(path)) {
            return stream
                    .filter(p -> Files.isRegularFile(p)) // only files, ignore folders
                    .filter(this::isSaveFile)
                    .map(p -> mapToSaveState(p)) // turn Path into SaveState
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new SaveSyncException("Could not read files from directory: " + path, e);
        }
    }

    private boolean isSaveFile(Path file) {
        String save = file.getFileName().toString().toLowerCase();
        return validExtensions.stream().anyMatch(save::endsWith);
    }

    private SaveState mapToSaveState(Path path) {
        try {
            /**
             * readAttributes asks for two parameters:
             * 1. path: The object rerpesenting the files location on the HD.
             * In this case, the `path` comes from the Stream that iterates through the saves folder.
             *
             * 2. "lastModifiedTime,size": A String parameter list containing the exact attributes names we need to,
             * separated by comma.
             */
            Map<String, Object> fileAttributes = Files.readAttributes(path, "lastModifiedTime,size");

            long size = (long) fileAttributes.get("size");

            long millis = ((FileTime) fileAttributes.get("lastModifiedTime")).toMillis();
            LocalDateTime lastModified = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());

            return SaveState.builder()
                    .fileName(path.getFileName().toString())
                    .extension(getFileExtension(path.getFileName().toString()))
                    .sizeInBytes(size)
                    .absolutePath(path.toAbsolutePath().toString())
                    .lastModified(lastModified)
                    .build();
        } catch (IOException e) {
            throw new SaveSyncException("Error extracting metadata from file: " + path.getFileName(), e);
        }
    }

    /**
     * Since SO tipically treat extension as part of the file name rather than
     * a separate attribute, this helper method extract the extension (everything after the last dot)
     * and return as a String.
     *
     * @param fileName
     * @return {@code the file extension}
     */
    private String getFileExtension(String fileName) {
        int lastIndex = fileName.lastIndexOf('.'); // return -1 if the char does not occur
        return (lastIndex == -1) ? "" : fileName.substring(lastIndex + 1);
    }

    @Override
    public void uploadFile(Path local, Path remote) {
        copyFileWithDirectoryCreation(local, remote);
    }

    @Override
    public void downloadFile(Path remote, Path local) {
        copyFileWithDirectoryCreation(remote, local);
    }

    public void copyFileWithDirectoryCreation(Path source, Path target) {
        try {
            if (target.getParent() != null && !Files.exists(target.getParent())) {
                Files.createDirectories(target.getParent());
                log.info("Create missing directory: {}", target.getParent());
            }

            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("success: File copied from {} to {}", source, target);
        } catch (IOException e) {
            log.error("Critical error copying file: {}", source.getFileName());
            throw new SaveSyncException("Failed to copy save file: " + source.getFileName(), e);
        }
    }
}


