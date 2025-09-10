package edu.lcaitlyn.cloudfilestorage.repository;

import org.springframework.stereotype.Repository;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.*;

import java.util.List;

@Repository
public interface S3Repository {
    /**
     * Получает метаданные объекта из S3 по ключу.
     *
     * @param key Ключ объекта (включая путь)
     * @return Метаданные объекта, включая размер
     * @throws S3Exception при других ошибках обращения к S3
     */
    HeadObjectResponse getObject(String key);

    void putObject(String key, byte[] data, String contentType);

    void deleteObject(String key);

    void deleteDirectory(String key);

    ResponseInputStream<GetObjectResponse> downloadObject(String key);

    void createDirectory(String key);

    List<S3Object> listAllObjects(String key);

    ListObjectsV2Response listKeyObjects(String key);

    ListObjectsV2Response listKeyObjects(String key, int maxKeys);

    void copyObject(String fromKey, String toKey);

}
