package com.jcheckpoint.service;

import com.jcheckpoint.exception.SaveSyncException;
import com.jcheckpoint.model.SaveState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class SyncServiceTest {

    @Mock // creates a false object
    StorageService service;

    @InjectMocks // injects the false object (mock) into the real service
    SyncService syncService;

    SaveState localSave;
    SaveState portableSave;
    List<SaveState> localSaves;
    List<SaveState> portableSaves;

    @BeforeEach
    void init() {

        localSave = SaveState.builder()
                .fileName("Chrono_trigger.srm")
                .extension("srm")
                .sizeInBytes(100L)
                .absolutePath("pc/save/Chrono_trigger.srm")
                .lastModified(LocalDateTime.now())
                .build();

        portableSave = SaveState.builder()
                .fileName("Chrono_trigger.srm")
                .extension("srm")
                .sizeInBytes(100L)
                .absolutePath("portable/save/Chrono_trigger.srm")
                .lastModified(LocalDateTime.now())
                .build();

        localSaves = new ArrayList<>(List.of(localSave));
        portableSaves = new ArrayList<>(List.of(portableSave));

    }

    @Test
    @DisplayName("Should sync from PC to portable when PC save is newer")
    void shouldSyncFromPCToPortableWhenPCSaveIsNewer() {

        localSave.setLastModified(LocalDateTime.now().plusDays(1));
        syncService.compareAndSync(localSaves, portableSaves, localSave.getAbsolutePath(), portableSave.getAbsolutePath());

        Mockito.verify(service, Mockito.times(1)).uploadFile(
                Paths.get(localSave.getAbsolutePath()),
                Paths.get(portableSave.getAbsolutePath())
        );
    }

    @Test
    @DisplayName("Should sync from portable to PC when portable save is newer")
    void shouldSyncFromPortableToPCWhenPortableSaveIsNewer() {

        portableSave.setLastModified(LocalDateTime.now().plusDays(1));
        syncService.compareAndSync(localSaves, portableSaves, localSave.getAbsolutePath(), portableSave.getAbsolutePath());

        Mockito.verify(service, Mockito.times(1)).downloadFile(
                Paths.get(portableSave.getAbsolutePath()),
                Paths.get(localSave.getAbsolutePath())
        );
    }

    @Test
    @DisplayName("Should not trigger any operation when both saves have the same last modified")
    void shouldNotTriggerAnyOperationWhenBothSavesHaveTheSameLastModified() {

        LocalDateTime now = LocalDateTime.now();

        localSave.setLastModified(now);
        portableSave.setLastModified(now);

        syncService.compareAndSync(localSaves, portableSaves, localSave.getAbsolutePath(), portableSave.getAbsolutePath());

        Mockito.verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Should throw SaveSyncException when file replacement fail")
    void shouldThrowSaveSyncExceptionWhenFileReplacementFail() {

        localSave.setLastModified(LocalDateTime.now().plusDays(10));

        Mockito.doThrow(new SaveSyncException("simulate SD card disconnection"))
                .when(service).uploadFile(
                        Paths.get(localSave.getAbsolutePath()),
                        Paths.get(portableSave.getAbsolutePath())
                );

        SaveSyncException exception = assertThrows(
                SaveSyncException.class,
                () -> syncService.compareAndSync(
                        localSaves,
                        portableSaves,
                        localSave.getAbsolutePath(),
                        portableSave.getAbsolutePath()));

        assertThat(exception.getMessage()).isEqualTo("simulate SD card disconnection");
    }
}