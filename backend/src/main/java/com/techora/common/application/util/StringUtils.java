package com.techora.common.application.util;

import org.jspecify.annotations.Nullable;

public class StringUtils {

    public static String trimToNull(@Nullable String value) {
        if (hasText(value)) {
            return value.trim();
        }
        return null;
    }

    public static String getOrDefault(@Nullable String value, String defaultValue) {
        return hasText(value) ? value : defaultValue;
    }

    public static boolean hasText(@Nullable String str) {
        return (str != null && !str.isBlank());
    }
}
