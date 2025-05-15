package edu.lcaitlyn.cloudfilestorage.utils;

import lombok.experimental.UtilityClass;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;

@UtilityClass
public class ControllerUtils {
    public static boolean isAuthenticated(Authentication auth) {
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }
}
