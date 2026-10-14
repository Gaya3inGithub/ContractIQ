package com.contractiq.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;

public class DateUtil {
    private static final Logger logger = LoggerFactory.getLogger(DateUtil.class);

    private static final List<DateTimeFormatter> FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,                    // 2025-01-15
            strictFormatter("uuuu/MM/dd"),                        // 2025/01/15
            strictFormatter("MM/dd/uuuu"),                        // 01/15/2025 when unambiguous
            strictFormatter("d MMMM uuuu"),                       // 15 January 2025
            strictFormatter("MMMM d, uuuu"),                      // January 15, 2025
            strictFormatter("dd-MM-uuuu")                         // 15-01-2025 when unambiguous
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

        if (isAmbiguousNumericDate(clean)) {
            logger.debug("Refusing ambiguous numeric date string '{}'.", clean);
            return null;
        }

        // Try standard formats strictly. ResolverStyle.STRICT prevents dates such
        // as 2025-02-30 from being silently normalised by a formatter.
        for (DateTimeFormatter fmt : FORMATTERS) {
            try {
                return LocalDate.parse(clean, fmt);
            } catch (DateTimeParseException ignored) {
            }
        }

        logger.debug("Could not parse date string '{}'. Treating as null (ambiguous/unspecified).", clean);
        return null;
    }

    private static DateTimeFormatter strictFormatter(String pattern) {
        return DateTimeFormatter.ofPattern(pattern).withResolverStyle(ResolverStyle.STRICT);
    }

    /**
     * Numeric dates whose first two components are both 12 or below do not
     * identify a unique day/month ordering. ISO and year-first dates are not
     * affected.
     */
    private static boolean isAmbiguousNumericDate(String value) {
        String[] slashParts = value.split("/");
        String[] dashParts = value.split("-");
        String[] parts = slashParts.length == 3 ? slashParts : (dashParts.length == 3 ? dashParts : null);
        if (parts == null || parts[0].length() == 4) {
            return false;
        }
        try {
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);
            return first >= 1 && first <= 12 && second >= 1 && second <= 12;
        } catch (NumberFormatException ignored) {
            return false;
        }
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
