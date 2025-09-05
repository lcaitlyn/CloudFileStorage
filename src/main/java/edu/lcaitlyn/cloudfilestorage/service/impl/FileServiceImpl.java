package edu.lcaitlyn.cloudfilestorage.service.impl;

import edu.lcaitlyn.cloudfilestorage.DTO.S3ObjectMetadata;
import edu.lcaitlyn.cloudfilestorage.DTO.request.MoveResourceRequestDTO;
import edu.lcaitlyn.cloudfilestorage.DTO.request.ResourceRequestDTO;
import edu.lcaitlyn.cloudfilestorage.DTO.response.DownloadResourceResponseDTO;
import edu.lcaitlyn.cloudfilestorage.DTO.response.ResourceResponseDTO;
import edu.lcaitlyn.cloudfilestorage.enums.Type;
import edu.lcaitlyn.cloudfilestorage.exception.DirectoryNotFound;
import edu.lcaitlyn.cloudfilestorage.exception.ResourceNotFound;
import edu.lcaitlyn.cloudfilestorage.models.User;
import edu.lcaitlyn.cloudfilestorage.service.FileService;
import edu.lcaitlyn.cloudfilestorage.service.S3Service;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

// todo     Починить path. Вообще какая-то хуевая логика у тебя по сути
@Slf4j
@Service
@AllArgsConstructor
public class FileServiceImpl implements FileService {

    private final String pathPrefixFormat = "user-%d-files";

    private final S3Service s3Service;

    @Override
    public ResourceResponseDTO getResource(ResourceRequestDTO request) {
        User user = request.getUser();
        String path = request.getPath();
        String key = addUserPrefix(user.getId(), path);

        try {
            log.info("FileServiceImpl: getFile: path = {} key = {}", path, key);
            S3ObjectMetadata response = s3Service.getObject(key);

            Type type = s3Service.isDirectory(key) ? Type.DIRECTORY : Type.FILE;

            return ResourceResponseDTO.builder()
                    .path(extractPathFromKey(key))
                    .name(extractNameFromKey(key))
                    .size(response.getContentLength())
                    .type(type)
                    .build();
        } catch (NoSuchKeyException e) {
            throw new ResourceNotFound(extractNameFromKey(key));
        } catch (S3Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while getting resource", e);
        }
    }

