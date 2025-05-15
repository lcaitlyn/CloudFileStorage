package edu.lcaitlyn.cloudfilestorage.utils;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@UtilityClass
public class PathValidationUtils {

    public static boolean isValidPath(String path) {
        if (path == null || path.trim().isEmpty()) return false;
        if (path.equals("/")) return true;

        Pattern pattern = Pattern.compile("(^//|//|\\.{2}|\\\\)");
        return !pattern.matcher(path).find();
    }

    public static String validateResourcePath(String path) {
        if (!isValidPath(path)) {
            throw new IllegalArgumentException("Invalid path: " + path);
        }

        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        return path;
    }

    public static String validateDirectoryPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "/";
        }

        path = path.trim().replace("\\", "/");

        String[] parts = path.split("/");
        List<String> cleaned = new ArrayList<>();
        for (String part : parts) {
            if (part.isEmpty() || part.equals(".") || part.equals("..")) continue;
            cleaned.add(part);
        }

        if (cleaned.isEmpty()) {
            return "/";
        }

        String lastPart = cleaned.get(cleaned.size() - 1);
        if (lastPart.contains(".")) {
            throw new IllegalArgumentException("Path must refer to a directory, not a file:" + path);
        }

        return "/" + String.join("/", cleaned) + "/";
    }
}
