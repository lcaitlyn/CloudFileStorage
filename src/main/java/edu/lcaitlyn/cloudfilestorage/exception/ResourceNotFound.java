package edu.lcaitlyn.cloudfilestorage.exception;

public class ResourceNotFound extends RuntimeException{
    public ResourceNotFound(String filename) {
        super("File '" + filename + "' not found");
    }
}
