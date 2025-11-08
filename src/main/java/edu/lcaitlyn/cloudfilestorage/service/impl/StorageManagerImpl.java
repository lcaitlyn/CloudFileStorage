package edu.lcaitlyn.cloudfilestorage.service.impl;

import edu.lcaitlyn.cloudfilestorage.DTO.ResourceMetadata;
import edu.lcaitlyn.cloudfilestorage.DTO.StorageDTO;
import edu.lcaitlyn.cloudfilestorage.enums.Type;
import edu.lcaitlyn.cloudfilestorage.exception.StorageException;
import edu.lcaitlyn.cloudfilestorage.repository.S3Repository;
import edu.lcaitlyn.cloudfilestorage.service.StorageManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@AllArgsConstructor
public class StorageManagerImpl implements StorageManager {

    private final S3Repository s3Repository;

    @Override
    public StorageDTO getResourceMetadata(String key) {
        try {
            HeadObjectResponse response = s3Repository.getObject(key);
            boolean isDirectory = isDirectory(key);

            return StorageDTO.builder()
                    .key(key)
                    .type((isDirectory) ? Type.DIRECTORY : Type.FILE)
                    .size(response.contentLength())
                    .build();
        } catch (NoSuchKeyException e) {
            return null;
        } catch (S3Exception e) {
            throw new StorageException("Error while getting metadata for key: " + key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        return getResourceMetadata(key) != null;
    }

    @Override
    public void save(String key, byte[] data, String contentType) {
        try {
            createDirectory(key);
            s3Repository.putObject(key, data, contentType);
            log.info("saved key: " + key);
        } catch (S3Exception e) {
            throw new StorageException("Error while saving data for key: " + key, e);
        }
    }

    @Override
    public List<StorageDTO> getDirectory(String key) {
        try {
            List<StorageDTO> list = new ArrayList<>();

            ListObjectsV2Response response = s3Repository.listKeyObjects(key);

            List<StorageDTO> files = response.contents().stream()
                    .filter(obj -> !obj.key().equals(key))
                    .map(obj -> StorageDTO.builder()
                            .key(obj.key())
                            .size(obj.size())
                            .type(Type.FILE)
                            .build())
                    .toList();

            List<StorageDTO> directories = response.commonPrefixes().stream()
                    .map(obj -> StorageDTO.builder()
                            .key(obj.prefix())
                            .size(0L)
                            .type(Type.DIRECTORY)
                            .build())
                    .toList();

            list.addAll(files);
            list.addAll(directories);

            return list;
        } catch (S3Exception e) {
            throw new StorageException("Error while getting metadata for key: " + key, e);
        }
    }

    @Override
    public List<StorageDTO> getFullDirectory(String key) {
        if (!isDirectory(key)) {
            return null;
        }

        try {
            List<StorageDTO> list = new ArrayList<>();

            List<S3Object> objects = s3Repository.listAllObjects(key);
            for (S3Object obj : objects) {
                if (obj.key().equals(key)) continue;

                Type type = (obj.key().endsWith("/") && obj.size() == 0 ? Type.DIRECTORY : Type.FILE);

                list.add(StorageDTO.builder()
                        .key(obj.key())
                        .size(obj.size())
                        .type(type)
                        .build());
            }

            return list;
        } catch (S3Exception e) {
            throw new StorageException("Error while getting metadata for key: " + key, e);
        }
    }

    private String deleteFilenameForKey(String key) {
        if (!key.endsWith("/")) {
            return key.substring(0, key.lastIndexOf("/") + 1);
        }
        return key;
    }

    private List<String> splitToDirectories(String dirKey) {
        if (dirKey.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(dirKey.split("/"))
                .filter(s -> !s.isEmpty())
                .toList();
    }

    @Override
    public void createDirectory(String key) {
        try {
            key = deleteFilenameForKey(key);

            StringBuilder builder = new StringBuilder();
            for (String dir : splitToDirectories(key)) {
                builder.append(dir).append("/");

                String curDir = builder.toString();
                if (!exists(curDir)) {
                    s3Repository.createDirectory(curDir);
                    log.info("createdDirectory: " + curDir);
                }
            }
        } catch (S3Exception e) {
            throw new StorageException("Error while creating directory for key: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            boolean isDirectory = isDirectory(key);

            if (isDirectory) {
                s3Repository.deleteDirectory(key);
                log.info("deleted directory key: " + key);
            } else {
                s3Repository.deleteObject(key);
                log.info("deleted object key: " + key);
            }
        } catch (S3Exception e) {
            throw new StorageException("Error while deleting key: " + key, e);
        }
    }

    @Override
    public void copy(String sourceKey, String targetKey) {
        try {
            createDirectory(targetKey);

            if (isDirectory(sourceKey)) {
                List<S3Object> objects = s3Repository.listAllObjects(sourceKey);
                for (S3Object obj : objects) {
                    if (obj.key().equals(sourceKey)) continue;

                    String destination = targetKey + obj.key().substring(sourceKey.length());
                    s3Repository.copyObject(obj.key(), destination);
                    log.info("copied key: " + obj.key() + " to target key: " + destination);
                }
            } else {
                s3Repository.copyObject(sourceKey, targetKey);
                log.info("copied key: " + sourceKey + " to target key: " + targetKey);
            }
        } catch (S3Exception e) {
            throw new StorageException("Error while copying key: " + sourceKey, e);
        }
    }

    @Override
    public void move(String sourceKey, String targetKey) {
        try {
            copy(sourceKey, targetKey);
            delete(sourceKey);
            log.info("moved key: " + sourceKey + " to target key: " + targetKey);
        } catch (S3Exception e) {
            throw new StorageException("Error while moving key: " + sourceKey, e);
        }
    }

    @Override
    public ResourceMetadata downloadFile(String key) {
        try (ResponseInputStream<GetObjectResponse> rIS = s3Repository.downloadObject(key)) {
            GetObjectResponse response = rIS.response();

            return ResourceMetadata.builder()
                    .data(rIS.readAllBytes())
                    .contentType(response.contentType())
                    .contentLength(response.contentLength())
                    .build();
        } catch (S3Exception | IOException e) {
            throw new StorageException("Error while downloading file with key: " + key, e);
        }
    }

    @Override
    public boolean isDirectory(String key) {
        if (!key.endsWith("/")) {
            key += "/";
        }

        ListObjectsV2Response response = s3Repository.listKeyObjects(key, 1);
        return !response.contents().isEmpty();
    }

    @Override
    public ResourceMetadata downloadFolder(String key, String folderName) {
        if (!isDirectory(key)) {
            throw new StorageException("Error while downloading folder with key. No such directory: " + key);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {

            List<S3Object> response = s3Repository.listAllObjects(key);

            for (S3Object o : response) {
                String name = o.key().substring(key.length());

                if (name.isEmpty() || o.key().equals(key)) continue;

                try (ResponseInputStream<GetObjectResponse> rIS = s3Repository.downloadObject(o.key())) {
                    zos.putNextEntry(new ZipEntry(folderName + "/" + name));
                    rIS.transferTo(zos);
                    zos.closeEntry();
                }
                log.info("downloaded key: " + o.key());
            }
        } catch (S3Exception | IOException e) {
            throw new StorageException("Error while downloading folder with key: " + key, e);
        }
        return ResourceMetadata.builder()
                .data(baos.toByteArray())
                .build();
    }
}
