# ADR-0018: Dual-Broker-Strategie mit Kafka und RabbitMQ

## Status

Accepted (supersedes ADR-0006)

## Context

ADR-0006 legte Apache Kafka als einzigen Event-Broker für alle asynchronen Kommunikationsszenarien fest. In der Praxis zeigen sich jedoch unterschiedliche Anforderungsprofile:

1. **Event-Streaming**: Domain Events (PostCreated, PostPublished etc.) bilden einen unveränderlichen Event-Strom, der als Source of Truth dient. Replay-Fähigkeit und Log-Semantik sind hier essenziell.
2. **Task-Verteilung**: Job-Queues (z.B. Übersetzungsaufträge, Podcast-Generierung, Snapshot-Erstellung) benötigen klassische Queue-Semantik mit Acknowledgment, Retry, Dead-Letter-Handling und komplexem Routing.

Ein einzelner Broker deckt beide Anforderungsprofile nur mit Kompromissen ab. Die bewusste Trennung in zwei spezialisierte Systeme vereinfacht Betrieb und Entwicklung.

## Decision

Wir verwenden **zwei Message-Broker** mit klar getrennten Verantwortlichkeiten:

### Apache Kafka (Event-Streaming)

Kafka bleibt der zentrale Event-Broker für:

- **Event Sourcing**: Domain Events als unveränderlicher Log
- **Log Aggregation**: Zentrale Sammlung aller Ereignisse
- **Stream Processing**: Kafka Streams oder Flink für Echtzeitverarbeitung
- **Datenpipelines**: Systemübergreifende Datenflüsse
- **Replay-Szenarien**: Wiederholbare Verarbeitung des Event-Stroms
- **Audit-Trail**: Der Event-Strom selbst ist die Wahrheit

**Konfiguration:** KRaft-Mode, Topics pro Event-Typ, Consumer Groups für unabhängige Verarbeitung.

### RabbitMQ (Task-Queues und Service-Kommunikation)

RabbitMQ übernimmt:

- **Task-Queues**: Arbeitsaufträge mit garantierter Zustellung
- **Job-Verteilung**: Work-Queue-Pattern mit konkurrierenden Consumern
- **Request/Reply-Patterns**: Synchrone Kommunikation über Messages
- **Service-zu-Service-Kommunikation**: Klassische point-to-point oder pub/sub Muster
- **Komplexes Routing**: Exchange-basiertes Routing (Topic, Headers, Fanout)
- **Überschaubarer Durchsatz**: Szenarien, bei denen Queue-Semantik wichtiger ist als Streaming

**Konfiguration:** Quorum Queues für Durability, Dead-Letter-Exchanges für Fehlerbehandlung, Prefetch-Limit für faire Verteilung.

### Abgrenzung

| Kriterium               | Kafka                          | RabbitMQ                         |
| ----------------------- | ------------------------------ | -------------------------------- |
| Semantik                | Event-Log (append-only)        | Message-Queue (consume & ack)    |
| Nachricht nach Konsum   | Bleibt erhalten (Retention)    | Wird entfernt (nach Ack)         |
| Replay                  | Ja                             | Nein                             |
| Routing-Komplexität     | Einfach (Topic-basiert)        | Hoch (Exchanges, Bindings)       |
| Reihenfolge             | Pro Partition garantiert       | Pro Queue garantiert             |
| Typische Nachrichtgröße | Klein bis mittel               | Beliebig (mit Limits)            |
| Use Cases hier          | Domain Events, Audit, Feeds    | Jobs, Übersetzung, TTS, Snapshots|

### Beispielhafte Zuordnung

**Kafka-Topics:**
- `post.created`, `post.updated`, `post.published`
- `comment.created`, `comment.moderated`
- `tenant.created`

**RabbitMQ-Queues:**
- `translation.requests` (KI-Übersetzungsaufträge)
- `podcast.generation` (TTS-Aufträge)
- `snapshot.requests` (Web-Snapshot-Erstellung)
- `notification.send` (E-Mail/Push-Benachrichtigungen)

## Consequences

**Positiv:**

- Jeder Broker wird für seine Stärken eingesetzt
- Einfacheres mentales Modell: Events vs. Jobs klar getrennt
- Kafka-Consumer können Events jederzeit replaying (Audit, Recovery)
- RabbitMQ bietet out-of-the-box: Retry, DLQ, Priority, TTL
- Spring Boot hat exzellente Unterstützung für beide (spring-kafka, spring-amqp)

**Negativ:**

- Zwei Messaging-Systeme zu betreiben (höherer Infrastruktur-Aufwand)
- Entwickler müssen wissen, wann welcher Broker einzusetzen ist (klare Richtlinie nötig)
- Monitoring für zwei Systeme (Kafka-UI + RabbitMQ Management)
- Schema-Evolution in beiden Systemen separat zu managen

## Mitigations

- Docker Compose enthält beide Broker für lokale Entwicklung
- Klare Namenskonvention trennt Kafka-Topics (`domain.event`) von RabbitMQ-Queues (`service.task`)
- Gemeinsame Event-Contracts-Library bleibt Broker-agnostisch
- Strimzi (Kafka) und RabbitMQ Cluster Operator für Kubernetes-Betrieb
- RabbitMQ Management UI (Port 15672) und Kafka UI (Port 8080) für Monitoring
