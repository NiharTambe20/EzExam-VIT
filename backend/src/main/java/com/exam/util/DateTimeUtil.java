package com.exam.util;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Utility for parsing and formatting date/time values consistently
 * across the REST API, Java models, and MySQL DATETIME/TIMESTAMP columns.
 */
public final class DateTimeUtil {

    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final List<DateTimeFormatter> SUPPORTED_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    );

    private DateTimeUtil() {}

    /**
     * Parses an input string into a java.sql.Timestamp.
     * Null, empty, or whitespace-only strings return null.
     * Throws IllegalArgumentException if the string is non-empty but cannot be parsed.
     */
    public static Timestamp parseTimestamp(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        String trimmed = input.trim();

        // Try supported date-time formats
        for (DateTimeFormatter fmt : SUPPORTED_FORMATS) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(trimmed, fmt);
                return Timestamp.valueOf(ldt);
            } catch (DateTimeParseException ignored) {}
        }

        // Try date-only format (assume start of day 00:00:00)
        try {
            LocalDate ld = LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE);
            return Timestamp.valueOf(ld.atStartOfDay());
        } catch (DateTimeParseException ignored) {}

        throw new IllegalArgumentException("Invalid date-time format: '" + input +
                "'. Expected formats: yyyy-MM-ddTHH:mm, yyyy-MM-dd HH:mm:ss, or yyyy-MM-dd.");
    }

    /**
     * Formats a Timestamp to a standard string "yyyy-MM-dd HH:mm:ss".
     * Returns null if timestamp is null.
     */
    public static String formatTimestamp(Timestamp ts) {
        if (ts == null) {
            return null;
        }
        return ts.toLocalDateTime().format(DISPLAY_FMT);
    }
}
