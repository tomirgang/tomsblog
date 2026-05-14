# ADR-0010: gRPC für synchrone Service-Kommunikation

## Status

Accepted

## Context

Die Plattform verwendet Kafka für asynchrone, event-basierte Kommunikation zwischen Services (siehe ADR-0006). Einige Szenarien erfordern jedoch synchrone Aufrufe zwischen Microservices:

- Ein Service benötigt Daten eines anderen Service in Echtzeit (z.B. Autor-Informationen vom Auth-Service)
- Request-Response-Muster, bei denen der Aufrufer auf das Ergebnis warten muss
- Service-to-Service-Queries, die nicht über Events abgebildet werden können

REST wäre eine Option, bietet aber keine typsicheren Contracts auf Schema-Ebene und erfordert manuell gepflegte Clients.

## Decision

Wir verwenden **gRPC** für die synchrone Kommunikation zwischen Microservices.

**Abgrenzung der Kommunikationsprotokolle:**

| Kommunikationsart         | Protokoll | Zweck                                |
| ------------------------- | --------- | ------------------------------------ |
| Client → Service (extern) | REST      | Text-Blog APIs (Thymeleaf, Angular)  |
| Client → Service (extern) | GraphQL   | Podcast/Video APIs (React)           |
| Service → Service (sync)  | gRPC      | Synchrone Abfragen zwischen Services |
| Service → Service (async) | Kafka     | Event-basierte lose Kopplung         |

**Umsetzung:**
- Service-Interfaces werden als `.proto`-Dateien definiert
- Proto-Dateien werden in einer geteilten Library (`libs/grpc-contracts/`) verwaltet
- Java-Code wird via `protobuf-maven-plugin` generiert
- Spring Boot Integration über `grpc-spring-boot-starter`
- Deadline/Timeout-Propagation für alle gRPC-Calls

## Consequences

**Positiv:**
- Typsichere Contracts via Protobuf (Compile-Time-Fehler bei Breaking Changes)
- Effizientes Binary-Protokoll (HTTP/2, geringer Overhead)
- Native Streaming-Unterstützung (Server-Streaming, Bidirectional)
- Code-Generierung für Client und Server aus einer Quelle
- Schema-Evolution mit Protobuf-Kompatibilitätsregeln

**Negativ:**
- Zusätzliche Toolchain (protoc, Protobuf-Plugin im Build)
- Nicht direkt im Browser testbar (kein curl wie bei REST)
- Stärkere Kopplung als Events (synchroner Aufruf = Abhängigkeit zur Laufzeit)
- Debugging erfordert spezielle Tools (grpcurl, Bloom RPC)

## References

- SWA-009: gRPC für synchrone Service-Kommunikation
- ADR-0004: REST und GraphQL Strategie
- ADR-0006: Event-Driven Architecture mit Kafka
