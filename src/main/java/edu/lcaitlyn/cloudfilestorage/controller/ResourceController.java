package edu.lcaitlyn.cloudfilestorage.controller;

import edu.lcaitlyn.cloudfilestorage.DTO.MoveResourceRequestDTO;
import edu.lcaitlyn.cloudfilestorage.DTO.ResourceResponseDTO;
import edu.lcaitlyn.cloudfilestorage.DTO.ResourceRequestDTO;
import edu.lcaitlyn.cloudfilestorage.models.User;
import edu.lcaitlyn.cloudfilestorage.service.FileService;
import edu.lcaitlyn.cloudfilestorage.service.UserService;
import edu.lcaitlyn.cloudfilestorage.utils.ErrorResponseUtils;
import edu.lcaitlyn.cloudfilestorage.utils.PathValidationUtils;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


// todo обернуть каждый метод в try catch как сделал в download
@RestController
@RequestMapping("/resource")
@AllArgsConstructor
public class ResourceController {

    private final FileService fileService;

    private final UserService userService;

    @GetMapping
    public ResponseEntity<?> getResource(
            @RequestParam String path,
            @AuthenticationPrincipal UserDetails userDetails) throws FileNotFoundException {
        if (userDetails == null) {
            return ErrorResponseUtils.print("User not logged in", HttpStatus.UNAUTHORIZED);
        }

        path = PathValidationUtils.validateResourcePath(path);

        Optional<User> user = userService.findByUsername(userDetails.getUsername());
        if (user.isEmpty()) {
            return ErrorResponseUtils.print("User not found", HttpStatus.NOT_FOUND);
        }

        try {
            ResourceResponseDTO responseDTO = fileService.getFile(ResourceRequestDTO.builder()
                    .user(user.get())
                    .path(path)
                    .build());

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (NoSuchKeyException e) {
            return ErrorResponseUtils.print("File " + path + " not found", HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadResource(
            @RequestPart("files") MultipartFile[] files,
            @RequestParam String path,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {

        if (userDetails == null) {
            return ErrorResponseUtils.print("User not logged in", HttpStatus.UNAUTHORIZED);
        }

        path = PathValidationUtils.validateDirectoryPath(path);

        // todo переделать это чтобы он отнимал куки, если user не найден
        Optional<User> user = userService.findByUsername(userDetails.getUsername());
        if (user.isEmpty()) {
            return ErrorResponseUtils.print("User not found", HttpStatus.NOT_FOUND);
        }

        List<ResourceResponseDTO> responseDTO = fileService.uploadFile(ResourceRequestDTO.builder()
                .files(files)
                .user(user.get())
                .path(path)
                .build()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    @DeleteMapping
    public ResponseEntity<?> deleteResource(@RequestParam String path, @AuthenticationPrincipal UserDetails userDetails) throws FileNotFoundException {
        if (userDetails == null) {
            return ErrorResponseUtils.print("User not logged in", HttpStatus.UNAUTHORIZED);
        }

        path = PathValidationUtils.validateResourcePath(path);

        Optional<User> user = userService.findByUsername(userDetails.getUsername());
        if (user.isEmpty()) {
            return ErrorResponseUtils.print("User not found", HttpStatus.NOT_FOUND);
        }

        try {
            fileService.deleteResourceOrDirectory(ResourceRequestDTO.builder()
                    .path(path)
                    .user(user.get())
                    .build()
            );

            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (NoSuchKeyException e) {
            return ErrorResponseUtils.print("File " + path + " not found", HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchResource(@RequestParam String query, @AuthenticationPrincipal UserDetails userDetails) throws FileNotFoundException {
        if (userDetails == null) {
            return ErrorResponseUtils.print("User not logged in", HttpStatus.UNAUTHORIZED);
        }

        if (query == null || query.isEmpty()) {
            return ErrorResponseUtils.print("Query string is empty", HttpStatus.BAD_REQUEST);
        }

        Optional<User> user = userService.findByUsername(userDetails.getUsername());
        if (user.isEmpty()) {
            return ErrorResponseUtils.print("User not found", HttpStatus.NOT_FOUND);
        }

        List<ResourceResponseDTO> response = fileService.findResource(ResourceRequestDTO.builder()
                .user(user.get())
                .path(query)
                .build());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // todo Реализовать
    @GetMapping("/move")
    public ResponseEntity<?> moveResource(@RequestParam String from, @RequestParam String to, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ErrorResponseUtils.print("User not logged in", HttpStatus.UNAUTHORIZED);
        }

        if (from == null || from.isEmpty() || to == null || to.isEmpty()) {
            return ErrorResponseUtils.print("From and To are empty. Try form=/1.txt&to=/123/", HttpStatus.BAD_REQUEST);
        }

        Optional<User> user = userService.findByUsername(userDetails.getUsername());
        if (user.isEmpty()) {
            return ErrorResponseUtils.print("User not found", HttpStatus.NOT_FOUND);
        }

        from = PathValidationUtils.validateResourcePath(from);
        to = PathValidationUtils.validateResourcePath(to);

        ResourceResponseDTO response = fileService.moveResource(MoveResourceRequestDTO.builder()
                .from(from)
                .to(to)
                .user(user.get())
                .build());

        return ResponseEntity.ok().body(response);
    }


    // todo тут траблы с названиями файлов и их расширениями. вроде остальное качает нормальноо
    @GetMapping("/download")
    public ResponseEntity<?> downloadResource(@RequestParam String path, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ErrorResponseUtils.print("User not logged in", HttpStatus.UNAUTHORIZED);
        }

        Optional<User> user = userService.findByUsername(userDetails.getUsername());
        if (user.isEmpty()) {
            return ErrorResponseUtils.print("User not found", HttpStatus.NOT_FOUND);
        }

        path = PathValidationUtils.validateResourcePath(path);

        try {
            byte[] response = fileService.download(ResourceRequestDTO.builder()
                    .user(user.get())
                    .path(path)
                    .build());

            String fileName = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
            fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
            if (fileName.isEmpty()) fileName = "download";

            if (path.endsWith("/")) {
                fileName += ".zip";
            }

            String contentDisposition = "attachment; filename=\"" + fileName + "\"";

            return ResponseEntity.status(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(response);

        } catch (NoSuchKeyException e) {
            return ErrorResponseUtils.print("File " + path + " not found", HttpStatus.NOT_FOUND);
        } catch (IOException e) {
            System.out.println("DownloadController: Exception! " + e.getMessage());
            return ErrorResponseUtils.print("Error occurred while downloading file", HttpStatus.SERVICE_UNAVAILABLE);
        }
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
// Ну и собственно try/catch должны быть везде чтобы говна не случалась. И конечно же это должно быть в Сервсие
