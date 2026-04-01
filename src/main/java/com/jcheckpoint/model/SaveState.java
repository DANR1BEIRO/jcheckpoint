package com.jcheckpoint.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveState {

    private String fileName;
    private String extension;
    private long sizeInBytes; // Used to detect if the file is corrupted or has been modified
    private String absolutePath;

    /**
     * Essential attribute for synchronization logic.
     * It determines which file is the most recent between the PC and the handheld.
     */
    private LocalDateTime lastModified;
}