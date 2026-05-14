# ADR-0019: Database per Service

## Status

Accepted

## Context

In einer Microservice-Architektur teilen sich Services häufig eine gemeinsame Datenbank. Das führt zu enger Kopplung auf Datenebene: Schema-Änderungen eines Services können andere Services brechen, Performance-Probleme strahlen aus, und unabhängige Deployments werden erschwert.

Unsere Plattform setzt auf hexagonale Architektur (ADR-0001) und unabhängige Microservices (STK-009). Die Datenbank-Strategie muss diese Unabhängigkeit auf Persistenzebene widerspiegeln.

Zusätzlich haben verschiedene Services unterschiedliche Datenzugriffsprofile: Der Blog Content Service arbeitet mit strukturierten, relationalen Daten (Posts, Tags, Übersetzungen), während ein zukünftiger Kommentar-Service oder Media-Service möglicherweise besser mit einem Dokumentenmodell bedient wäre.

## Decision

Wir verwenden das **Database per Service**-Pattern:

### Grundregel

Jeder Microservice besitzt seine eigene, dedizierte Datenbank-Instanz. Kein Service darf direkt auf die Datenbank eines anderen Services zugreifen.

### Technologiewahl pro Service

Die Wahl der Datenbank-Technologie (PostgreSQL oder MongoDB) wird **pro Service** entschieden, basierend auf dem Datenmodell und den Zugriffsmustern:

| Kriterium                    | PostgreSQL                              | MongoDB                                  |
| ---------------------------- | --------------------------------------- | ---------------------------------------- |
| Datenmodell                  | Relationale, stark strukturierte Daten  | Dokumentenbasierte, flexible Schemas     |
| Konsistenz                   | ACID-Transaktionen                      | Eventual Consistency (default)           |
| Abfragen                     | Komplexe JOINs, Aggregationen           | Dokumentenabfragen, Aggregation Pipeline |
| Schema-Evolution             | Flyway-Migrationen                      | Schema-Versionierung im Code             |
| Typische Services            | Blog Content, Auth, Feed                | Kommentare, Media Metadata, Analytics    |

### Aktuelle Zuordnung

| Service       | Datenbank  | Begründung                                                    |
| ------------- | ---------- | ------------------------------------------------------------- |
| Blog Content  | PostgreSQL | Stark relationale Daten (Posts, Tags, Translations, Sources)  |
| Auth Service  | PostgreSQL | User/Role-Management mit referentieller Integrität            |

Weitere Services werden bei ihrer Erstellung zugeordnet.

### Isolation

Auch wenn mehrere Services PostgreSQL verwenden, erhält jeder eine **eigene Datenbank-Instanz** (eigener Container in Dev, eigener CloudNativePG-Cluster oder dedizierte Datenbank in Prod). Es gibt keine geteilten Schemas, keine Cross-Database-Queries.

### Datenzugriff zwischen Services

Wenn ein Service Daten eines anderen Services benötigt:

- **Synchron**: gRPC-Aufruf an den besitzenden Service (SWA-009)
- **Asynchron**: Domain Events über Kafka konsumieren (SWA-005)
- **Lokale Projektion**: Eventuelle Konsistenz durch lokale Read-Models (Event-basiert)

Direkter Datenbankzugriff über Service-Grenzen hinweg ist **verboten**.

## Consequences

**Positiv:**

- Volle Service-Unabhängigkeit: Schema-Änderungen betreffen nur den eigenen Service
- Unabhängige Skalierung der Datenbankschicht pro Service
- Technologie-Heterogenität: Jeder Service nutzt die optimale Datenbank
- Klare Ownership: Daten gehören einem Service, keine Ambiguität
- Unabhängige Deployments ohne Koordination der Datenbankschicht

**Negativ:**

- Mehr Datenbank-Instanzen zu betreiben (CloudNativePG Operator mildert das)
- Keine Cross-Service-JOINs (muss über APIs oder lokale Projektionen gelöst werden)
- Eventuelle Konsistenz zwischen Services (statt DB-Transaktionen)
- Höherer Ressourcenverbrauch im Cluster

## Mitigations

- CloudNativePG Operator verwaltet PostgreSQL-Instanzen deklarativ auf Kubernetes
- MongoDB Community Operator für MongoDB-Instanzen
- Lokale Entwicklung: Ein Docker-Container pro Service-Datenbank (Init-Scripts für Multi-DB-Setup)
- Shared Kernel (libs/shared-kernel) für gemeinsame Domain Primitives, nicht für Datenzugriff
- Konsistenz zwischen Services wird über Domain Events und lokale Read-Models sichergestellt
