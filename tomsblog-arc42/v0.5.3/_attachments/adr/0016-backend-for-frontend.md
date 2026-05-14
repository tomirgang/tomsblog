# ADR-0016: Backend for Frontend (BFF)

## Status

Accepted

## Context

Die Plattform wird von verschiedenen Client-Typen konsumiert:

- Server-Side Rendered Thymeleaf-UI (Meilenstein 1)
- Angular SPA (Meilenstein 3)
- React SPA für Podcast/Video (Meilensteine 4/5)
- Android-App für Post-Autoren (Meilenstein 7)

Jeder Client hat unterschiedliche Anforderungen an Datenformat, Aggregationstiefe und Authentifizierungsfluss. Ein generisches API-Design führt zu Kompromissen: Entweder werden zu viele Daten ausgeliefert (Over-fetching) oder der Client muss mehrere Aufrufe machen und selbst aggregieren (Under-fetching).

Das Backend for Frontend (BFF) Pattern löst dieses Problem, indem für jede Client-Kategorie ein dedizierter Backend-Layer existiert, der die Microservice-APIs optimal für den jeweiligen Client aufbereitet.

## Decision

Wir setzen das **Backend for Frontend (BFF) Pattern** ein.

### Architektur

- Jeder Client-Typ erhält einen dedizierten BFF-Service
- BFFs aggregieren Daten aus mehreren Microservices und liefern eine client-optimierte API
- BFFs übernehmen client-spezifische Authentifizierung (z.B. Session-basiert für Web, Token-basiert für Mobile)
- BFFs werden hinter dem API Gateway platziert

### Geplante BFFs

| BFF                | Client              | Protokoll   |
| ------------------ | ------------------- | ----------- |
| Web BFF            | Thymeleaf, Angular  | REST        |
| Mobile BFF         | Android-App         | REST        |
| Media BFF          | React Podcast/Video | GraphQL     |

### Regeln

- BFFs enthalten keine Geschäftslogik, nur Orchestrierung und Transformation
- BFFs kommunizieren mit den Domain-Services via gRPC (synchron) und Kafka (asynchron)
- BFFs sind zustandslos und horizontal skalierbar
- Jeder BFF ist ein eigenständiger Spring Boot Service

## Consequences

**Vorteile:**
- Optimale API pro Client ohne Kompromisse
- Unabhängige Weiterentwicklung pro Client-Team
- Client-spezifische Concerns (Caching, Pagination, Auth) isoliert
- Einfachere Versionierung pro Client

**Nachteile:**
- Mehr Services zu betreiben
- Potenzielle Code-Duplizierung zwischen BFFs (mitigiert durch Shared Libraries)
- Zusätzliche Netzwerk-Hops

## References

- Sam Newman: "Building Microservices" (BFF Pattern)
- ADR-0017: API Gateway
