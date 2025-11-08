package edu.lcaitlyn.cloudfilestorage.utils;

import edu.lcaitlyn.cloudfilestorage.service.impl.FileServiceImpl;

public class FileServiceUtils {
    public static String createKey(Long userId, String path) {
        String prefix = String.format(FileServiceImpl.KEY_PREFIX, userId);
        return prefix + path;
    }

    public static String extractNameFromKey(String key) {
        if (key.endsWith("/")) {
            key = key.substring(0, key.length() - 1);
        }

        int lastSlash = key.lastIndexOf('/');
        if (lastSlash < 0) {
            return "";
        }

        return key.substring(lastSlash + 1);
    }

    public static String extractPathFromKey(String key) {
        key = key.substring(key.indexOf("/") + 1);

        if (key.endsWith("/")) {
            key = key.substring(0, key.length() - 1);
        }

        int lastSlash = key.lastIndexOf('/');
        String rawPath = key.substring(0, lastSlash + 1);

        return rawPath.isEmpty() ? "/" : "/" + rawPath;
    }

    public static String removeNameFormPath(String path) {
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        if (!path.contains("/")) {
            return "/";
        }

        int lastSlash = path.lastIndexOf('/');
        return path.substring(0, lastSlash + 1);
    }

    public static boolean isRootPath(String path) {
        return path.length() == 1 && path.startsWith("/");
    }
}
