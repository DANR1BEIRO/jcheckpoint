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
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SaveService {

    @Value("${checkpoint.save-path}") // inject config file properties
    private String savePath;


    /**
     * List all save files found in configured directory
     *
     * @return mapped SaveState object list
     */
    public List<SaveState> listAllSaves() {
        Path path = Paths.get(savePath);

        // checks whether directory exists or not
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            throw new RuntimeException("Save directory not found: " + savePath);
        }

        try (Stream<Path> stream = Files.list(path)) {
            return stream
                    .filter(Files::isRegularFile)          // only files, ignore folders
                    .map(this::mapToSaveState) // turn Path into SaveState
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler arquivos de save", e);
        }
    }

    private SaveState mapToSaveState(Path path) {
        try {
            var fileAttributes = Files.readAttributes(path, "basic:lastModifiedTime,size");
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

    private String getFileExtension(String fileName) {
        int lastIndex = fileName.lastIndexOf('.');
        return (lastIndex == -1) ? "" : fileName.substring(lastIndex + 1);
    }
}
