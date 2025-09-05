package edu.lcaitlyn.cloudfilestorage.controller.api;

import edu.lcaitlyn.cloudfilestorage.models.AuthUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/directory")
public interface DirectoryController {
    @GetMapping
    ResponseEntity<?> getDirectory(@RequestParam String path, @AuthenticationPrincipal AuthUserDetails userDetails);

    @PostMapping
    ResponseEntity<?> createDirectory(@RequestParam String path, @AuthenticationPrincipal AuthUserDetails userDetails);
}
