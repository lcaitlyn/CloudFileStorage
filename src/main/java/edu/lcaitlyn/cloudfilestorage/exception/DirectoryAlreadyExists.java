package edu.lcaitlyn.cloudfilestorage.exception;

public class DirectoryAlreadyExists extends RuntimeException {
    public DirectoryAlreadyExists(String fileName) {
        super("Directory " + fileName + " already exists");
    }
}
