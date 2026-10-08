package com.spimex.user.client.util;

public class StringUtils {
    public static String requireText(String value, String name) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }

    public static String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    public static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
