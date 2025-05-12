package edu.lcaitlyn.cloudfilestorage.service.impl;

import edu.lcaitlyn.cloudfilestorage.DTO.MoveResourceRequestDTO;
import edu.lcaitlyn.cloudfilestorage.DTO.ResourceResponseDTO;
import edu.lcaitlyn.cloudfilestorage.DTO.ResourceRequestDTO;
import edu.lcaitlyn.cloudfilestorage.config.S3Properties;
import edu.lcaitlyn.cloudfilestorage.enums.ResourceType;
import edu.lcaitlyn.cloudfilestorage.exception.DirectoryNotFound;
import edu.lcaitlyn.cloudfilestorage.models.User;
import edu.lcaitlyn.cloudfilestorage.service.FileService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@AllArgsConstructor
public class FileServiceImpl implements FileService {

    private final S3Client s3;

    private final S3Properties s3Properties;

    private final String pathPrefixFormat = "user-%d-files";

    @Override
    public List<ResourceResponseDTO> uploadFile(ResourceRequestDTO request) throws IOException {
        User user = request.getUser();
        String path = request.getPath();
        MultipartFile[] files = request.getFiles();

        String prefix = addUserPrefix(user.getId(), path);
        System.out.println("FileServiceImpl: uploadFile: path = " + path + " prefix = " + prefix);

        for (MultipartFile file : files) {
            try {
                getFile(ResourceRequestDTO.builder()
                        .user(user)
                        .path(path + file.getOriginalFilename())
                        .build());

                // если нашел такой файл выйдет ошибка ебать
                throw new ResponseStatusException(HttpStatus.CONFLICT, "File \"" + file.getOriginalFilename() + "\" already exists.");
            } catch (NoSuchKeyException e) {
            }
        }

        List<ResourceResponseDTO> response = new ArrayList<>();
        for (MultipartFile file : files) {
            PutObjectRequest objectRequest = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(prefix + file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .build();

            System.out.println("FileServiceImpl: uploadFile: path = " + path + " key = " + prefix + file.getOriginalFilename());
            s3.putObject(objectRequest, RequestBody.fromBytes(file.getBytes()));

            response.add(ResourceResponseDTO.builder()
                    .name(file.getOriginalFilename())
                    .size(file.getSize())
                    .path(path)
                    .resourceType(ResourceType.FILE)
                    .build()
            );
        }

        return response;
    }

    @Override
    public ResourceResponseDTO getFile(ResourceRequestDTO request) {
        User user = request.getUser();
        String path = request.getPath();

        String key = addUserPrefix(user.getId(), path);
        System.out.println("FileServiceImpl: getFile: path = " + path + " key = " + key);

        HeadObjectRequest objectRequest = HeadObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(key)
                .build();

        HeadObjectResponse response = s3.headObject(objectRequest);

        return ResourceResponseDTO.builder()
                .name(extractNameFromKey(key))
                .path(path)
                .size(response.contentLength())
                .resourceType(ResourceType.FILE)
                .build();
    }

    @Override
    public List<ResourceResponseDTO> getDirectory(ResourceRequestDTO request) {
        User user = request.getUser();
        String path = request.getPath();

        String prefix = addUserPrefix(user.getId(), path);
        System.out.println("FileServiceImpl: getDirectory: path = " + path + " prefix = " + prefix);

        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(s3Properties.getBucket())
                .delimiter("/")
                .prefix(prefix)
                .build();

        List<ResourceResponseDTO> result = new ArrayList<>();
        ListObjectsV2Response response = s3.listObjectsV2(listRequest);

        if (response.contents().isEmpty() && response.commonPrefixes().isEmpty()) {
            throw new DirectoryNotFound("Directory not found " + request.getPath());
        }

        for (CommonPrefix c : response.commonPrefixes()) {
            result.add(ResourceResponseDTO.builder()
                    .name(extractNameFromKey(path)) // todo как-то хуево отрабатывает
                    .path(request.getPath())
                    .resourceType(ResourceType.DIRECTORY)
                    .build());
        }

        for (S3Object o : response.contents()) {
            String fullKey = o.key();
            if (fullKey.equals(prefix)) continue; // Пропускаем саму папку

            String relativePath = fullKey.substring(prefix.length());
            if (relativePath.contains("/")) {
                // Пропускаем вложенные элементы — они попадут в подзапрос
                continue;
            }

            result.add(ResourceResponseDTO.builder()
                    .name(extractNameFromKey(relativePath)) // todo как-то хуево отрабатывает
                    .path(request.getPath())
                    .size(o.size())
                    .resourceType(ResourceType.FILE)
                    .build());
        }

        return result;
    }

    @Override
    public ResourceResponseDTO createDirectory(ResourceRequestDTO request) {
        User user = request.getUser();
        String path = request.getPath();

        String prefix = addUserPrefix(user.getId(), path);
        System.out.println("FileServiceImpl: createDirectory: path = " + path + " prefix = " + prefix);

        try {
            PutObjectRequest objectRequest = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(prefix)
                    .contentLength(0L)
                    .build();

            s3.putObject(objectRequest, RequestBody.empty());

            return ResourceResponseDTO.builder()
                    .name(extractNameFromKey(path))
                    .resourceType(ResourceType.DIRECTORY)
                    .path(path)
                    .build();
        } catch (S3Exception e) {
            throw new RuntimeException("Failed to create directory in MinIO: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    @Override
    public void deleteResourceOrDirectory(ResourceRequestDTO request) {
        User user = request.getUser();
        String path = request.getPath();

        String prefix = addUserPrefix(user.getId(), path);

        boolean isFolder = s3.listObjectsV2(ListObjectsV2Request.builder()
                .bucket(s3Properties.getBucket())
                .prefix(prefix + "/")
                .maxKeys(1)
                .build()
        ).hasContents();

        if (isFolder) {
            System.out.println("FileServiceImpl: deleteDirectory: path = " + path + " key = " + prefix);
            deleteDirectory(prefix);
        } else {
            System.out.println("FileServiceImpl: deleteResource: path = " + path + " key = " + prefix);
            deleteResource(prefix);
        }
    }

    private void deleteResource(String key) {
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

    private void deleteDirectory(String key) {
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

    // todo мб реализовать чтобы он и искал не только пустые папки
    @Override
    public List<ResourceResponseDTO> findResource(ResourceRequestDTO request) {
        User user = request.getUser();
        String query = request.getPath().toLowerCase();

        String prefix = addUserPrefix(user.getId(), "/");
        System.out.println("FileServiceImpl: findResource: path = " + prefix + " query = " + query);

        List<ResourceResponseDTO> result = new ArrayList<>();

        String continuationToken = null;

        while (true) {
            ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder()
                    .bucket(s3Properties.getBucket())
                    .prefix(prefix)
                    .maxKeys(1000);

            if (continuationToken != null) {
                builder = builder.continuationToken(continuationToken);
            }

            ListObjectsV2Response response = s3.listObjectsV2(builder.build());

            for (S3Object o : response.contents()) {
                String key = o.key();
                String name = extractNameFromKey(key);

                if (!name.toLowerCase().contains(query)) {
                    continue;
                }

                boolean isDirectory = key.endsWith("/");
                String path = extractPathFromKey(key);

                result.add(ResourceResponseDTO.builder()
                        .name(name)
                        .resourceType((isDirectory ? ResourceType.DIRECTORY : ResourceType.FILE))
                        .path(path)
                        .size(o.size())
                        .build());
            }

            if (!response.isTruncated()) break;

            continuationToken = response.nextContinuationToken();
        }

        return result;
    }

    // todo проблема этого говна в том что я не знаю что сюда падает в from и to. Надеюсь это просто файл и его новый
//    путь. Т.е содержимое файла просто перетекает в новый key
    @Override
    public ResourceResponseDTO moveResource(MoveResourceRequestDTO request) {
        User user = request.getUser();
        String from = request.getFrom();
        String to = request.getTo();

        from = addUserPrefix(user.getId(), from);
        to = addUserPrefix(user.getId(), to);

        // Проверяем, что from существует и это файл
        try {
            s3.headObject(HeadObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(from)
                    .build());
        } catch (NoSuchKeyException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found: " + request.getFrom()); // 404
        }

        // Проверяем, что to не занят
        try {
            s3.headObject(HeadObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(to)
                    .build());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Destination already exists: " + request.getTo()); // 409
        } catch (NoSuchKeyException e) {
            // Если объекта нет, это хорошо, продолжаем
        }

        CopyObjectRequest objectRequest = CopyObjectRequest.builder()
                .sourceBucket(s3Properties.getBucket())
                .sourceKey(from)
                .destinationBucket(s3Properties.getBucket())
                .destinationKey(to)
                .build();

        s3.copyObject(objectRequest);
        s3.deleteObject(DeleteObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(from)
                .build()
        );

        Long size = s3.headObject(HeadObjectRequest.builder()
                        .bucket(s3Properties.getBucket())
                        .key(to)
                        .build())
                .contentLength();

        return ResourceResponseDTO.builder()
                .name(extractPathFromKey(to))
                .resourceType(ResourceType.FILE)
                .size(size)
                .build();
    }

    @Override
    public byte[] download(ResourceRequestDTO request) throws NoSuchKeyException, IOException {
        User user = request.getUser();
        String path = request.getPath();

        String key = addUserPrefix(user.getId(), path);

        if (isDirectory(key)) {
            return zipDirectory(key);
        }

        return downloadFile(key);
    }

    private byte[] downloadFile(String key) throws NoSuchKeyException, IOException {
        System.out.println("FileServiceImpl: downloadFile: key = " + key);

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(key)
                .build();

        try (ResponseInputStream<GetObjectResponse> response = s3.getObject(getObjectRequest)) {
            return response.readAllBytes();
        }
    }

    private byte[] zipDirectory(String key) throws NoSuchKeyException, IOException {
        System.out.println("FileServiceImpl: downloadDirectory: key = " + key);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(s3Properties.getBucket())
                    .prefix(key)
                    .build();

            ListObjectsV2Response response = s3.listObjectsV2(listRequest);

            for (S3Object o : response.contents()) {
                String name = o.key().substring(key.length());

                if (name.isEmpty()) continue;

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
        return baos.toByteArray();
    }

    private boolean isDirectory(String key) throws NoSuchKeyException {
        if (!key.endsWith("/")) {
            s3.headObject(HeadObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(key)
                    .build());
            return false;
        }

        ListObjectsV2Response list = s3.listObjectsV2(ListObjectsV2Request.builder()
                .bucket(s3Properties.getBucket())
                .prefix(key)
                .maxKeys(1)
                .build());

        return !list.contents().isEmpty();
    }

    private String addUserPrefix(Long userId, String path) {
        String prefix = String.format(pathPrefixFormat, userId);
        return prefix + path;
    }

    private String extractNameFromKey(String key) {
        if (key.endsWith("/")) {
            key = key.substring(0, key.length() - 1);
        }

        int lastSlash = key.lastIndexOf('/');
        if (lastSlash < 0) {
            return ""; // это root-папка, имя пустое
        }

        return key.substring(lastSlash + 1);
    }

    private String extractPathFromKey(String key) {
        if (!key.contains("/")) {
            return "/"; // это root
        }

        int lastSlash = key.lastIndexOf('/');
        String rawPath = key.substring(0, lastSlash + 1); // включая слэш

        // Убираем префикс пользователя (например, "user-9-files/")
        String cleaned = rawPath.replaceFirst("^user-\\d+-files/", "");

        return cleaned.isEmpty() ? "/" : "/" + cleaned;
    }
}
