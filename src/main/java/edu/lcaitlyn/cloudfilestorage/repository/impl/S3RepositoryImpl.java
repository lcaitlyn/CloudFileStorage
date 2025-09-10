package edu.lcaitlyn.cloudfilestorage.repository.impl;

import edu.lcaitlyn.cloudfilestorage.config.S3Properties;
import edu.lcaitlyn.cloudfilestorage.repository.S3Repository;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.ArrayList;
import java.util.List;

@Repository
public class S3RepositoryImpl implements S3Repository {

    private final S3Client s3;

    private final S3Properties s3Properties;

    private final String bucketName;

    public S3RepositoryImpl(S3Client s3, S3Properties s3Properties) {
        this.s3 = s3;
        this.s3Properties = s3Properties;
        this.bucketName = s3Properties.getBucket();
    }

    @Override
    public HeadObjectResponse getObject(String key) {
        HeadObjectRequest objectRequest = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        return s3.headObject(objectRequest);
    }

    @Override
    public void putObject(String key, byte[] data, String contentType) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        s3.putObject(objectRequest, RequestBody.fromBytes(data));
    }

    @Override
    public void deleteObject(String key) {
        HeadObjectRequest headReq = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3.headObject(headReq);

        s3.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build());
    }

    @Override
    public void deleteDirectory(String key) {
        List<S3Object> objects = s3.listObjectsV2(ListObjectsV2Request.builder()
                        .bucket(bucketName)
                        .prefix(key)
                        .build()
                )
                .contents();

        List<ObjectIdentifier> toDelete = objects.stream()
                .map(obj -> ObjectIdentifier.builder().key(obj.key()).build())
                .toList();

        s3.deleteObjects(DeleteObjectsRequest.builder()
                .bucket(bucketName)
                .delete(Delete.builder().objects(toDelete).build())
                .build());
    }

    @Override
    public ResponseInputStream<GetObjectResponse> downloadObject(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        return s3.getObject(getObjectRequest);
    }

    @Override
    public void createDirectory(String key) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentLength(0L)
                .build();

        s3.putObject(objectRequest, RequestBody.empty());
    }

    @Override
    public ListObjectsV2Response listKeyObjects(String key) {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(key)
                .delimiter("/")
                .build();

        return s3.listObjectsV2(request);
    }

    @Override
    public ListObjectsV2Response listKeyObjects(String key, int maxKeys) {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(key)
                .delimiter("/")
                .maxKeys(maxKeys)
                .build();

        return s3.listObjectsV2(request);
    }

    @Override
    public List<S3Object> listAllObjects(String key) {
        List<S3Object> allObjects = new ArrayList<>();
        String continuationToken = null;

        do {
            ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(key);

            if (continuationToken != null) {
                requestBuilder.continuationToken(continuationToken);
            }

            ListObjectsV2Response response = s3.listObjectsV2(requestBuilder.build());
            allObjects.addAll(response.contents());
            continuationToken = response.nextContinuationToken();

        } while (continuationToken != null);

        return allObjects;
    }

    @Override
    public void copyObject(String fromKey, String toKey) {
        CopyObjectRequest request = CopyObjectRequest.builder()
                .sourceBucket(bucketName)
                .sourceKey(fromKey)
                .destinationBucket(bucketName)
                .destinationKey(toKey)
                .build();

        s3.copyObject(request);
    }
}
