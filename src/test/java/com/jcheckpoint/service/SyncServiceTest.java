package com.jcheckpoint.service;

import com.jcheckpoint.exception.SaveSyncException;
import com.jcheckpoint.model.SaveState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class SyncServiceTest {

    @Mock // creates a false object
    LocalStorageService service;

    @InjectMocks // injects the false object (mock) into the real service
    SyncService syncService;

    @Test
    @DisplayName("Should sync from PC to portable when PC save is newer")
    void shouldSyncFromPCToPortableWhenPCSaveIsNewer() {

        SaveState pcSave = SaveState.builder()
                .fileName("Chrono_trigger.srm")
                .extension("srm")
                .sizeInBytes(100L)
                .absolutePath("pc/save/Chrono_trigger.srm")
                .lastModified(LocalDateTime.now().plusDays(1))
                .build();

        SaveState portableSave = SaveState.builder()
                .fileName("Chrono_trigger.srm")
                .extension("srm")
                .sizeInBytes(100L)
                .absolutePath("portable/save/Chrono_trigger.srm")
                .lastModified(LocalDateTime.now())
                .build();

        List<SaveState> pcSaveList = List.of(pcSave);
        List<SaveState> portableSaveList = List.of(portableSave);

        syncService.compareAndSync(pcSaveList, portableSaveList, pcSave.getAbsolutePath(), portableSave.getAbsolutePath());

        Mockito.verify(service, Mockito.times(1)).replaceFile(
                Paths.get(pcSave.getAbsolutePath()),
                Paths.get(portableSave.getAbsolutePath())
        );
    }

    @Test
    @DisplayName("Should sync from portable to PC when portable save is newer")
    void shouldSyncFromPortableToPCWhenPortableSaveIsNewer() {

        SaveState pcSave = SaveState.builder()
                .fileName("Chrono_trigger.srm")
                .extension("srm")
                .sizeInBytes(100L)
                .absolutePath("pc/save/Chrono_trigger.srm")
                .lastModified(LocalDateTime.now())
                .build();

        SaveState portableSave = SaveState.builder()
                .fileName("Chrono_trigger.srm")
                .extension("srm")
                .sizeInBytes(100L)
                .absolutePath("portable/save/Chrono_trigger.srm")
                .lastModified(LocalDateTime.now().plusDays(1))
                .build();

        List<SaveState> pcSaveList = List.of(pcSave);
        List<SaveState> portableSaveList = List.of(portableSave);

        syncService.compareAndSync(pcSaveList, portableSaveList, pcSave.getAbsolutePath(), portableSave.getAbsolutePath());

        Mockito.verify(service, Mockito.times(1)).replaceFile(
                Paths.get(portableSave.getAbsolutePath()),
                Paths.get(pcSave.getAbsolutePath())
        );
    }

    @Test
    @DisplayName("Should not trigger any operation when both saves have the same last modifffied")
    void shouldNotTriggerAnyOperationWhenBothSavesHaveTheSameLastModified() {

        LocalDateTime exactlyTheSameTime = LocalDateTime.now();

        SaveState pcSave = SaveState.builder()
                .fileName("Chrono_trigger.srm")
                .extension("srm")
                .sizeInBytes(100L)
                .absolutePath("pc/save/Chrono_trigger.srm")
                .lastModified(exactlyTheSameTime)
                .build();

        SaveState portableSave = SaveState.builder()
                .fileName("Chrono_trigger.srm")
                .extension("srm")
                .sizeInBytes(100L)
                .absolutePath("portable/save/Chrono_trigger.srm")
                .lastModified(exactlyTheSameTime)
                .build();

        List<SaveState> pcSaveList = List.of(pcSave);
        List<SaveState> portableSaveList = List.of(portableSave);

        syncService.compareAndSync(pcSaveList, portableSaveList, pcSave.getAbsolutePath(), portableSave.getAbsolutePath());

        Mockito.verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Should throw SaveSyncException when file replacement fail")
    void shouldThrowSaveSyncExceptionWhenFileReplacementFail() {
        SaveState pcSave = SaveState.builder()
                .fileName("Chrono_trigger.srm")
                .extension("srm").sizeInBytes(100L)
                .absolutePath("pc/save/Chrono_trigger.srm")
                .lastModified(LocalDateTime.now().plusDays(1))
                .build();

        SaveState portableSave = SaveState.builder()
                .fileName("Chrono_trigger.srm")
                .extension("srm")
                .sizeInBytes(100L)
                .absolutePath("portable/save/Chrono_trigger.srm")
                .lastModified(LocalDateTime.now())
                .build();


        Mockito.doThrow(new SaveSyncException("simulate SD card disconnection"))
                .when(service).replaceFile(
                        Paths.get(pcSave.getAbsolutePath()),
                        Paths.get(portableSave.getAbsolutePath())
                );

        SaveSyncException exception = assertThrows(
                SaveSyncException.class,
                () -> syncService.compareAndSync(List.of(pcSave), List.of(portableSave), pcSave.getAbsolutePath(), portableSave.getAbsolutePath()));

        assertThat(exception.getMessage()).isEqualTo("simulate SD card disconnection");
    }


}