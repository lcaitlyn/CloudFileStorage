package edu.lcaitlyn.cloudfilestorage.service.impl;

import edu.lcaitlyn.cloudfilestorage.DTO.response.DownloadResourceResponseDTO;
import edu.lcaitlyn.cloudfilestorage.DTO.request.MoveResourceRequestDTO;
import edu.lcaitlyn.cloudfilestorage.DTO.response.ResourceResponseDTO;
import edu.lcaitlyn.cloudfilestorage.DTO.request.ResourceRequestDTO;
import edu.lcaitlyn.cloudfilestorage.config.S3Properties;
import edu.lcaitlyn.cloudfilestorage.enums.ResourceType;
import edu.lcaitlyn.cloudfilestorage.exception.DirectoryNotFound;
import edu.lcaitlyn.cloudfilestorage.exception.ResourceNotFound;
import edu.lcaitlyn.cloudfilestorage.models.User;
import edu.lcaitlyn.cloudfilestorage.service.FileService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@AllArgsConstructor
public class FileServiceImpl implements FileService {

    private final S3Client s3;

    private final S3Properties s3Properties;

    private final String pathPrefixFormat = "user-%d-files";

    @Override
    public List<ResourceResponseDTO> uploadFile(ResourceRequestDTO request) {
        User user = request.getUser();
        String path = request.getPath();
        MultipartFile[] files = request.getFiles();

        String prefix = addUserPrefix(user.getId(), path);
        System.out.println("FileServiceImpl: uploadFile: path = " + path + " prefix = " + prefix);


        try {
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
                        .path(path) // todo что-то не так тут. проверить!
                        .name(file.getOriginalFilename())
                        .size(file.getSize())
                        .resourceType(ResourceType.FILE)
                        .build()
                );
            }

            return response;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while uploading file");
        }
    }

    // todo проверка на директорию?
    @Override
    public ResourceResponseDTO getFile(ResourceRequestDTO request) {
        User user = request.getUser();
        String path = request.getPath();
        String key = addUserPrefix(user.getId(), path);
        System.out.println("FileServiceImpl: getFile: path = " + path + " key = " + key);

        try {
            HeadObjectRequest objectRequest = HeadObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(key)
                    .build();

            HeadObjectResponse response = s3.headObject(objectRequest);

            return ResourceResponseDTO.builder()
                    .path(path) // todo крч тут тоже ебала. она добавляет / перед path
                    .name(extractNameFromKey(key))
                    .size(response.contentLength())
                    .resourceType(ResourceType.FILE)
                    .build();
        } catch (NoSuchKeyException e) {
            throw new ResourceNotFound(e.getMessage());
        }
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
            String relativePath = c.prefix().substring(prefix.length());
            result.add(ResourceResponseDTO.builder()
                    .path(request.getPath())
                    .name(relativePath)
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
                    .path(extractPathFromKey(fullKey))
                    .name(relativePath)
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
                    .path(removeNameFormPath(path))
                    .name(extractNameFromKey(path) + "/")
                    .resourceType(ResourceType.DIRECTORY)
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

        try {
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
        } catch (NoSuchKeyException e) {
            throw new ResourceNotFound(e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while deleting resource");
        }

    }

    private void deleteResource(String key) {
        try {
            HeadObjectRequest headReq = HeadObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(key)
                    .build();

            s3.headObject(headReq);

            s3.deleteObject(DeleteObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(key)
                    .build());
        } catch (NoSuchKeyException e) {
            throw new ResourceNotFound(e.getMessage());
        } catch (S3Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while deleting resource");
        }
    }

    private void deleteDirectory(String key) {
        try {
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
        } catch (NoSuchKeyException e) {
            throw new ResourceNotFound(e.getMessage());
        } catch (S3Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while deleting resource");
        }
    }

    @Override
    public List<ResourceResponseDTO> findResource(ResourceRequestDTO request) {
        User user = request.getUser();
        String query = request.getPath().toLowerCase();
        String prefix = addUserPrefix(user.getId(), "/");
        System.out.println("FileServiceImpl: findResource: path = " + prefix + " query = " + query);

        try {
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
                            .path(path)
                            .name(name)
                            .resourceType((isDirectory ? ResourceType.DIRECTORY : ResourceType.FILE))
                            .size(o.size())
                            .build());
                }

                if (!response.isTruncated()) break;

                continuationToken = response.nextContinuationToken();
            }

            return result;
        } catch (S3Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while searching resource");
        }
    }

    @Override
    public ResourceResponseDTO moveResource(MoveResourceRequestDTO request) {
        User user = request.getUser();
        String from = request.getFrom();
        String to = request.getTo();

        from = addUserPrefix(user.getId(), from);
        to = addUserPrefix(user.getId(), to);

        try {
            s3.headObject(HeadObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(from)
                    .build());
        } catch (NoSuchKeyException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found: " + request.getFrom());
        }

        try {
            s3.headObject(HeadObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(to)
                    .build());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Destination already exists: " + request.getTo());
        } catch (NoSuchKeyException e) {

        }

        System.out.println("FileServiceImpl: moveResource: from = " + from + " to = " + to);

        try {
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
                    .path(extractPathFromKey(to))
                    .name(extractNameFromKey(to))
                    .resourceType(ResourceType.FILE)
                    .size(size)
                    .build();
        } catch (S3Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while moving resource");
        }
    }

    @Override
    public DownloadResourceResponseDTO download(ResourceRequestDTO request) {
        User user = request.getUser();
        String path = request.getPath();
        String key = addUserPrefix(user.getId(), path);

        byte [] data;
        String filename = extractNameFromKey(key);
        ResourceType type = ResourceType.FILE;
        if (isDirectory(key)) {
            filename += ".zip";
            data = zipDirectory(key);
            type = ResourceType.DIRECTORY;
        } else {
            data = downloadFile(key);
        }

        return DownloadResourceResponseDTO.builder()
                .filename(filename)
                .data(data)
                .resourceType(type)
                .build();
    }

    private byte[] downloadFile(String key) {
        System.out.println("FileServiceImpl: downloadFile: key = " + key);

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(key)
                .build();

        try (ResponseInputStream<GetObjectResponse> response = s3.getObject(getObjectRequest)) {
            return response.readAllBytes();
        } catch (NoSuchKeyException e) {
            throw new ResourceNotFound(e.getMessage());
        } catch (IOException | S3Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while downloading file");
        }
    }

    private byte[] zipDirectory(String key) {
        System.out.println("FileServiceImpl: downloadDirectory: key = " + key);

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
        } catch (NoSuchKeyException e) {
            throw new ResourceNotFound(e.getMessage());
        } catch (S3Exception | IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while downloading file");
        }
    }

    private boolean isDirectory(String key) {
        try {
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
        } catch (NoSuchKeyException e) {
            throw new ResourceNotFound(e.getMessage());
        } catch (S3Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while downloading resource");
        }
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

    private String removeNameFormPath(String path) {
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        if (!path.contains("/")) {
            return "/"; // это root
        }

        int lastSlash = path.lastIndexOf('/');
        return path.substring(0, lastSlash + 1);
    }

    private String removePrefixFromKey(Long userId, String key) {
        String prefix = String.format(pathPrefixFormat, userId);
        return key.substring(prefix.length());
    }
}
