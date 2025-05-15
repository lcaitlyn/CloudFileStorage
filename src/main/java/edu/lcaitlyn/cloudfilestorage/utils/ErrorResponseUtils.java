package edu.lcaitlyn.cloudfilestorage.utils;

import edu.lcaitlyn.cloudfilestorage.DTO.response.ErrorResponseDTO;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

@UtilityClass
public class ErrorResponseUtils {
    public static ResponseEntity<ErrorResponseDTO> print(String message, HttpStatusCode status) {
        ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO();
        errorResponseDTO.setMessage(message);
        return new ResponseEntity<>(errorResponseDTO, status);
    }
}
