package edu.lcaitlyn.cloudfilestorage.exception;

public class StorageException extends RuntimeException {
    public StorageException(String message, Exception cause) {
        super(message, cause);
    }

    public StorageException(String message) {
        super(message);
    }
}
