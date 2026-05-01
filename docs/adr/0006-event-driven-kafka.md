# ADR-0006: Event-Driven Architecture mit Apache Kafka

## Status

Superseded by [ADR-0018](0018-dual-broker-kafka-rabbitmq.md)

## Context

Die Microservices der Plattform müssen asynchron kommunizieren. Bestimmte Aktionen (Post veröffentlicht) lösen Folgeprozesse aus:
- Feed-Generierung
- Übersetzung (KI)
- Podcast-Generierung (TTS)
- Web-Snapshot-Erstellung

Wir brauchen einen Mechanismus für lose Kopplung zwischen Services.

## Decision

Wir verwenden **Apache Kafka** als Event-Broker für die asynchrone Service-Kommunikation.

**Event-Design:**
- Events als Java Records in `libs/event-contracts/` (geteiltes Schema)
- Jedes Event trägt `EventMetadata` (eventId, occurredAt, tenantId, correlationId)
- Event-Typen: `post.created`, `post.updated`, `post.published`
- Services produzieren Events nach erfolgreicher Domain-Operation
- Consumer-Services reagieren unabhängig auf Events

## Consequences

**Positiv:**
- Lose Kopplung: Producer kennt Consumer nicht
- Replay-Fähigkeit: Events können wiederholt werden
- Skalierbarkeit: Consumer können unabhängig skalieren
- Audit-Trail: Kafka als Event-Log

**Negativ:**
- Eventual Consistency statt sofortige Konsistenz
- Komplexeres Debugging (verteilte Traces nötig → Tempo)
- Infrastruktur-Overhead (Kafka-Cluster betreiben → Strimzi Operator)
- Schema-Evolution muss bewusst gemanagt werden
