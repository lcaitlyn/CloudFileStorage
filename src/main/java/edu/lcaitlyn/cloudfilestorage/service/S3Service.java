package edu.lcaitlyn.cloudfilestorage.service;

import edu.lcaitlyn.cloudfilestorage.DTO.S3ObjectMetadata;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.util.List;

@Service
public interface S3Service {
    /**
     * Получает метаданные объекта из S3 по ключу.
     *
     * @param key Ключ объекта (включая путь)
     * @return Метаданные объекта, включая размер
     * @throws NoSuchKeyException если объект не найден в S3
     * @throws S3Exception при других ошибках обращения к S3
     */
    S3ObjectMetadata getObject(String key);

    boolean isDirectory(String key);

    boolean isFileExist(String key);

    void putObject(String key, S3ObjectMetadata metadata);

    void deleteObject(String key);

    void deleteDirectory(String key);

    S3ObjectMetadata downloadObject(String key);

    S3ObjectMetadata downloadDirectory(String key);

    void createDirectory(String key);

    List<String> listDirectories(String key);

    List<S3Object> listObjects(String key);

    ListObjectsV2Response listAllObjects(String prefix, String continuationToken);

    void copyObject(String fromKey, String toKey);

    boolean isResourceExists(String key);
}
