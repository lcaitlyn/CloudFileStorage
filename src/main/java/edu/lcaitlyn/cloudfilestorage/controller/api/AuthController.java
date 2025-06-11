package edu.lcaitlyn.cloudfilestorage.controller.api;

import edu.lcaitlyn.cloudfilestorage.DTO.request.UserRequestDTO;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public interface AuthController {
    @PostMapping("/sign-in")
    ResponseEntity<?> login(@RequestBody UserRequestDTO dto, HttpSession session);

    @PostMapping("/sign-up")
    ResponseEntity<?> register(@Valid @RequestBody UserRequestDTO dto);

    @PostMapping("/sign-out")
    ResponseEntity<?> logout(HttpSession session);
}
