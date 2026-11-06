package com.orbit.events;

import java.time.Instant;
import java.util.UUID;

public record AuditView(UUID id, String type, UUID applicationId, String detail, Instant occurredAt) {

    public static AuditView from(AuditLog a) {
        return new AuditView(a.getId(), a.getEventType(), a.getApplicationId(), a.getDetail(), a.getOccurredAt());
    }
}
