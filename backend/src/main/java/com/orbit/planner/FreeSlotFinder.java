package com.orbit.planner;

import java.time.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Pure scheduling logic: given busy ranges (for example from Google Calendar free/busy),
 * finds free working-hours gaps long enough to be focus blocks. The Calendar API call itself
 * is a thin layer to add later (see docs/TASK_BOARD.md), so this logic can be fully unit tested.
 */
public final class FreeSlotFinder {

    private FreeSlotFinder() {}

    public static List<TimeRange> find(List<TimeRange> busy, LocalDate from, int days,
                                       LocalTime dayStart, LocalTime dayEnd, int minMinutes, ZoneId zone) {
        List<TimeRange> sorted = busy.stream().sorted(Comparator.comparing(TimeRange::start)).toList();
        Duration min = Duration.ofMinutes(minMinutes);
        List<TimeRange> free = new ArrayList<>();

        for (int i = 0; i < days; i++) {
            LocalDate day = from.plusDays(i);
            Instant windowEnd = day.atTime(dayEnd).atZone(zone).toInstant();
            Instant cursor = day.atTime(dayStart).atZone(zone).toInstant();

            for (TimeRange b : sorted) {
                if (!b.end().isAfter(cursor)) {
                    continue;                      // ended before our cursor
                }
                if (!b.start().isBefore(windowEnd)) {
                    break;                         // starts after this day's window
                }
                Instant gapEnd = b.start().isBefore(windowEnd) ? b.start() : windowEnd;
                addIfLongEnough(free, cursor, gapEnd, min);
                if (b.end().isAfter(cursor)) {
                    cursor = b.end();
                }
                if (!cursor.isBefore(windowEnd)) {
                    break;
                }
            }
            addIfLongEnough(free, cursor, windowEnd, min);
        }
        return free;
    }

    private static void addIfLongEnough(List<TimeRange> out, Instant start, Instant end, Duration min) {
        if (end.isAfter(start) && Duration.between(start, end).compareTo(min) >= 0) {
            out.add(new TimeRange(start, end));
        }
    }
}
