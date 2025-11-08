package edu.lcaitlyn.cloudfilestorage.service.impl;

import edu.lcaitlyn.cloudfilestorage.DTO.DownloadResourceDTO;
import edu.lcaitlyn.cloudfilestorage.DTO.ResourceMetadata;
import edu.lcaitlyn.cloudfilestorage.DTO.StorageDTO;
import edu.lcaitlyn.cloudfilestorage.DTO.request.MoveResourceRequestDTO;
import edu.lcaitlyn.cloudfilestorage.DTO.request.ResourceRequestDTO;
import edu.lcaitlyn.cloudfilestorage.DTO.response.ResourceResponseDTO;
import edu.lcaitlyn.cloudfilestorage.enums.Type;
import edu.lcaitlyn.cloudfilestorage.exception.DirectoryNotFound;
import edu.lcaitlyn.cloudfilestorage.exception.FileServiceException;
import edu.lcaitlyn.cloudfilestorage.exception.ResourceAlreadyExists;
import edu.lcaitlyn.cloudfilestorage.exception.ResourceNotFound;
import edu.lcaitlyn.cloudfilestorage.service.FileService;
import edu.lcaitlyn.cloudfilestorage.service.StorageManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static edu.lcaitlyn.cloudfilestorage.utils.FileServiceUtils.*;

// todo     Починить path. Вообще какая-то хуевая логика у тебя по сути

// todo     Починить одинаковые имена (если папка 123 уже есть и ты загружаешь файл 123, то он загрузиться). Исправить в upload в move

// todo     Имена папок с . не работают (типо не могу создать папку с название 1.txt)
@Slf4j
@Service
@AllArgsConstructor
public class FileServiceImpl implements FileService {

    public static final String KEY_PREFIX = "user-%d-files";

    private final StorageManager storageManager;

    @Override
    public ResourceResponseDTO getResource(ResourceRequestDTO request) {
        String key = createKey(request.getUser().getId(), request.getPath());

        StorageDTO resource = storageManager.getResourceMetadata(key);

        if (resource == null) {
            log.warn("User [{}] unsuccessfully get resource with path = {}. Resource not found", request.getUser().getUsername(), request.getPath());
            throw new ResourceNotFound(extractNameFromKey(key));
        }

        return ResourceResponseDTO.builder()
                .path(extractPathFromKey(key))
                .name(extractNameFromKey(key))
                .size(resource.getSize())
                .type(resource.getType())
                .build();
    }

    // todo Сделать чтобы при одинаковых названиях файлов, все четенько работало, если нет 409
    //                                      (11.06 обновил)
    @Override
    public List<ResourceResponseDTO> uploadFile(ResourceRequestDTO request) {
        String prefix = createKey(request.getUser().getId(), request.getPath());
        MultipartFile[] files = request.getFiles();

        for (MultipartFile file : files) {
            String key = prefix + file.getOriginalFilename();
            if (storageManager.exists(key) || storageManager.isDirectory(key + "/")) {
                log.warn("User [{}] unsuccessfully uploaded resource with path = {}: resource already exists.", request.getUser().getUsername(), request.getPath());
                throw new ResourceAlreadyExists(file.getOriginalFilename());
            }
        }

        List<ResourceResponseDTO> response = new ArrayList<>();
        for (MultipartFile file : files) {
            String key = prefix + file.getOriginalFilename();
            try {
                storageManager.save(key, file.getBytes(), file.getContentType());
                log.info("User [{}] uploaded file: {}", request.getUser().getUsername(), request.getPath() + file.getOriginalFilename());

                StorageDTO resource = storageManager.getResourceMetadata(key);
                response.add(ResourceResponseDTO.builder()
                        .path(request.getPath())
                        .name(resource.getKey().substring(prefix.length()))
                        .size(resource.getSize())
                        .type(resource.getType())
                        .build()
                );
            } catch (IOException e) {
                log.error("User [{}] unsuccessfully uploaded resource: {}. IOException : {}", request.getUser().getUsername(), request.getPath() + file.getOriginalFilename(), e.getMessage());
                throw new FileServiceException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload file '" + file.getOriginalFilename() + "'. Error reading bytes from input.");
            }
        }
        return response;
    }

    @Override
    public List<ResourceResponseDTO> getDirectory(ResourceRequestDTO request) {
        String prefix = createKey(request.getUser().getId(), request.getPath());

        if (!storageManager.exists(prefix) || !storageManager.isDirectory(prefix)) {
            log.warn("User [{}] unsuccessfully got directory with path = {}. Directory not found", request.getUser().getUsername(), request.getPath());
            throw new DirectoryNotFound(extractNameFromKey(request.getPath()));
        }

        List<StorageDTO> resources = storageManager.getDirectory(prefix);

        return resources.stream().map(r -> ResourceResponseDTO.builder()
                .name(r.getKey().substring(prefix.length()))
                .path(request.getPath())
                .size(r.getSize())
                .type(r.getType())
                .build()
        ).toList();
    }

    @Override
    public ResourceResponseDTO createDirectory(ResourceRequestDTO request) {
        String prefix = createKey(request.getUser().getId(), request.getPath());

        if (storageManager.exists(prefix)) {
            log.warn("User [{}] unsuccessfully created directory with path = {}. Directory already exists", request.getUser().getUsername(), request.getPath());
            throw new ResourceAlreadyExists(extractNameFromKey(prefix));
        }

        if (request.getPath().length() != 1 && storageManager.exists(prefix.substring(0, prefix.length() - 1))) {
            log.warn("User [{}] unsuccessfully created directory with path = {}. Directory already exists", request.getUser().getUsername(), request.getPath());
            throw new ResourceAlreadyExists(extractNameFromKey(prefix));
        }

        storageManager.createDirectory(prefix);
        log.info("User [{}] created directory: {}", request.getUser().getUsername(), request.getPath());
        return getResource(request);
    }

