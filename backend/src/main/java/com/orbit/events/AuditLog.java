package com.orbit.events;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    private UUID id;
    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;
    @Column(name = "event_type", nullable = false)
    private String eventType;
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;
    @Column(name = "application_id")
    private UUID applicationId;
    private String detail;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AuditLog() {}

    public AuditLog(ApplicationEvent e) {
        this.id = UUID.randomUUID();
        this.eventId = e.eventId();
        this.eventType = e.type();
        this.ownerId = e.ownerId();
        this.applicationId = e.applicationId();
        this.detail = e.detail();
        this.occurredAt = e.occurredAt();
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public UUID getOwnerId() { return ownerId; }
    public UUID getApplicationId() { return applicationId; }
    public String getDetail() { return detail; }
    public Instant getOccurredAt() { return occurredAt; }
}
