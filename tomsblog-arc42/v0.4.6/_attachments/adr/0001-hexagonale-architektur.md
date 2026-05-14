# ADR-0001: Hexagonale Architektur (Ports & Adapters)

## Status

Accepted

## Context

Wir bauen eine Microservice-Plattform als Lernprojekt. Jeder Service soll unabhängig testbar, austauschbar in seinen technischen Abhängigkeiten und langfristig wartbar sein. Wir brauchen eine klare Trennung zwischen Geschäftslogik und technischer Infrastruktur.

Alternativen:
- **Layered Architecture**: einfach, aber fördert Durchgriffe (Repositories direkt im Controller)
- **Clean Architecture**: ähnlich, aber mit mehr Abstraktionsebenen als nötig
- **Hexagonale Architektur**: klare Ports & Adapters, gut für DDD und Testbarkeit

## Decision

Jeder Microservice folgt der hexagonalen Architektur mit folgender Paketstruktur:

```
domain/          # Entities, Value Objects, Domain Events, Domain Services
application/     # Use Cases, Port Interfaces (inbound + outbound)
adapter/
  inbound/       # REST Controller, Event Listener, CLI
  outbound/      # JPA Repository, Kafka Producer, HTTP Client
```

**Regeln:**
- `domain/` darf keine Framework-Imports enthalten (kein Spring, kein JPA, kein Kafka)
- `application/` definiert Port-Interfaces, die von `adapter/outbound/` implementiert werden
- `adapter/inbound/` ruft Use Cases aus `application/` auf
- Dependency-Richtung: adapter → application → domain

## Consequences

**Positiv:**
- Domain-Logik ist framework-unabhängig und trivial testbar (Plain Unit Tests)
- Technische Adapter können ausgetauscht werden ohne Domain-Änderungen
- Erzwingt bewusstes Dependency-Management

**Negativ:**
- Mehr Interfaces/Klassen als bei flacher Schichtung
- Mapping zwischen Domain-Objekten und Persistence-Entities nötig
- Lernkurve für Entwickler, die nur Layered Architecture kennen
