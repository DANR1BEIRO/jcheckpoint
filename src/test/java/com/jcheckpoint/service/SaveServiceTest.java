package com.jcheckpoint.service;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.jcheckpoint.model.SaveState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SaveServiceTest {

    private FileSystem fileSystem;
    private SaveService service;
    private Path path;

    /**
     * Executes before each test method.
     * Ensures every test case starts with a fresh service instance
     * and a clean virtual file system.
     */
    @BeforeEach
    void setup() throws IOException {

        // starts the virtual disk with unix rules
        fileSystem = Jimfs.newFileSystem(Configuration.unix());

        // defines and creates the virtual directory path
        path = fileSystem.getPath("/fake/save");
        Files.createDirectories(path);

        service = new SaveService();
    }

    /**
     * Executes after each test method.
     * Properly closes the virtual file system to release allocated RAM.
     */
    @AfterEach
    void closeVirtualFileSystem() throws IOException {
        if (fileSystem != null) {
            fileSystem.close();
        }
    }

    @Test
    @DisplayName("Should return an empty list when directory does not exist")
    void shouldReturnEmptyListWhenDirectoryDoesNotExist() {

        Path invalidPath = fileSystem.getPath("fake/invalid");

        List<SaveState> result = service.listAllSaves(invalidPath);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should successfully list and map a valid save file")
    void shouldListAndMapValidSaveFile() throws IOException {

        // concatenates path "/fake/save" and "chrono_trigger.srm"
        // "/fake/save/chrono_trigger.srm"
        Path chronoTrigger = path.resolve("chrono_trigger.srm");

        // creates the file into Jimfs RAM disc
        Files.createFile(chronoTrigger);
        Files.writeString(chronoTrigger, "simulate bytes");

        List<SaveState> saveList = service.listAllSaves(path);
        assertThat(saveList).hasSize(1);

        SaveState chronoTriggerSave = saveList.get(0);

        assertThat(chronoTriggerSave.getExtension()).isEqualTo("srm");
        assertThat(chronoTriggerSave.getFileName()).isEqualTo("chrono_trigger.srm");
    }

    @Test
    @DisplayName("Should ignore subfolders and lists only regular files")
    void shouldIgnoreSubfoldersAndListOnlyRegularFiles() throws IOException {

        // creates a valid file and concatenate its address to the valid path
        Files.createFile(path.resolve("chrono_trigger.srm"));
        Files.createDirectory(path.resolve("invalid_directory"));

        List<SaveState> saves = service.listAllSaves(path);

        assertThat(saves.size()).isEqualTo(1);

        assertThat(saves.get(0).getFileName()).isEqualTo("chrono_trigger.srm");
    }

    @Test
    @DisplayName("Should handle extension with multiple dots")
    void shouldHandleExtensionWithMultipleDots() throws IOException {

        Path chronoTrigger = path.resolve("chrono_trigger.tar.srm");
        Files.createFile(chronoTrigger);
        Files.writeString(chronoTrigger, "add some bytes");

        List<SaveState> saves = service.listAllSaves(path);

        assertThat(saves).hasSize(1);
        assertThat(saves.get(0).getFileName()).isEqualTo("chrono_trigger.tar.srm");
        assertThat(saves.get(0).getExtension()).isEqualTo("srm");
    }

    @Test
    @DisplayName("Should map file attributes correctly")
    void shouldMapFileAttributesCorrectly() throws IOException {
        Path chronoTrigger = path.resolve("chrono_trigger.srm");
        String mockText = "add bytes to save file";

        Files.createFile(chronoTrigger);
        Files.writeString(chronoTrigger, mockText);

        List<SaveState> saves = service.listAllSaves(path);

        SaveState chronoTriggerSave = saves.get(0);

        System.out.println("Size in bytes: " + chronoTriggerSave.getSizeInBytes());

        assertThat(saves).hasSize(1);
        assertThat(chronoTriggerSave.getSizeInBytes()).isEqualTo(22L);
        assertThat(chronoTriggerSave.getLastModified()).isNotNull();
    }

    @Test
    @DisplayName("Should replace existing file content when replaceFile is called")
    void shouldReplaceExistingFile() throws IOException {

        Path chronoTriggerNewData = fileSystem.getPath("pc/save/chrono_trigger.srm");
        Files.createDirectories(chronoTriggerNewData.getParent());
        Files.writeString(chronoTriggerNewData, "new data");

        Path chronoTriggerOldData = fileSystem.getPath("/trimui/save/chrono_trigger.srm");
        Files.createDirectories(chronoTriggerOldData.getParent());
        Files.writeString(chronoTriggerOldData, "old data");

        assertThat(chronoTriggerOldData.getFileName()).isEqualTo(chronoTriggerNewData.getFileName());

        service.replaceFile(chronoTriggerNewData, chronoTriggerOldData);

        assertThat(Files.readString(chronoTriggerOldData)).isEqualTo("new data");
    }
}