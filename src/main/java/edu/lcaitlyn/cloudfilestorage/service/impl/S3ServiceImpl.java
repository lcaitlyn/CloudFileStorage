package edu.lcaitlyn.cloudfilestorage.service.impl;

import edu.lcaitlyn.cloudfilestorage.DTO.S3ObjectMetadata;
import edu.lcaitlyn.cloudfilestorage.config.S3Properties;
import edu.lcaitlyn.cloudfilestorage.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class S3ServiceImpl implements S3Service {

    private final S3Client s3;

    private final S3Properties s3Properties;

    @Override
    public S3ObjectMetadata getObject(String key) {
        HeadObjectRequest objectRequest = HeadObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(key)
                .build();

        HeadObjectResponse response = s3.headObject(objectRequest);

        return S3ObjectMetadata.builder()
                .contentLength(response.contentLength())
                .build();
    }

    @Override
    public boolean isDirectory(String key) {
        if (!key.endsWith("/")) {
            key += "/";
        }

        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(s3Properties.getBucket())
                .prefix(key)
                .maxKeys(1)
                .build();

        ListObjectsV2Response response = s3.listObjectsV2(listRequest);
        return !response.contents().isEmpty();
    }

    @Override
    public boolean isFileExist(String key) {
        try {
            getObject(key);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    @Override
    public void putObject(String key, S3ObjectMetadata metadata) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(key)
                .contentType(metadata.getContentType())
                .build();

        s3.putObject(objectRequest, RequestBody.fromBytes(metadata.getData()));
    }

    @Override
    public void deleteObject(String key) {
        HeadObjectRequest headReq = HeadObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(key)
                .build();

        s3.headObject(headReq);

        s3.deleteObject(DeleteObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(key)
                .build());
    }

    @Override
    public void deleteDirectory(String key) {
        List<S3Object> objects = s3.listObjectsV2(ListObjectsV2Request.builder()
                        .bucket(s3Properties.getBucket())
                        .prefix(key)
                        .build()
                )
                .contents();

        List<ObjectIdentifier> toDelete = objects.stream()
                .map(obj -> ObjectIdentifier.builder().key(obj.key()).build())
                .toList();

        s3.deleteObjects(DeleteObjectsRequest.builder()
                .bucket(s3Properties.getBucket())
                .delete(Delete.builder().objects(toDelete).build())
                .build());
    }

    @Override
    public S3ObjectMetadata downloadObject(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(key)
                .build();

        try (ResponseInputStream<GetObjectResponse> response = s3.getObject(getObjectRequest)) {
            return S3ObjectMetadata.builder()
                    .data(response.readAllBytes())
                    .build();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while downloading file");
        }
    }

    @Override
    public S3ObjectMetadata downloadDirectory(String key) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                        .bucket(s3Properties.getBucket())
                        .prefix(key)
                        .build();

                ListObjectsV2Response response = s3.listObjectsV2(listRequest);

                for (S3Object o : response.contents()) {
                    String name = o.key().substring(key.length());

                    if (name.isEmpty() || o.key().equals(key)) continue;

                    GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                            .bucket(s3Properties.getBucket())
                            .key(o.key())
                            .build();

                    try (ResponseInputStream<GetObjectResponse> s3Object = s3.getObject(getObjectRequest)) {
                        zos.putNextEntry(new ZipEntry(name));
                        s3Object.transferTo(zos);
                        zos.closeEntry();
                    }
                }
            }

            return S3ObjectMetadata.builder()
                    .data(baos.toByteArray())
                    .build();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while downloading directory");
        }
    }

    @Override
    public void createDirectory(String key) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(key)
                .contentLength(0L)
                .build();

        s3.putObject(objectRequest, RequestBody.empty());
    }

    @Override
    public List<String> listDirectories(String key) {
        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(s3Properties.getBucket())
                .delimiter("/")
                .prefix(key)
                .build();

        ListObjectsV2Response response = s3.listObjectsV2(listRequest);

        return response.commonPrefixes().stream()
                .map(CommonPrefix::prefix)
                .collect(Collectors.toList());
    }

    @Override
    public List<S3Object> listObjects(String key) {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(s3Properties.getBucket())
                .prefix(key)
                .delimiter("/")
                .build();

        ListObjectsV2Response response = s3.listObjectsV2(request);
        return response.contents();
    }

    @Override
    public ListObjectsV2Response listAllObjects(String prefix, String continuationToken) {
        ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder()
                .bucket(s3Properties.getBucket())
                .prefix(prefix)
                .maxKeys(1000);

        if (continuationToken != null) {
            builder.continuationToken(continuationToken);
        }

        return s3.listObjectsV2(builder.build());
    }

    @Override
    public void copyObject(String fromKey, String toKey) {
        CopyObjectRequest request = CopyObjectRequest.builder()
                .sourceBucket(s3Properties.getBucket())
                .sourceKey(fromKey)
                .destinationBucket(s3Properties.getBucket())
                .destinationKey(toKey)
                .build();

        s3.copyObject(request);
    }

    @Override
    public boolean isResourceExists(String key) {
        return isFileExist(key) || isDirectory(key);
    }
}
