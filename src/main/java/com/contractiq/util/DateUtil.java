package com.contractiq.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class DateUtil {
    private static final Logger logger = LoggerFactory.getLogger(DateUtil.class);

    private static final List<DateTimeFormatter> FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,                    // 2025-01-15
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),           // 2025/01/15
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),           // 01/15/2025
            DateTimeFormatter.ofPattern("d MMMM yyyy"),          // 15 January 2025
            DateTimeFormatter.ofPattern("MMMM d, yyyy"),         // January 15, 2025
            DateTimeFormatter.ofPattern("dd-MM-yyyy")            // 15-01-2025
    );

    /**
     * Parses a date string safely into LocalDate.
     * Returns null if missing, null, or ambiguous/invalid.
     */
    public static LocalDate parseSafe(String dateStr) {
        if (dateStr == null) return null;
        String clean = dateStr.trim();
        if (clean.isEmpty() || "null".equalsIgnoreCase(clean) || "n/a".equalsIgnoreCase(clean) || "undefined".equalsIgnoreCase(clean)) {
            return null;
        }

        // Try standard formats
        for (DateTimeFormatter fmt : FORMATTERS) {
            try {
                return LocalDate.parse(clean, fmt);
            } catch (DateTimeParseException ignored) {
            }
        }

        logger.debug("Could not parse date string '{}'. Treating as null (ambiguous/unspecified).", clean);
        return null;
    }

    public static String formatIso(LocalDate date) {
        if (date == null) return null;
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    /**
     * Calculates renewal notice deadline: Expiration Date minus notice period days.
     */
    public static LocalDate calculateNoticeDeadline(LocalDate expirationDate, Integer noticePeriodDays) {
        if (expirationDate == null || noticePeriodDays == null || noticePeriodDays <= 0) {
            return null;
        }
        return expirationDate.minusDays(noticePeriodDays);
    }
}
