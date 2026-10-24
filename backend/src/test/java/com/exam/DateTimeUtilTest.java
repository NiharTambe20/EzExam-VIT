package com.exam;

import com.exam.util.DateTimeUtil;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

public class DateTimeUtilTest {

    @Test
    public void testParseNullOrEmptyDates() {
        assertNull(DateTimeUtil.parseTimestamp(null));
        assertNull(DateTimeUtil.parseTimestamp(""));
        assertNull(DateTimeUtil.parseTimestamp("   "));
    }

    @Test
    public void testParseValidDateFormats() {
        // ISO local date-time with T
        Timestamp ts1 = DateTimeUtil.parseTimestamp("2026-10-25T14:30:00");
        assertNotNull(ts1);

        // HTML5 datetime-local without seconds
        Timestamp ts2 = DateTimeUtil.parseTimestamp("2026-10-25T14:30");
        assertNotNull(ts2);

        // SQL standard date-time with space
        Timestamp ts3 = DateTimeUtil.parseTimestamp("2026-10-25 14:30:00");
        assertNotNull(ts3);

        // Date-only
        Timestamp ts4 = DateTimeUtil.parseTimestamp("2026-10-25");
        assertNotNull(ts4);
    }

    @Test
    public void testRejectInvalidDateFormats() {
        assertThrows(IllegalArgumentException.class, () -> {
            DateTimeUtil.parseTimestamp("invalid-date-string");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            DateTimeUtil.parseTimestamp("25/10/2026");
        });
    }

    @Test
    public void testFormatTimestamp() {
        assertNull(DateTimeUtil.formatTimestamp(null));

        Timestamp ts = Timestamp.valueOf("2026-10-25 14:30:00");
        assertEquals("2026-10-25 14:30:00", DateTimeUtil.formatTimestamp(ts));
    }
}
