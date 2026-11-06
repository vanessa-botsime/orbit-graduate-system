package com.orbit.planner;

import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FreeSlotFinderTest {

    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final LocalDate DAY = LocalDate.of(2026, 11, 9);

    private static Instant at(int hour) {
        return DAY.atTime(hour, 0).atZone(UTC).toInstant();
    }

    @Test
    void splitsWorkdayAroundAMeeting() {
        List<TimeRange> busy = List.of(new TimeRange(at(10), at(11)));
        List<TimeRange> free = FreeSlotFinder.find(busy, DAY, 1, LocalTime.of(9, 0), LocalTime.of(17, 0), 60, UTC);
        assertEquals(List.of(new TimeRange(at(9), at(10)), new TimeRange(at(11), at(17))), free);
    }

    @Test
    void wholeDayFreeWhenNothingBusy() {
        List<TimeRange> free = FreeSlotFinder.find(List.of(), DAY, 1, LocalTime.of(9, 0), LocalTime.of(17, 0), 60, UTC);
        assertEquals(List.of(new TimeRange(at(9), at(17))), free);
    }

    @Test
    void dropsGapsShorterThanMinimum() {
        List<TimeRange> busy = List.of(new TimeRange(at(9), at(16)), new TimeRange(at(16), at(17)));
        List<TimeRange> free = FreeSlotFinder.find(busy, DAY, 1, LocalTime.of(9, 0), LocalTime.of(17, 0), 30, UTC);
        assertTrue(free.isEmpty());
    }

    @Test
    void overlappingMeetingsAreMerged() {
        List<TimeRange> busy = List.of(new TimeRange(at(10), at(12)), new TimeRange(at(11), at(13)));
        List<TimeRange> free = FreeSlotFinder.find(busy, DAY, 1, LocalTime.of(9, 0), LocalTime.of(17, 0), 60, UTC);
        assertEquals(List.of(new TimeRange(at(9), at(10)), new TimeRange(at(13), at(17))), free);
    }
}
