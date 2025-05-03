package edu.lcaitlyn.cloudfilestorage.controller;

import edu.lcaitlyn.cloudfilestorage.DTO.ResourceResponseDTO;
import edu.lcaitlyn.cloudfilestorage.DTO.ResourceRequestDTO;
import edu.lcaitlyn.cloudfilestorage.exception.DirectoryNotFound;
import edu.lcaitlyn.cloudfilestorage.models.User;
import edu.lcaitlyn.cloudfilestorage.service.FileService;
import edu.lcaitlyn.cloudfilestorage.service.UserService;
import edu.lcaitlyn.cloudfilestorage.utils.ErrorResponseUtils;
import edu.lcaitlyn.cloudfilestorage.utils.PathValidationUtils;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/directory")
@AllArgsConstructor
public class DirectoryController {

    private FileService fileService;

    private UserService userService;

    @GetMapping
    public ResponseEntity<?> getDirectory(@RequestParam String path,  @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ErrorResponseUtils.print("User not logged in", HttpStatus.UNAUTHORIZED);
        }

        path = PathValidationUtils.validateDirectoryPath(path);

        Optional<User> user = userService.findByUsername(userDetails.getUsername());
        if (user.isEmpty()) {
            return ErrorResponseUtils.print("User not found", HttpStatus.NOT_FOUND);
        }

        try {
            List<ResourceResponseDTO> list = fileService.getDirectory(ResourceRequestDTO.builder()
                            .path(path)
                            .user(user.get())
                    .build());

            return new ResponseEntity<>(list, HttpStatus.OK);
        } catch (DirectoryNotFound e) {
            return ErrorResponseUtils.print(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }


    @PostMapping
    public ResponseEntity<?> createDirectory(@RequestParam String path,  @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ErrorResponseUtils.print("User not logged in", HttpStatus.UNAUTHORIZED);
        }

        path = PathValidationUtils.validateDirectoryPath(path);

        Optional<User> user = userService.findByUsername(userDetails.getUsername());
        if (user.isEmpty()) {
            return ErrorResponseUtils.print("User not found", HttpStatus.NOT_FOUND);
        }

        ResourceResponseDTO responseDTO = fileService.createDirectory(ResourceRequestDTO.builder()
                        .path(path)
                        .user(user.get())
                        .build());

        return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
    }
}
