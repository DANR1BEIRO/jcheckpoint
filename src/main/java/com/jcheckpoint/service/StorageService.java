package com.jcheckpoint.service;

import com.jcheckpoint.model.SaveState;

import java.nio.file.Path;
import java.util.List;

public interface StorageService {
    List<SaveState> listAllSaves(Path path);

    void uploadFile(Path localSource, Path remoteDestination);

    void downloadFile(Path remoteSource, Path localDestination);
}
