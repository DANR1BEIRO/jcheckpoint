package com.jcheckpoint.service;

import com.jcheckpoint.model.SaveState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SaveService {

    @Value("${checkpoint.save-path}") // inject config file properties into savePath
    private String savePath;

    /**
     * List all save files found in configured directory
     * @return  mapped SaveState object list
     */
    public List<SaveState> listAllSaves() {
        Path path = Paths.get(savePath);

        // checks whether directory exists or not
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            throw new RuntimeException("Save directory not found: " + savePath);
        }

        try (Stream<Path> stream = Files.list(path)) {
            return stream
                    .filter(p -> Files.isRegularFile(p)) // only files, ignore folders
                    .map(p -> mapToSaveState(p)) // turn Path into SaveState
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler arquivos de save", e);
        }
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
            throw new RuntimeException("Error mapping file: " + path.getFileName());
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
}
