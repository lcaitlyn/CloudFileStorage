package edu.lcaitlyn.cloudfilestorage.service;

import edu.lcaitlyn.cloudfilestorage.DTO.ResourceMetadata;
import edu.lcaitlyn.cloudfilestorage.DTO.StorageDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface StorageManager {

    StorageDTO getResourceMetadata(String key);

    boolean exists(String key);

    void save(String key, byte[] data, String contentType);

    boolean isDirectory(String key);

    List<StorageDTO> getDirectory(String key);

    List<StorageDTO> getFullDirectory(String key);

    void createDirectory(String key);

    void deleteFile(String key);

    void deleteDirectory(String key);

    void copyFile(String sourceKey, String targetKey);

    void copyDirectory(String sourceKey, String targetKey);

    void moveFile(String sourceKey, String targetKey);

    void moveDirectory(String sourceKey, String targetKey);

    ResourceMetadata downloadFile(String key);

    ResourceMetadata downloadDirectory(String key, String directoryName);
}
