package com.jcheckpoint.service;

import com.jcheckpoint.model.SaveState;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "sftp")
public class SftpStorageService implements StorageService {

    @Value("#{'${app.trimui.save-extensions}'.split(',')}")
    List<String> validExtensions;

    @Value("${app.trimui.ip}")
    private String remoteHost;

    @Value("${app.trimui.user}")
    private String remoteUser;

    @Value("${app.trimui.password}")
    private String remotePassword;

    @Value("${app.trimui.save-path}")
    private String remoteSavePath;

    @Value("${app.trimui.port:2022}")
    private int remotePort;

    private SSHClient createConnectedClient() throws IOException {
        SSHClient ssh = new SSHClient();
        ssh.addHostKeyVerifier(new PromiscuousVerifier());

        log.info("Connecting to Trimui at {}:{}", remoteHost, remotePort);
        ssh.connect(remoteHost, remotePort);
        ssh.authPassword(remoteUser, remotePassword);

        return ssh;
    }

    private boolean isSaveFile(String name) {
        String lowerName = name.toLowerCase();
        return validExtensions.stream().anyMatch(lowerName::endsWith);
    }

    @Override
    public List<SaveState> listAllSaves(Path path) {
        List<SaveState> saves = new ArrayList<>();

        try (SSHClient ssh = createConnectedClient()) {
            try (SFTPClient sftp = ssh.newSFTPClient()) {
                log.info("Starting recursive search in: {}", remoteSavePath);
                recursiveScan(sftp, remoteSavePath, saves);
            }
        } catch (IOException e) {
            log.error("SFTP communication fail: {}", e.getMessage());
        }
        return saves;
    }

    private void recursiveScan(SFTPClient sftp, String currentPath, List<SaveState> foundSaves) throws IOException {
        List<RemoteResourceInfo> contents = sftp.ls(currentPath);

        for (RemoteResourceInfo item : contents) {
            String fullPath = currentPath.endsWith("/") ? currentPath + item.getName() : currentPath + "/" + item.getName();

            if (item.isDirectory()) {
                if (!item.getName().equals(".") && !item.getName().equals("..")) {
                    recursiveScan(sftp, fullPath, foundSaves);
                }
            } else if (isSaveFile(item.getName())) {
                foundSaves.add(mapToSaveState(item, currentPath));
            }
        }
    }

    @Override
    public void uploadFile(Path localSource, Path remotePath) {
        log.info("Starting upload: {} --> {}", localSource, remotePath);

        try (SSHClient ssh = createConnectedClient(); SFTPClient sftp = ssh.newSFTPClient()) {

            String remoteDirectory = remotePath.getParent().toString().replace("\\", "/");

            ensureRemoteDirectories(sftp, remoteDirectory);

            sftp.put(localSource.toString(), remotePath.toString());

            log.info("upload successful {}", remotePath.getFileName());

        } catch (IOException e) {
            log.error("Error during SFTP uploading: {}", e.getMessage());
        }
    }

    @Override
    public void downloadFile(Path remoteSource, Path localPath) {

        log.info("Starting download: {} -> {}", remoteSource, localPath);

        try {
            Path localDirectory = localPath.getParent();
            if (localDirectory != null && !Files.exists(localDirectory)) {
                Files.createDirectories(localDirectory);
                log.info("Created local directory: {}", localDirectory);
            }

            try (SSHClient ssh = createConnectedClient(); SFTPClient sftp = ssh.newSFTPClient()) {

                sftp.get(remoteSource.toString(), localPath.toString());
                log.info("download successful");
            }

        } catch (IOException e) {
            log.error("Error during SFTP downloading: {}", e.getMessage());
        }
    }

    private SaveState mapToSaveState(RemoteResourceInfo file, String path) {
        long lastModifiedEpoch = file.getAttributes().getMtime();
        LocalDateTime lastModified = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(lastModifiedEpoch),
                ZoneId.systemDefault()
        );

        return SaveState.builder()
                .fileName(file.getName())
                .sizeInBytes(file.getAttributes().getSize())
                .absolutePath(path + "/" + file.getName())
                .lastModified(lastModified)
                .build();
    }

    private void ensureRemoteDirectories(SFTPClient sftpClient, String directoryPath) throws IOException {
        String[] folders = directoryPath.split("/");
        StringBuilder currentPath = new StringBuilder();

        for (String folder : folders) {
            if (folder.isEmpty()) continue;

            currentPath.append(folder).append("/");
            String pathToCheck = currentPath.toString();

            try {
                sftpClient.stat(pathToCheck);
            } catch (IOException e) {
                log.info("Creating missing remote directory: {}", pathToCheck);
                sftpClient.mkdir(pathToCheck);
            }
        }
    }
}