    // todo Сделать чтобы при одинаковых названиях файлов, все четенько работало, если нет 409
    //                                      (11.06 обновил)
    @Override
    public List<ResourceResponseDTO> uploadFile(ResourceRequestDTO request) {
        User user = request.getUser();
        String path = request.getPath();
        MultipartFile[] files = request.getFiles();

        String prefix = addUserPrefix(user.getId(), path);

        try {
            for (MultipartFile file : files) {
                String key = prefix + file.getOriginalFilename();
                if (s3Service.isResourceExists(key)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "File '" + file.getOriginalFilename() + "' is already exists.");
                }
            }

            createDirectoriesIfNeeded(prefix);

            List<ResourceResponseDTO> response = new ArrayList<>();

            for (MultipartFile file : files) {
                String key = prefix + file.getOriginalFilename();

                try {
                    log.info("FileServiceImpl: uploadFile: key = {}{}", prefix, file.getOriginalFilename());
                    s3Service.putObject(key, S3ObjectMetadata.builder()
                            .contentType(file.getContentType())
                            .data(file.getBytes())
                            .build()
                    );
                } catch (IOException e) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Failed to upload file. Error with getting bytes.");
                }

                response.add(ResourceResponseDTO.builder()
                        .path(path)
                        .name(file.getOriginalFilename())
                        .size(file.getSize())
                        .type(Type.FILE)
                        .build()
                );
            }
            return response;
        } catch (S3Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while uploading file");
        }
    }

    @Override
    public List<ResourceResponseDTO> getDirectory(ResourceRequestDTO request) {
        User user = request.getUser();
        String path = request.getPath();

        String prefix = addUserPrefix(user.getId(), path);

        if (!s3Service.isDirectory(prefix)) {
            if (path.isEmpty() || path.equals("/")) {
                createRootDirectory(request);
            } else {
                throw new DirectoryNotFound(extractNameFromKey(path));
            }
        }

        try {
            List<ResourceResponseDTO> response = new ArrayList<>();

            log.info("FileServiceImpl: getDirectory: path = {}, prefix = {}", path, prefix);
            List<String> directories = s3Service.listDirectories(prefix);
            for (String directory : directories) {
                String relativePath = directory.substring(prefix.length());

                response.add(ResourceResponseDTO.builder()
                        .name(relativePath)
                        .path(path)
                        .type(Type.DIRECTORY)
                        .build()
                );
            }

            List<S3Object> files = s3Service.listObjects(prefix);
            for (S3Object file : files) {
                String fullKey = file.key();
                if (fullKey.equals(prefix)) continue;

                String relativePath = fullKey.substring(prefix.length());
                if (relativePath.contains("/")) continue;

                response.add(ResourceResponseDTO.builder()
                        .name(relativePath)
                        .path(path)
                        .size(file.size())
                        .type(Type.FILE)
                        .build()
                );
            }

            return response;
        } catch (S3Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while getting objects");
        }
    }

    @Override
    public ResourceResponseDTO createDirectory(ResourceRequestDTO request) {
        User user = request.getUser();
        String path = request.getPath();

        String prefix = addUserPrefix(user.getId(), path);

        if (s3Service.isResourceExists(prefix)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Directory '" + path + "' is already exists.");
        }

        try {
            createDirectoriesIfNeeded(prefix);

            return ResourceResponseDTO.builder()
                    .path(removeNameFormPath(path))
                    .name(extractNameFromKey(path) + "/")
                    .type(Type.DIRECTORY)
                    .build();
        } catch (S3Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create directory: " + path);
        }
    }

    private void createDirectoriesIfNeeded(String key) {
        if (!key.endsWith("/")) {
            key += "/";
        }

        key = key.substring(0, key.length() - 1);

        Deque<String> stack = new ArrayDeque<>();

        while (key.contains("/") && !s3Service.isResourceExists(key + "/")) {
            stack.push(key + "/");
            key = key.substring(0, key.lastIndexOf("/"));
        }

        while (!stack.isEmpty()) {
            String dirToCreate = stack.pop();
            log.info("FileServiceImpl: createDirectory: prefix = {}", dirToCreate);
            s3Service.createDirectory(dirToCreate);
        }
    }

    @Override
    public void createRootDirectory(ResourceRequestDTO request) {
        User user = request.getUser();

        String prefix = addUserPrefix(user.getId(), "/");

        try {
            log.info("FileServiceImpl: createRootDirectory: prefix = {}", prefix);
            s3Service.createDirectory(prefix);
        } catch (S3Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create directory: " + prefix);
        }
    }

    @Override
    public void deleteResource(ResourceRequestDTO request) {
        User user = request.getUser();
        String path = request.getPath();
        String prefix = addUserPrefix(user.getId(), path);

        try {
            if (prefix.endsWith("/")) {
                boolean isDir = s3Service.isDirectory(prefix);
                if (!isDir) {
                    throw new DirectoryNotFound(extractNameFromKey(path));
                }

                log.info("FileServiceImpl: deleteDirectory: path = {}, prefix = {}", path, prefix);
                s3Service.deleteDirectory(prefix);
            } else {
                if (!s3Service.isFileExist(prefix)) {
                    throw new ResourceNotFound(extractNameFromKey(path));
                }

                boolean isDir = s3Service.isDirectory(prefix);
                if (isDir) {
                    log.info("FileServiceImpl: deleteDirectory (no trailing slash): path = {}, prefix = {}", path, prefix);
                    s3Service.deleteDirectory(prefix);
                } else {
                    log.info("FileServiceImpl: deleteFile: path = {}, prefix = {}", path, prefix);
                    s3Service.deleteObject(prefix);
                }
            }
        } catch (S3Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while deleting resource");
        }
    }

    @Override
    public List<ResourceResponseDTO> findResource(ResourceRequestDTO request) {
        User user = request.getUser();
        String query = request.getPath().toLowerCase();
        String prefix = addUserPrefix(user.getId(), "/");

        List<ResourceResponseDTO> response = new ArrayList<>();
        String continuationToken = null;

        try {
            while (true) {
                log.info("FileServiceImpl: findResource: path = {}, query = {}", prefix, query);
                ListObjectsV2Response objectv2Response = s3Service.listAllObjects(prefix, continuationToken);

                for (CommonPrefix cp : objectv2Response.commonPrefixes()) {
                    String key = cp.prefix();
                    String name = extractNameFromKey(key);

                    if (!name.toLowerCase().contains(query)) continue;

                    response.add(ResourceResponseDTO.builder()
                            .path(extractPathFromKey(key))
                            .name(name)
                            .type(Type.DIRECTORY)
                            .build());
                }

                for (S3Object o : objectv2Response.contents()) {
                    String key = o.key();
                    String name = extractNameFromKey(key);

                    if (!name.toLowerCase().contains(query)) continue;

                    ResourceResponseDTO.ResourceResponseDTOBuilder builder = ResourceResponseDTO.builder()
                            .path(extractPathFromKey(key))
                            .name(name);

                    if (key.endsWith("/") && o.size() == 0) {
                        builder.type(Type.DIRECTORY);
                    } else {
                        builder.type(Type.FILE)
                                .size(o.size());
                    }

                    response.add(builder.build());
                }

                if (!objectv2Response.isTruncated()) break;
                continuationToken = objectv2Response.nextContinuationToken();
            }

            return response;
        } catch (S3Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while getting objects");
        }
    }


    // todo:    Сделать чтобы работало с папками
    @Override
    public ResourceResponseDTO moveResource(MoveResourceRequestDTO request) {
        User user = request.getUser();
        String from = request.getFrom();
        String to = request.getTo();

        from = addUserPrefix(user.getId(), from);
        to = addUserPrefix(user.getId(), to);

        if (from.endsWith("/") || to.endsWith("/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Should not ends with '/': from =" + request.getFrom() + ", to = " + request.getTo());
        }

        if (!s3Service.isFileExist(from)) {
            throw new ResourceNotFound(request.getFrom());
        }

        if (s3Service.isResourceExists(to)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Destination is already exists: " + request.getTo());
        }

        try {
            createDirectoriesIfNeeded(removeNameFormPath(to));

            log.info("FileServiceImpl: moveResource.copy: from = {} to = {}", from, to);
            s3Service.copyObject(from, to);

            log.info("FileServiceImpl: moveResource.delete: from = {} to = {}", from, to);
            s3Service.deleteObject(from);

            Long size = s3Service.getObject(to).getContentLength();

            return ResourceResponseDTO.builder()
                    .path(extractPathFromKey(to))
                    .name(extractNameFromKey(to))
                    .type(Type.FILE)
                    .size(size)
                    .build();
        } catch (S3Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while moving resource");
        }
    }

    @Override
    public DownloadResourceResponseDTO downloadResource(ResourceRequestDTO request) {
        User user = request.getUser();
        String path = request.getPath();
        String key = addUserPrefix(user.getId(), path);

        S3ObjectMetadata metadata;
        String filename = extractNameFromKey(key);
        if (filename.isEmpty()) {
            filename = user.getUsername();
        }
        Type type = Type.FILE;

        try {
            if (s3Service.isDirectory(key)) {
                if (!key.endsWith("/")) {
                    key += '/';
                }
                log.info("FileServiceImpl: downloadDirectory: key = {}", key);
                filename += ".zip";
                metadata = s3Service.downloadDirectory(key);
                type = Type.DIRECTORY;
            } else {
                log.info("FileServiceImpl: downloadFile: key = {}", key);
                metadata = s3Service.downloadObject(key);
            }

            return DownloadResourceResponseDTO.builder()
                    .filename(filename)
                    .data(metadata.getData())
                    .type(type)
                    .build();
        } catch (NoSuchKeyException e) {
            throw new ResourceNotFound(path);
        } catch (S3Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while downloading file");
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
        key = key.substring(key.indexOf("/") + 1);

        if (key.endsWith("/")) {
            key = key.substring(0, key.length() - 1);
        }

        int lastSlash = key.lastIndexOf('/');
        String rawPath = key.substring(0, lastSlash + 1);

        return rawPath.isEmpty() ? "/" : "/" + rawPath;
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
}
