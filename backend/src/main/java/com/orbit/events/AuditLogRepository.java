package com.orbit.events;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    boolean existsByEventId(UUID eventId);
    List<AuditLog> findTop50ByOwnerIdOrderByOccurredAtDesc(UUID ownerId);
}
