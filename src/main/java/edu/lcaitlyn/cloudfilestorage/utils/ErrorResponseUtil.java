package edu.lcaitlyn.cloudfilestorage.utils;

import edu.lcaitlyn.cloudfilestorage.DTO.ErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ErrorResponseUtil {
    public static ResponseEntity<ErrorResponseDTO> print(String message, HttpStatus status) {
        ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO();
        errorResponseDTO.setMessage(message);
        return new ResponseEntity<>(errorResponseDTO, status);
    }
}
