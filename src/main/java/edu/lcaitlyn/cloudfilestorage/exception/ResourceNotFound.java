package edu.lcaitlyn.cloudfilestorage.exception;

public class ResourceNotFound extends RuntimeException{
    public ResourceNotFound(String message) {
        super(message);
    }
}
