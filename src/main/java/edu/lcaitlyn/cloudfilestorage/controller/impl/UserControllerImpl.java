package edu.lcaitlyn.cloudfilestorage.controller.impl;

import edu.lcaitlyn.cloudfilestorage.DTO.response.UserResponseDTO;
import edu.lcaitlyn.cloudfilestorage.controller.api.UserController;
import edu.lcaitlyn.cloudfilestorage.models.AuthUserDetails;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class UserControllerImpl implements UserController {

    @Override
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal AuthUserDetails userDetails) {
        UserResponseDTO responseDTO = UserResponseDTO.builder()
                .username(userDetails.getUsername())
                .build();
        return ResponseEntity.ok(responseDTO);
    }

}
