package com.techora.common.application.util;

public class StringUtils {

    public static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
