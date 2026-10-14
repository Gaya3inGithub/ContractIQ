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
