package edu.lcaitlyn.cloudfilestorage.controller;

import edu.lcaitlyn.cloudfilestorage.DTO.response.DownloadResourceResponseDTO;
import edu.lcaitlyn.cloudfilestorage.DTO.request.MoveResourceRequestDTO;
import edu.lcaitlyn.cloudfilestorage.DTO.response.ResourceResponseDTO;
import edu.lcaitlyn.cloudfilestorage.DTO.request.ResourceRequestDTO;
import edu.lcaitlyn.cloudfilestorage.models.AuthUserDetails;
import edu.lcaitlyn.cloudfilestorage.service.FileService;
import edu.lcaitlyn.cloudfilestorage.utils.ErrorResponseUtils;
import edu.lcaitlyn.cloudfilestorage.utils.PathValidationUtils;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


// todo обернуть каждый метод в try catch как сделал в download
@RestController
@RequestMapping("/resource")
@AllArgsConstructor
public class ResourceController {

    private final FileService fileService;

    @GetMapping
    public ResponseEntity<?> getResource(
            @RequestParam String path,
            @AuthenticationPrincipal AuthUserDetails userDetails) {
        path = PathValidationUtils.validateResourcePath(path);

        ResourceResponseDTO responseDTO = fileService.getFile(
                ResourceRequestDTO.builder()
                        .user(userDetails.getUser())
                        .path(path)
                        .build());

        return new ResponseEntity<>(responseDTO, HttpStatus.OK);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadResource(
            @RequestPart("files") MultipartFile[] files,
            @RequestParam String path,
            @AuthenticationPrincipal AuthUserDetails userDetails) {
        path = PathValidationUtils.validateDirectoryPath(path);

        List<ResourceResponseDTO> responseDTO = fileService.uploadFile(ResourceRequestDTO.builder()
                .files(files)
                .user(userDetails.getUser())
                .path(path)
                .build()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    @DeleteMapping
    public ResponseEntity<?> deleteResource(
            @RequestParam String path,
            @AuthenticationPrincipal AuthUserDetails userDetails) {
        path = PathValidationUtils.validateResourcePath(path);

        fileService.deleteResourceOrDirectory(ResourceRequestDTO.builder()
                .path(path)
                .user(userDetails.getUser())
                .build()
        );

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchResource(
            @RequestParam String query,
            @AuthenticationPrincipal AuthUserDetails userDetails) {
        if (query == null || query.isEmpty()) {
            return ErrorResponseUtils.print("Query string is empty", HttpStatus.BAD_REQUEST);
        }

        List<ResourceResponseDTO> response = fileService.findResource(ResourceRequestDTO.builder()
                .user(userDetails.getUser())
                .path(query)
                .build());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/move")
    public ResponseEntity<?> moveResource(
            @RequestParam String from,
            @RequestParam String to,
            @AuthenticationPrincipal AuthUserDetails userDetails) {
        from = PathValidationUtils.validateResourcePath(from);
        to = PathValidationUtils.validateResourcePath(to);

        ResourceResponseDTO response = fileService.moveResource(MoveResourceRequestDTO.builder()
                .from(from)
                .to(to)
                .user(userDetails.getUser())
                .build());

        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/download")
    public ResponseEntity<?> downloadResource(
            @RequestParam String path,
            @AuthenticationPrincipal AuthUserDetails userDetails) {
        path = PathValidationUtils.validateResourcePath(path);

        DownloadResourceResponseDTO response = fileService.download(ResourceRequestDTO.builder()
                .user(userDetails.getUser())
                .path(path)
                .build()
        );

        String contentDisposition = "attachment; filename=\"" + response.getFilename() + "\"";

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(response.getData());
    }
}

// todo читай ниже ебать. Тут настоящий туду
// ну вообще ебать хуйня какая-то получилась. конечно работает, но с костылями.
// Из задач: 1. Переделать нахуй имена файлов и папок ( мб заюзай substring(key.length()) )
// Контроллер мне нахуй не нравятся.
// Еще эти ебаные User проверки, которые везде проверятся. Мб вынести нахуй в функцию и возвращать только User
// Вообще по моему контроллеру должно быть похуй над логикой. Он должен проверить логин (хотя это проверяет SpringSecuriy)
// Т.е если нам прилетает path, то контроллер не должен валидировать его. Это все должно делаться в логике нахуй
// В Сервисе уже должно все проверятся и высылаться обратно ебаный exception или какой-нибудь хуевый респонс.
// Т.е контроллеру должно быть похуй, он только для того чтобы размечать (get/post mapping крч)
// Ну и собственно try/catch должны быть везде чтобы говна не случалась. И конечно же это должно быть в Сервисе

// А так же сделать ебать интеграцию с фронтом

// убрать все sout логгеры

// вынести нахуй логику S3 в отдельный класс

// переделать аунтификацию
