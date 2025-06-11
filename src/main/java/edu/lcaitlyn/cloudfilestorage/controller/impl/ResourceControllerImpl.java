package edu.lcaitlyn.cloudfilestorage.controller.impl;

import edu.lcaitlyn.cloudfilestorage.DTO.request.MoveResourceRequestDTO;
import edu.lcaitlyn.cloudfilestorage.DTO.request.ResourceRequestDTO;
import edu.lcaitlyn.cloudfilestorage.DTO.response.DownloadResourceResponseDTO;
import edu.lcaitlyn.cloudfilestorage.DTO.response.ResourceResponseDTO;
import edu.lcaitlyn.cloudfilestorage.controller.api.ResourceController;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/resource")
@AllArgsConstructor
public class ResourceControllerImpl implements ResourceController {

    private final FileService fileService;

    @Override
    public ResponseEntity<?> getResource(
            @RequestParam String path,
            @AuthenticationPrincipal AuthUserDetails userDetails) {
        path = PathValidationUtils.validateResourcePath(path);

        ResourceResponseDTO responseDTO = fileService.getResource(
                ResourceRequestDTO.builder()
                        .user(userDetails.getUser())
                        .path(path)
                        .build());

        return new ResponseEntity<>(responseDTO, HttpStatus.OK);
    }

    @Override
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

    @Override
    public ResponseEntity<?> deleteResource(
            @RequestParam String path,
            @AuthenticationPrincipal AuthUserDetails userDetails) {
        path = PathValidationUtils.validateResourcePath(path);

        fileService.deleteResource(ResourceRequestDTO.builder()
                .path(path)
                .user(userDetails.getUser())
                .build()
        );

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Override
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

    @Override
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

    @Override
    public ResponseEntity<?> downloadResource(
            @RequestParam String path,
            @AuthenticationPrincipal AuthUserDetails userDetails) {
        path = PathValidationUtils.validateResourcePath(path);

        DownloadResourceResponseDTO response = fileService.downloadResource(ResourceRequestDTO.builder()
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
