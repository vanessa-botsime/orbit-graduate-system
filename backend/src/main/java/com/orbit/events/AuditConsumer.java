package com.orbit.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Turns the event stream into an append-only audit trail. Idempotent: duplicates are ignored. */
@Component
public class AuditConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditConsumer.class);

    private final AuditLogRepository repo;
    private final ObjectMapper mapper;

    public AuditConsumer(AuditLogRepository repo, ObjectMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    @KafkaListener(topics = "${orbit.kafka.topic}")
    @Transactional
    public void onEvent(String payload) {
        try {
            ApplicationEvent event = mapper.readValue(payload, ApplicationEvent.class);
            if (!repo.existsByEventId(event.eventId())) {
                repo.save(new AuditLog(event));
            }
        } catch (JsonProcessingException e) {
            log.warn("Skipping malformed event payload", e);
        }
    }
}
