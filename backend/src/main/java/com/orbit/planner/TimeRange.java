package com.orbit.planner;

import java.time.Duration;
import java.time.Instant;

public record TimeRange(Instant start, Instant end) {
    public Duration length() {
        return Duration.between(start, end);
    }
}
