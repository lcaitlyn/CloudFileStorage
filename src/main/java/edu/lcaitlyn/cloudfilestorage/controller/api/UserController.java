package edu.lcaitlyn.cloudfilestorage.controller.api;

import edu.lcaitlyn.cloudfilestorage.models.AuthUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public interface UserController {
    @GetMapping("/me")
    ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal AuthUserDetails userDetails);
}
