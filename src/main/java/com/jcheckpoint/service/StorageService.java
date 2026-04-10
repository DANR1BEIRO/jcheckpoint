package com.jcheckpoint.service;

import com.jcheckpoint.model.SaveState;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface StorageService {
    List<SaveState> listAllSaves(Path path);
    void replaceFile(Path source, Path destination);
}
