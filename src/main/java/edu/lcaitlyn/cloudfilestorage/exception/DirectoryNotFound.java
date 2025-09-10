package edu.lcaitlyn.cloudfilestorage.exception;

public class DirectoryNotFound extends RuntimeException {
    public DirectoryNotFound(String fileName) {
        super("Directory '" + fileName + "' not found");
    }
}
