# ADR-0008: Shared Kernel als Framework-freie Library

## Status

Accepted

## Context

Mehrere Microservices teilen gemeinsame Konzepte: Entity-IDs, Audit-Felder, Tenant-Kontext, Domain-Event-Interface. Diese müssen wiederverwendbar sein, ohne Framework-Abhängigkeiten in die Domain-Schicht zu ziehen.

## Decision

Die Library `libs/shared-kernel` enthält reine Java-Klassen ohne jegliche Framework-Abhängigkeit:

| Klasse          | Zweck                                                    |
| --------------- | -------------------------------------------------------- |
| `EntityId`      | Interface für typsichere IDs (implementiert als Records) |
| `AggregateRoot` | Basis-Klasse mit Domain-Event-Sammlung                   |
| `DomainEvent`   | Interface für Event-Metadaten                            |
| `TenantId`      | Typsicherer Tenant-Identifier                            |
| `Auditable`     | Interface für Audit-Felder                               |

**Regeln:**
- Keine `org.springframework.*` Imports erlaubt
- Keine `jakarta.*` Imports erlaubt
- Nur Java Standard Library
- Value Objects als Records

## Consequences

**Positiv:**
- Domain-Schicht aller Services bleibt framework-frei
- Trivial testbar (kein ApplicationContext nötig)
- Wiederverwendbar auch außerhalb von Spring (z.B. CLI-Tools)
- Erzwingt saubere Architektur-Grenzen

**Negativ:**
- Mapping-Aufwand: Domain-Entities ↔ JPA-Entities in jedem Service
- Keine Spring-Magic (kein `@Component`, kein Auto-Wiring) in der Library
- Services müssen eigene Adapter für Persistence schreiben
