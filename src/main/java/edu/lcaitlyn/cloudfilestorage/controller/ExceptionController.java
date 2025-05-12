package edu.lcaitlyn.cloudfilestorage.controller;

import edu.lcaitlyn.cloudfilestorage.exception.UserAlreadyExist;
import edu.lcaitlyn.cloudfilestorage.exception.UserNotFoundException;
import edu.lcaitlyn.cloudfilestorage.utils.ErrorResponseUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.FileNotFoundException;
import java.util.Map;

@ControllerAdvice
public class ExceptionController {
    @ExceptionHandler
    public ResponseEntity<?> handleUserNotFoundException(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler
    public ResponseEntity<?> handeBadCredentialsException(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler
    public ResponseEntity<?> handeUserAlreadyExistException(UserAlreadyExist ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler
    public ResponseEntity<?> handeUsernameNotFoundException(UsernameNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler
    public ResponseEntity<?> handeNoSuchKeyException(NoSuchKeyException ex) {
        return ErrorResponseUtils.print(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler
    public ResponseEntity<?> handleFileNotFoundException(FileNotFoundException ex) {
        return ErrorResponseUtils.print(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler
    public ResponseEntity<?> handeResponseStatusException(ResponseStatusException ex) {
        return ErrorResponseUtils.print(ex.getReason(), ex.getStatusCode());
    }
}
