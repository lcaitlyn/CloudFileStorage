package edu.lcaitlyn.cloudfilestorage.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class FileServiceException extends RuntimeException {
    public FileServiceException(String message) {
        super(message);
    }

    public FileServiceException( HttpStatus status, String message) {
        throw new ResponseStatusException(status, message);
    }
}
