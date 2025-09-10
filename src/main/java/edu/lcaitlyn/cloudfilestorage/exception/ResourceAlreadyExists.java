package edu.lcaitlyn.cloudfilestorage.exception;

public class ResourceAlreadyExists extends RuntimeException {
    public ResourceAlreadyExists(String fileName) {
        super("Resource " + fileName + " already exists");
    }
}
