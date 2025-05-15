package edu.lcaitlyn.cloudfilestorage.controller;

import edu.lcaitlyn.cloudfilestorage.DTO.response.UserResponseDTO;
import edu.lcaitlyn.cloudfilestorage.models.AuthUserDetails;
import edu.lcaitlyn.cloudfilestorage.models.User;
import edu.lcaitlyn.cloudfilestorage.service.UserService;
import edu.lcaitlyn.cloudfilestorage.utils.ControllerUtils;
import edu.lcaitlyn.cloudfilestorage.utils.ErrorResponseUtils;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/user")
@AllArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal AuthUserDetails userDetails) {
        UserResponseDTO responseDTO = UserResponseDTO.builder()
                .username(userDetails.getUsername())
                .build();
        return ResponseEntity.ok(responseDTO);
    }

}
