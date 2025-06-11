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

        // Отклоняем //, .., \\, . как отдельный сегмент, ?, и *
        Pattern pattern = Pattern.compile(
                "(^//|//|\\.{2}|\\\\|(^|/)\\.(?:/|$)|[?*\"<>|:%\b\n\r\t])"
        );
        return !pattern.matcher(path).find();
    }

    public static String validateResourcePath(String path) throws IllegalArgumentException {
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

        // Проверяем на запрещённые символы (например: *, ?, #, %, .., .)
        Pattern unsafePattern = Pattern.compile("[*?#%]|(^|/)\\.\\.?(/|$)");
        if (unsafePattern.matcher(path).find()) {
            throw new IllegalArgumentException("Invalid path: " + path);
        }

        // Удаляем пустые части и нормализуем
        String[] parts = path.split("/");
        List<String> cleaned = new ArrayList<>();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            cleaned.add(part);
        }

        if (cleaned.isEmpty()) {
            return "/";
        }

        // Проверка: последний сегмент не должен быть файлом
        String lastPart = cleaned.getLast();
        if (lastPart.contains(".")) {
            throw new IllegalArgumentException("Path must refer to a directory, not a file: " + path);
        }

        return "/" + String.join("/", cleaned) + "/";
    }
}
