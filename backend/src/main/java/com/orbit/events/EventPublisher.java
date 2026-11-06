package com.orbit.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;
    private final String topic;

    public EventPublisher(KafkaTemplate<String, String> kafka, ObjectMapper mapper,
                          @Value("${orbit.kafka.topic}") String topic) {
        this.kafka = kafka;
        this.mapper = mapper;
        this.topic = topic;
    }

    /**
     * Keyed by owner so one user's events stay ordered within a partition.
     * Known limitation: publishing is not atomic with the DB write (see ADR-001, transactional outbox).
     */
    public void publish(ApplicationEvent event) {
        try {
            kafka.send(topic, event.ownerId().toString(), mapper.writeValueAsString(event))
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            log.error("Failed to publish {} event {}", event.type(), event.eventId(), error);
                        }
                    });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialise event", e);
        } catch (RuntimeException e) {
            log.error("Kafka unavailable, event {} not published", event.eventId(), e);
        }
    }
}