    @Override
    public void deleteResource(ResourceRequestDTO request) {
        String prefix = createKey(request.getUser().getId(), request.getPath());

        if (!storageManager.exists(prefix)) {
            log.warn("User [{}] unsuccessfully deleted resource = {}. Resource not found", request.getUser().getUsername(), request.getPath());
            throw new ResourceNotFound(extractNameFromKey(request.getPath()));
        }

        if (isRootPath(request.getPath())) {
            log.warn("User [{}] unsuccessfully deleted resource = {}. Can not delete root directory", request.getUser().getUsername(), request.getPath());
            throw new FileServiceException("Can not delete root directory");
        }

        storageManager.delete(prefix);
        log.info("User [{}] deleted resource: {}", request.getUser().getUsername(), request.getPath());
    }

    @Override
    public List<ResourceResponseDTO> findResource(ResourceRequestDTO request, String query) {
        String prefix = createKey(request.getUser().getId(), "/");
        List<ResourceResponseDTO> response = new ArrayList<>();

        List<StorageDTO> resources = storageManager.getFullDirectory(prefix);
        if (resources == null) {
            log.warn("User [{}] unsuccessfully found resource: {} ; path = {}. Problem with retrieving root directory.", request.getUser().getUsername(), query, request.getPath());
            throw new FileServiceException("Problem with retrieving root directory");
        }

        for (StorageDTO r : resources) {
            String filename = extractNameFromKey(r.getKey());

            if (filename.contains(query)) {
                response.add(ResourceResponseDTO.builder()
                        .path(extractPathFromKey(r.getKey()))
                        .name(extractNameFromKey(r.getKey()))
                        .size(r.getSize())
                        .type(r.getType())
                        .build());
            }
        }

        return response;
    }


    // todo сделать проверку на повторение файлов. допустим есть файл 123 и в эту папку прилетит новая папка 123/. Сделать исключение
    // todo сделать проверку на то что родительскую нельзя закинуть в дочернюю
    @Override
    public ResourceResponseDTO moveResource(MoveResourceRequestDTO request) {
        if (isRootPath(request.getFrom()) || isRootPath(request.getTo())) {
            log.warn("User [{}] unsuccessfully moved resource: from = {} to = {}. Can not delete root directory", request.getUser().getUsername(), request.getTo(), request.getFrom());
            throw new FileServiceException("Can not move root directory");
        }

        String from = createKey(request.getUser().getId(), request.getFrom());
        String to = createKey(request.getUser().getId(), request.getTo());

        if (!storageManager.exists(from)) {
            log.warn("User [{}] unsuccessfully moved resource: from = {} to = {}. Source resource is not exists", request.getUser().getUsername(), request.getTo(), request.getFrom());
            throw new ResourceNotFound(extractNameFromKey(request.getFrom()));
        }

        if (storageManager.exists(to)) {
            log.warn("User [{}] unsuccessfully moved resource: from = {} to = {}. Target resource is already exists", request.getUser().getUsername(), request.getTo(), request.getFrom());
            throw new ResourceAlreadyExists(extractNameFromKey(request.getTo()));
        }


        storageManager.move(from, to);
        log.info("User [{}] moved resource: from = {}; to = {}", request.getUser().getUsername(), request.getFrom(), request.getTo());
        StorageDTO resource = storageManager.getResourceMetadata(to);

        if (resource == null) {
            log.warn("User [{}] unsuccessfully get resource with path = {}. Resource not found", request.getUser().getUsername(), request.getTo());
            throw new ResourceNotFound(extractNameFromKey(request.getTo()));
        }

        return ResourceResponseDTO.builder()
                .name(extractNameFromKey(resource.getKey()))
                .path(extractPathFromKey(resource.getKey()))
                .size(resource.getSize())
                .type(resource.getType())
                .build();
    }

    @Override
    public DownloadResourceDTO downloadResource(ResourceRequestDTO request) {
        String key = createKey(request.getUser().getId(), request.getPath());

        StorageDTO resource = storageManager.getResourceMetadata(key);

        if (resource == null) {
            log.warn("User [{}] unsuccessfully get resource with path = {}. Resource not found", request.getUser().getUsername(), request.getPath());
            throw new ResourceNotFound(extractNameFromKey(request.getPath()));
        }

        ResourceMetadata metadata;
        String filename = createDownloadName(request.getPath(), request.getUser().getUsername(), resource.getType());

        if (resource.getType() == Type.DIRECTORY) {
            metadata = storageManager.downloadFolder(resource.getKey(), filename);
            filename += ".zip";
            log.info("User [{}] downloaded directory: {}", request.getUser().getUsername(), request.getPath() + filename);
        } else {
            metadata = storageManager.downloadFile(key);
            log.info("User [{}}] downloaded file: {}", request.getUser().getUsername(), request.getPath() + filename);
        }

        return DownloadResourceDTO.builder()
                .filename(filename)
                .type(resource.getType())
                .data(metadata.getData())
                .type(resource.getType())
                .build();
    }

    private String createDownloadName(String path, String username, Type type) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        if (path.isEmpty()) {
            return username;
        }

        if (type == Type.DIRECTORY) {
            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            return path.substring(path.lastIndexOf("/") + 1);
        }

        return path.substring(path.lastIndexOf("/") + 1);
    }
}
