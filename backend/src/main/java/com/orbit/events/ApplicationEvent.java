package com.orbit.events;

import java.time.Instant;
import java.util.UUID;

/** The message published to Kafka whenever something happens to an application. */
public record ApplicationEvent(UUID eventId, String type, UUID ownerId, UUID applicationId,
                               String detail, Instant occurredAt) {

    public static ApplicationEvent of(String type, UUID ownerId, UUID applicationId, String detail) {
        String safe = detail != null && detail.length() > 480 ? detail.substring(0, 480) : detail;
        return new ApplicationEvent(UUID.randomUUID(), type, ownerId, applicationId, safe, Instant.now());
    }
}
