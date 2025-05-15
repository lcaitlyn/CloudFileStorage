package edu.lcaitlyn.cloudfilestorage.controller;

import edu.lcaitlyn.cloudfilestorage.DTO.response.ResourceResponseDTO;
import edu.lcaitlyn.cloudfilestorage.DTO.request.ResourceRequestDTO;
import edu.lcaitlyn.cloudfilestorage.exception.DirectoryNotFound;
import edu.lcaitlyn.cloudfilestorage.models.AuthUserDetails;
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

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/directory")
@AllArgsConstructor
public class DirectoryController {

    private FileService fileService;

    private UserService userService;

    // todo опа а тут еще есть говна)
    @GetMapping
    public ResponseEntity<?> getDirectory(
            @RequestParam String path,
            @AuthenticationPrincipal AuthUserDetails userDetails) {
        path = PathValidationUtils.validateDirectoryPath(path);
        List<ResourceResponseDTO> list = fileService.getDirectory(ResourceRequestDTO.builder()
                .path(path)
                .user(userDetails.getUser())
                .build());

        return new ResponseEntity<>(list, HttpStatus.OK);
    }


    @PostMapping
    public ResponseEntity<?> createDirectory(
            @RequestParam String path,
            @AuthenticationPrincipal AuthUserDetails userDetails) {
        path = PathValidationUtils.validateDirectoryPath(path);

        ResourceResponseDTO responseDTO = fileService.createDirectory(ResourceRequestDTO.builder()
                        .path(path)
                        .user(userDetails.getUser())
                        .build());

        return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
    }
}
