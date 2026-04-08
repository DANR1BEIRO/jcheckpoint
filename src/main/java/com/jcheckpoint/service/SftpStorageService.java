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

    @Value("${app.trimui.ip}")
    private String host;

    @Value("${app.trimui.user}")
    private String userName;

    @Value("${app.trimui.password}")
    private String password;

    @Value("${app.trimui.save-path}")
    private String remoteSavePath;

    private SSHClient connect() throws IOException {
        SSHClient ssh = new SSHClient();

        ssh.addHostKeyVerifier(new PromiscuousVerifier());

        log.info("Trying connecting to Trimui via SSH with the IP {} in port 2022 ", host);
        ssh.connect(host, 2022);

        ssh.authPassword(userName, password);

        log.info("Successful SSH connection!");
        return ssh;
    }


    @Override
    public List<SaveState> listAllSaves(Path ignoreThisPath) {

        List<SaveState> saves = new ArrayList<>();

        try (SSHClient ssh = connect();
             SFTPClient sftp = ssh.newSFTPClient()) {

            log.info("Reading remote folder files: {}", remoteSavePath);
            List<RemoteResourceInfo> remoteFiles = sftp.ls(remoteSavePath);

            for (RemoteResourceInfo file : remoteFiles) {
                if (file.isRegularFile()) {

                    long lastModifiedEpoch = file.getAttributes().getMtime();
                    LocalDateTime lastModified = LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(lastModifiedEpoch),
                            ZoneId.systemDefault()
                    );

                    SaveState save = SaveState.builder()
                            .fileName(file.getName())
                            .sizeInBytes(file.getAttributes().getSize())
                            .absolutePath(file.getPath())
                            .lastModified(lastModified)
                            .build();

                    saves.add(save);
                }
            }
        } catch (IOException e) {
            log.error("fail read");
        }
        return saves;
    }

    @Override
    public void replaceFile(Path source, Path destination) {
        try (SSHClient ssh = connect();
             SFTPClient sftpClient = ssh.newSFTPClient()) {

            String sourcePath = source.toString().replace("\\", "/");
            String destinationPath = destination.toString().replace("\\", "/");

            if (Files.exists(source)) {

                log.info("Uploading from PC to trimui: {} -> {}", sourcePath, destinationPath);

                sftpClient.put(sourcePath, destinationPath);

                log.info("Upload successful!");

            } else {
                log.info("Downloading from trimui to PC: {} -> {}", sourcePath, destinationPath);

                sftpClient.get(sourcePath, destinationPath);

                log.info("Download successful!");
            }
        } catch (IOException e) {
            log.error("SFTP File transfer failed: {}", e.getMessage());
        }
    }
}
