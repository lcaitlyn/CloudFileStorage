package edu.lcaitlyn.cloudfilestorage.controller.impl;

import edu.lcaitlyn.cloudfilestorage.DTO.request.ResourceRequestDTO;
import edu.lcaitlyn.cloudfilestorage.DTO.request.UserRequestDTO;
import edu.lcaitlyn.cloudfilestorage.DTO.response.UserResponseDTO;
import edu.lcaitlyn.cloudfilestorage.controller.api.AuthController;
import edu.lcaitlyn.cloudfilestorage.models.AuthUserDetails;
import edu.lcaitlyn.cloudfilestorage.models.User;
import edu.lcaitlyn.cloudfilestorage.service.FileService;
import edu.lcaitlyn.cloudfilestorage.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@AllArgsConstructor
public class AuthControllerImpl implements AuthController {

    private final UserService userService;

    private final AuthenticationManager authenticationManager;

    private final FileService fileService;

    @Override
    public ResponseEntity<?> login(@RequestBody UserRequestDTO dto, HttpSession session) {
        Optional<User> user = userService.authenticate(dto.getUsername(), dto.getPassword());
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AuthUserDetails userDetails = new AuthUserDetails(user.get());

        Authentication authToken = new UsernamePasswordAuthenticationToken(
                dto.getUsername(),
                dto.getPassword(),
                userDetails.getAuthorities()
        );
        Authentication authenticated = authenticationManager.authenticate(authToken);
        SecurityContextHolder.getContext().setAuthentication(authenticated);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());

        UserResponseDTO responseDTO = UserResponseDTO.builder()
                .username(user.get().getUsername())
                .build();

        return ResponseEntity.ok(responseDTO);
    }

    // todo сделать проверку на логин, чтобы он был не меньше 5 символов и прочее. все еще не работает
    // todo сделать чтобы логинило после входа сразу
    @Override
    public ResponseEntity<?> register(@Valid @RequestBody UserRequestDTO dto) {
        userService.save(new User(dto.getUsername(), dto.getPassword()));

        Optional<User> user = userService.findByUsername(dto.getUsername());
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        fileService.createRootDirectory(ResourceRequestDTO.builder()
                .user(user.get())
                .build()
        );

        UserResponseDTO responseDTO = UserResponseDTO.builder().username(dto.getUsername()).build();
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    @Override
    public ResponseEntity<?> logout(HttpSession session) {
        SecurityContextHolder.clearContext();
        session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
