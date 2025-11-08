package edu.lcaitlyn.cloudfilestorage.controller.impl;

import edu.lcaitlyn.cloudfilestorage.exception.*;
import edu.lcaitlyn.cloudfilestorage.utils.ErrorResponseUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class ExceptionController {
    @ExceptionHandler
    public ResponseEntity<?> handleUserNotFoundException(UserNotFoundException ex) {
        log.error("User not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler
    public ResponseEntity<?> handeBadCredentialsException(BadCredentialsException ex) {
        log.error("Bad Credentials: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler
    public ResponseEntity<?> handeUserAlreadyExistException(UserAlreadyExists ex) {
        log.error("User Already Exists: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler
    public ResponseEntity<?> handeUsernameNotFoundException(UsernameNotFoundException ex) {
        log.error("Username Not Found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Illegal Argument: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler
    public ResponseEntity<?> handeNoSuchKeyException(NoSuchKeyException ex) {
        log.error("No Such Key: {}", ex.getMessage());
        return ErrorResponseUtils.print(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler
    public ResponseEntity<?> handleFileNotFoundException(FileNotFoundException ex) {
        log.error("File Not Found: {}", ex.getMessage());
        return ErrorResponseUtils.print(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler
    public ResponseEntity<?> handleDirectoryNotFoundException(DirectoryNotFound ex) {
        log.error("Directory Not Found: {}", ex.getMessage());
        return ErrorResponseUtils.print(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler
    public ResponseEntity<?> handeResponseStatusException(ResponseStatusException ex) {
        log.error(ex.getMessage());
        return ErrorResponseUtils.print(ex.getReason(), ex.getStatusCode());
    }

    @ExceptionHandler
    public ResponseEntity<?> handeResourceNotFoundException(ResourceNotFound ex) {
        log.error("Resource Not Found: {}", ex.getMessage());
        return ErrorResponseUtils.print(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        log.error(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        log.error(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Resource size should not be larger than 1MB!"));
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<?> handleStorageException(StorageException ex) {
        log.error("Storage error: {}", ex.getMessage(), ex);
        return ErrorResponseUtils.print(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ResourceAlreadyExists.class)
    public ResponseEntity<?> handleResourceAlreadyExistsException(ResourceAlreadyExists ex) {
        log.error("Resource already exists: {}", ex.getMessage(), ex);
        return ErrorResponseUtils.print(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(DirectoryAlreadyExists.class)
    public ResponseEntity<?> handleResourceAlreadyExistsException(DirectoryAlreadyExists ex) {
        log.error("Directory already exists: {}", ex.getMessage(), ex);
        return ErrorResponseUtils.print(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(FileServiceException.class)
    public ResponseEntity<?> handeFileServiceException(FileServiceException ex) {
        log.error("File Service Exception: {}", ex.getMessage(), ex);
        return ErrorResponseUtils.print(ex.getMessage(), HttpStatus.CONFLICT);
    }
}
