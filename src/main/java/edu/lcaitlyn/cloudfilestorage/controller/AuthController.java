package edu.lcaitlyn.cloudfilestorage.controller;

import edu.lcaitlyn.cloudfilestorage.DTO.UserRequestDTO;
import edu.lcaitlyn.cloudfilestorage.DTO.UserResponseDTO;
import edu.lcaitlyn.cloudfilestorage.models.User;
import edu.lcaitlyn.cloudfilestorage.service.UserService;
import edu.lcaitlyn.cloudfilestorage.utils.ControllerUtils;
import edu.lcaitlyn.cloudfilestorage.utils.ErrorResponseUtils;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@AllArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    private final AuthenticationManager authenticationManager;

    @PostMapping("/sign-in")
    public ResponseEntity<?> login(@RequestBody UserRequestDTO userRequestDTO, HttpSession session) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (ControllerUtils.isAuthenticated(auth)) {
            return ErrorResponseUtils.print("User already authenticated", HttpStatus.BAD_REQUEST);
        }

        String username = userRequestDTO.getUsername();
        String password = userRequestDTO.getPassword();
        Optional<User> user = userService.authenticate(username, password);

        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Authentication authToken = new UsernamePasswordAuthenticationToken(username, password);
        Authentication authenticated = authenticationManager.authenticate(authToken);
        SecurityContextHolder.getContext().setAuthentication(authenticated);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());

        UserResponseDTO responseDTO = UserResponseDTO.builder().username(user.get().getUsername()).build();
        return ResponseEntity.ok(responseDTO);
    }

    @PostMapping("/sign-up")
    public ResponseEntity<?> register(@RequestBody UserRequestDTO userRequestDTO) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (ControllerUtils.isAuthenticated(auth)) {
            return ErrorResponseUtils.print("User already authenticated", HttpStatus.BAD_REQUEST);
        }

        String username = userRequestDTO.getUsername();
        String password = userRequestDTO.getPassword();

        userService.save(new User(username, password));

        UserResponseDTO responseDTO = UserResponseDTO.builder().username(username).build();
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    @PostMapping("/sign-out")
    public ResponseEntity<?> logout(HttpSession session) {
        // todo переделать это на UserDetail
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!ControllerUtils.isAuthenticated(auth)) {
            return ErrorResponseUtils.print("User are not authenticated", HttpStatus.UNAUTHORIZED);
        }

        SecurityContextHolder.clearContext();
        session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
