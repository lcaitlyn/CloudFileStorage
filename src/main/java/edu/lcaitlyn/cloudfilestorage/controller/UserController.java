package edu.lcaitlyn.cloudfilestorage.controller;

import edu.lcaitlyn.cloudfilestorage.DTO.UserResponseDTO;
import edu.lcaitlyn.cloudfilestorage.models.User;
import edu.lcaitlyn.cloudfilestorage.service.UserService;
import edu.lcaitlyn.cloudfilestorage.utils.ControllerUtils;
import edu.lcaitlyn.cloudfilestorage.utils.ErrorResponseUtils;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/user")
@AllArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!ControllerUtils.isAuthenticated(auth)) {
            return ErrorResponseUtils.print("User are not authenticated", HttpStatus.UNAUTHORIZED);
        }

        String username = auth.getName();
        Optional<User> user = userService.findByUsername(username);

        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        UserResponseDTO responseDTO = UserResponseDTO.builder().username(user.get().getUsername()).build();
        return ResponseEntity.ok(responseDTO);
    }

}
