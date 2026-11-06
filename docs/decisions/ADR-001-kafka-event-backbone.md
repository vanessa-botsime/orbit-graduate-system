# ADR-001: Use Kafka as the event backbone

**Status:** Accepted

## Context
Every change to an application (created, status changed, scored, deleted) is a fact other parts of the
system care about: the audit trail now, and later the data pipeline, the security monitor
(`orbit-sentinel`) and notifications.

## Decision
The API publishes an `ApplicationEvent` to the Kafka topic `orbit.application-events`, keyed by owner id.
Consumers are independent. The first one writes an idempotent audit log.

## Consequences
- New features (analytics, alerts, sentinel) subscribe without changing the API.
- Events for one user stay ordered, because they share a partition key.
- It is more infrastructure than a one-user app strictly needs. This is a deliberate learning and
  architecture choice, and the cost is accepted. Managed Kafka (AWS MSK) is expensive, so cloud
  deployment will run Kafka in a container or use a cheaper managed alternative.
- **Known gap:** the event is sent after the database write, not atomically with it. If the app crashes in
  between, an event can be lost. The fix is a transactional outbox table (on the task board).
