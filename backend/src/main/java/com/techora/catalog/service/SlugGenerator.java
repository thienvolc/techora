package com.techora.catalog.service;

import java.text.Normalizer;
import java.util.Locale;

public class SlugGenerator {
    private static final String NON_ASCII_REGEX = "[^\\p{ASCII}]";
    private static final String NON_ALPHANUMERIC_REGEX = "[^a-z0-9]+";
    private static final String EDGE_SEPARATOR_REGEX = "^-|-$";
    private static final String EMPTY_SLUG = "item";

    public static String generate(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll(NON_ASCII_REGEX, "")
                .toLowerCase(Locale.ROOT)
                .replaceAll(NON_ALPHANUMERIC_REGEX, "-")
                .replaceAll(EDGE_SEPARATOR_REGEX, "");

        return normalized.isBlank() ? EMPTY_SLUG : normalized;
    }
}
