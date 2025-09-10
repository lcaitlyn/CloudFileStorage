package edu.lcaitlyn.cloudfilestorage.service.impl;

import edu.lcaitlyn.cloudfilestorage.DTO.DownloadResourceDTO;
import edu.lcaitlyn.cloudfilestorage.DTO.ResourceMetadata;
import edu.lcaitlyn.cloudfilestorage.DTO.StorageDTO;
import edu.lcaitlyn.cloudfilestorage.DTO.request.MoveResourceRequestDTO;
import edu.lcaitlyn.cloudfilestorage.DTO.request.ResourceRequestDTO;
import edu.lcaitlyn.cloudfilestorage.DTO.response.ResourceResponseDTO;
import edu.lcaitlyn.cloudfilestorage.enums.Type;
import edu.lcaitlyn.cloudfilestorage.exception.ResourceAlreadyExists;
import edu.lcaitlyn.cloudfilestorage.exception.ResourceNotFound;
import edu.lcaitlyn.cloudfilestorage.repository.S3Repository;
import edu.lcaitlyn.cloudfilestorage.service.FileService;
import edu.lcaitlyn.cloudfilestorage.service.StorageManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// todo     Починить path. Вообще какая-то хуевая логика у тебя по сути

// todo     Починить одинаковые имена (если папка 123 уже есть и ты загружаешь файл 123, то он загрузиться). Исправить в upload в move

// todo     Сделать нормальное логирование ошибок. Тип кто че замутил и хули пошло не так
@Slf4j
@Service
@AllArgsConstructor
public class FileServiceImpl implements FileService {

    private final String KEY_PREFIX = "user-%d-files";

    private final StorageManager storageManager;

    @Override
    public ResourceResponseDTO getResource(ResourceRequestDTO request) {
        String key = createKey(request.getUser().getId(), request.getPath());

        StorageDTO resource = storageManager.getResourceMetadata(key);

        if (resource == null) {
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
    // todo Затестить будет ли он загружать папку в которой есть другие папки
    @Override
    public List<ResourceResponseDTO> uploadFile(ResourceRequestDTO request) {
        String prefix = createKey(request.getUser().getId(), request.getPath());
        MultipartFile[] files = request.getFiles();

        for (MultipartFile file : files) {
            String key = prefix + file.getOriginalFilename();
            if (storageManager.exists(key) || storageManager.isDirectory(key + "/")) {
                throw new ResourceAlreadyExists(file.getOriginalFilename());
            }
        }

        List<ResourceResponseDTO> response = new ArrayList<>();
        for (MultipartFile file : files) {
            String key = prefix + file.getOriginalFilename();
            try {
                storageManager.save(key, file.getBytes(), file.getContentType());
                log.info("User [" + request.getUser().getUsername() + "] uploaded file " + request.getPath() + file.getOriginalFilename());

                StorageDTO resource = storageManager.getResourceMetadata(key);
                response.add(ResourceResponseDTO.builder()
                        .path(request.getPath())
                        .name(resource.getKey().substring(prefix.length()))
                        .size(resource.getSize())
                        .type(resource.getType())
                        .build()
                );
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload file '" + file.getOriginalFilename() + "'. Error reading bytes from input.");
            }
        }
        return response;
    }

    @Override
    public List<ResourceResponseDTO> getDirectory(ResourceRequestDTO request) {
        String prefix = createKey(request.getUser().getId(), request.getPath());

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
            throw new ResourceAlreadyExists(extractNameFromKey(prefix));
        }

        if (request.getPath().length() != 1 && storageManager.exists(prefix.substring(0, prefix.length() - 1))) {
            throw new ResourceAlreadyExists(extractNameFromKey(prefix));
        }

        storageManager.createDirectory(prefix);
        return getResource(request);
    }

    // todo Сделать проверку на то что нельзя удалять root
    @Override
    public void deleteResource(ResourceRequestDTO request) {
        String prefix = createKey(request.getUser().getId(), request.getPath());

        if (!storageManager.exists(prefix)) {
            throw new ResourceNotFound(extractNameFromKey(request.getPath()));
        }
        storageManager.delete(prefix);
        log.info("User [" + request.getUser().getUsername() + "] deleted file " + request.getPath());
    }

    @Override
    public List<ResourceResponseDTO> findResource(ResourceRequestDTO request, String query) {
        String prefix = createKey(request.getUser().getId(), "/");
        List<ResourceResponseDTO> response = new ArrayList<>();

        List<StorageDTO> resources = storageManager.getFullDirectory(prefix);
        for (StorageDTO r : resources) {
            if (r.getKey().substring(prefix.length()).contains(query)) {
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

    // todo:    Сделать чтобы не работала с root
    @Override
    public ResourceResponseDTO moveResource(MoveResourceRequestDTO request) {
        String from = createKey(request.getUser().getId(), request.getFrom());
        String to = createKey(request.getUser().getId(), request.getTo());

        if (!storageManager.exists(from)) {
            throw new ResourceNotFound(extractNameFromKey(request.getFrom()));
        }

        if (storageManager.exists(to)) {
            throw new ResourceAlreadyExists(extractNameFromKey(request.getTo()));
        }

        storageManager.move(from, to);
        StorageDTO resource = storageManager.getResourceMetadata(to);

        if (resource == null) {
            throw new ResourceNotFound(extractNameFromKey(request.getTo()));
        }

        return ResourceResponseDTO.builder()
                .name(extractNameFromKey(resource.getKey()))
                .path(extractPathFromKey(resource.getKey()))
                .size(resource.getSize())
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

    @Override
    public DownloadResourceDTO downloadResource(ResourceRequestDTO request) {
        String key = createKey(request.getUser().getId(), request.getPath());

        StorageDTO resource = storageManager.getResourceMetadata(key);

        if (resource == null) {
            throw new ResourceNotFound(extractNameFromKey(request.getPath()));
        }

        ResourceMetadata metadata;
        String filename = createDownloadName(request.getPath(), request.getUser().getUsername(), resource.getType());

        if (resource.getType() == Type.DIRECTORY) {
            metadata = storageManager.downloadFolder(resource.getKey(), filename);
            filename += ".zip";
            log.info("User [" + request.getUser().getUsername() + "] downloaded directory " + request.getPath() + filename);
        } else {
            metadata = storageManager.downloadFile(key);
            log.info("User [" + request.getUser().getUsername() + "] downloaded file " + request.getPath() + filename);
        }

        return DownloadResourceDTO.builder()
                .filename(filename)
                .type(resource.getType())
                .data(metadata.getData())
                .type(resource.getType())
                .build();
    }


    // todo снести в UtilsClass
    private String createKey(Long userId, String path) {
        String prefix = String.format(KEY_PREFIX, userId);
        return prefix + path;
    }

    // todo снести в UtilsClass
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

    // todo снести в UtilsClass
    private String extractPathFromKey(String key) {
        key = key.substring(key.indexOf("/") + 1);

        if (key.endsWith("/")) {
            key = key.substring(0, key.length() - 1);
        }

        int lastSlash = key.lastIndexOf('/');
        String rawPath = key.substring(0, lastSlash + 1);

        return rawPath.isEmpty() ? "/" : "/" + rawPath;
    }

    // todo снести в UtilsClass
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
