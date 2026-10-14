package com.contractiq.test;

import com.contractiq.util.DateUtil;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DateUtilTest {

    @Test
    void testParseSafeStandardIso() {
        LocalDate d = DateUtil.parseSafe("2025-06-30");
        assertNotNull(d);
        assertEquals(2025, d.getYear());
        assertEquals(6, d.getMonthValue());
        assertEquals(30, d.getDayOfMonth());
    }

    @Test
    void testParseSafeAlternativeFormats() {
        LocalDate d1 = DateUtil.parseSafe("2025/12/31");
        assertNotNull(d1);
        assertEquals(2025, d1.getYear());

        LocalDate d2 = DateUtil.parseSafe("01/15/2026");
        assertNotNull(d2);
        assertEquals(2026, d2.getYear());
    }

    @Test
    void testParseSafeInvalidAndAmbiguousReturnsNull() {
        assertNull(DateUtil.parseSafe(null));
        assertNull(DateUtil.parseSafe(""));
        assertNull(DateUtil.parseSafe("   "));
        assertNull(DateUtil.parseSafe("null"));
        assertNull(DateUtil.parseSafe("undefined"));
        assertNull(DateUtil.parseSafe("some-arbitrary-string"));
        assertNull(DateUtil.parseSafe("2025-99-99"));
        assertNull(DateUtil.parseSafe("2025-02-30"));
        assertNull(DateUtil.parseSafe("01/02/2025"));
        assertNull(DateUtil.parseSafe("10-11-2025"));
    }

    @Test
    void testParseCommonLegalDateText() {
        assertEquals(LocalDate.of(2026, 1, 1), DateUtil.parseSafe("1 January 2026"));
        assertEquals(LocalDate.of(2026, 12, 31), DateUtil.parseSafe("31 December 2026"));
    }

    @Test
    void testCalculateNoticeDeadline() {
        LocalDate expiry = LocalDate.of(2026, 1, 1);
        LocalDate deadline = DateUtil.calculateNoticeDeadline(expiry, 60);

        assertNotNull(deadline);
        assertEquals(LocalDate.of(2025, 11, 2), deadline);
    }

    @Test
    void testCalculateNoticeDeadlineNullHandling() {
        assertNull(DateUtil.calculateNoticeDeadline(null, 30));
        assertNull(DateUtil.calculateNoticeDeadline(LocalDate.now(), null));
        assertNull(DateUtil.calculateNoticeDeadline(LocalDate.now(), -5));
    }
}
