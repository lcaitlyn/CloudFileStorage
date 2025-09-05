package edu.lcaitlyn.cloudfilestorage.controller.api;

import edu.lcaitlyn.cloudfilestorage.models.AuthUserDetails;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/resource")
public interface ResourceController {
    @GetMapping
    ResponseEntity<?> getResource(
            @RequestParam String path,
            @AuthenticationPrincipal AuthUserDetails userDetails
    );

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<?> uploadResource(
            @RequestPart("object") MultipartFile[] files,
            @RequestParam String path,
            @AuthenticationPrincipal AuthUserDetails userDetails
    );

    @DeleteMapping
    ResponseEntity<?> deleteResource(
            @RequestParam String path,
            @AuthenticationPrincipal AuthUserDetails userDetails
    );

    @GetMapping("/search")
    ResponseEntity<?> searchResource(
            @RequestParam String query,
            @AuthenticationPrincipal AuthUserDetails userDetails
    );

    @GetMapping("/move")
    ResponseEntity<?> moveResource(
            @RequestParam String from,
            @RequestParam String to,
            @AuthenticationPrincipal AuthUserDetails userDetails
    );

    @GetMapping("/download")
    ResponseEntity<?> downloadResource(
            @RequestParam String path,
            @AuthenticationPrincipal AuthUserDetails userDetails
    );
}
