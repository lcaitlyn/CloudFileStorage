package edu.lcaitlyn.cloudfilestorage.controller;

import edu.lcaitlyn.cloudfilestorage.DTO.ResourceResponseDTO;
import edu.lcaitlyn.cloudfilestorage.DTO.ResourceRequestDTO;
import edu.lcaitlyn.cloudfilestorage.models.User;
import edu.lcaitlyn.cloudfilestorage.service.FileService;
import edu.lcaitlyn.cloudfilestorage.service.UserService;
import edu.lcaitlyn.cloudfilestorage.utils.ErrorResponseUtils;
import edu.lcaitlyn.cloudfilestorage.utils.PathValidationUtils;
import lombok.AllArgsConstructor;
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
}
