package edu.lcaitlyn.cloudfilestorage.controller.impl;

import edu.lcaitlyn.cloudfilestorage.DTO.request.ResourceRequestDTO;
import edu.lcaitlyn.cloudfilestorage.DTO.response.ResourceResponseDTO;
import edu.lcaitlyn.cloudfilestorage.controller.api.DirectoryController;
import edu.lcaitlyn.cloudfilestorage.models.AuthUserDetails;
import edu.lcaitlyn.cloudfilestorage.service.FileService;
import edu.lcaitlyn.cloudfilestorage.utils.PathValidationUtils;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
public class DirectoryControllerImpl implements DirectoryController {

    private FileService fileService;

    @Override
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


    @Override
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
