package edu.lcaitlyn.cloudfilestorage.controller;

import edu.lcaitlyn.cloudfilestorage.exception.UserAlreadyExist;
import edu.lcaitlyn.cloudfilestorage.exception.UserNotFoundException;
import edu.lcaitlyn.cloudfilestorage.models.User;
import edu.lcaitlyn.cloudfilestorage.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.Optional;

@Controller
@AllArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String login(HttpSession session) {
        if (session.getAttribute("user") != null)  return "redirect:/";

        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, HttpSession session) {
        if (session.getAttribute("user") != null)  return "redirect:/";

        Optional<User> user = userService.authenticate(username, password);
        if (user.isPresent()) {
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    user.get(), password, Collections.singletonList(new SimpleGrantedAuthority("ADMIN"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            session.setAttribute("user", user.get().getUsername());
        }
        return "redirect:/";
    }

    @ExceptionHandler
    public ResponseEntity<String> handleUserNotFoundException(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler
    public ResponseEntity<String> handeBadCredentialsException(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
    }

    @GetMapping("/register")
    public String register(HttpSession session) {
        if (session.getAttribute("user") != null)  return "redirect:/";

        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username, @RequestParam String password, HttpSession session) {
        if (session.getAttribute("user") != null)  return "redirect:/";

        userService.save(new User(username, password));
        return login(username, password, session);
    }

    @ExceptionHandler
    public ResponseEntity<String> handeUserAlreadyExistException(UserAlreadyExist ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        SecurityContextHolder.clearContext();
        session.removeAttribute("user");
        return "redirect:/";
    }
}
