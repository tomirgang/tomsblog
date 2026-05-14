# ADR-0028: Multi-Tenant-Isolation auf Datenebene

## Status

Accepted

## Context

Unsere Plattform ist multi-tenant-fähig (STK-002). Jeder Service besitzt eine eigene Datenbank (ADR-0019). Innerhalb dieser Datenbank muss sichergestellt werden, dass Tenant-Daten voneinander isoliert sind. Drei gängige Ansätze existieren:

1. **Schema-per-Tenant**: Jeder Tenant erhält ein eigenes Datenbank-Schema. Queries laufen immer gegen das jeweilige Schema.
2. **Row-Level Security (RLS)**: Ein gemeinsames Schema mit einer `tenant_id`-Spalte. PostgreSQL erzwingt auf DB-Ebene, dass Queries nur Zeilen des aktiven Tenants sehen.
3. **Shared Schema mit Application-Level Filtering**: Ein gemeinsames Schema mit einer `tenant_id`-Spalte. Die Anwendung filtert in allen Queries nach `tenant_id`.

### Bewertung

| Kriterium                  | Schema-per-Tenant       | RLS (DB-enforced)                | Shared Schema + App Filtering    |
| -------------------------- | ----------------------- | -------------------------------- | -------------------------------- |
| Isolationsstärke           | Sehr hoch (physisch)    | Hoch (DB-enforced)               | Mittel (App-enforced)            |
| Komplexität Migrationen    | Hoch (pro Schema)       | Mittel (Policy-Management)       | Gering (eine Migration)          |
| Komplexität Connection-Mgmt| Hoch (Schema-Switching) | Gering (Session-Variable)        | Gering (keine)                   |
| Performance                | Gut (wenige Zeilen)     | Gut (Index + Filter)             | Gut (Index + Filter)             |
| CloudNativePG-Kompatibilität| Eingeschränkt          | Voll                             | Voll                             |
| Onboarding neuer Tenants   | Aufwändig (DDL nötig)   | Einfach (INSERT)                 | Einfach (INSERT)                 |
| Testbarkeit                | Aufwändig               | Gut                              | Sehr gut                         |
| Hexagonale Architektur     | Leak in Adapter         | Leak in DB-Config                | Sauber im Domain/App Layer       |

**Schema-per-Tenant** scheidet aus: CloudNativePG verwaltet einen PostgreSQL-Cluster pro Service. Dynamisches Schema-Management zur Laufzeit wäre ein erheblicher Infrastruktur-Aufwand und bricht mit dem Flyway-Migrationsmodell.

**RLS** bietet zusätzliche Sicherheit auf DB-Ebene, erfordert aber Session-Variable-Management (`SET app.current_tenant = ...`) in jedem Connection-Setup und erschwert Testcontainers-basierte Tests, wo Policies separat geprüft werden müssen.

**Shared Schema + App Filtering** ist der einfachste Ansatz und passt zur hexagonalen Architektur: Die Isolation wird durch die Application-Layer-Ports erzwungen (`findAllByTenantId`, `findByIdAndTenantId`). Die Repository-Interfaces definieren TenantId als Pflichtparameter.

## Decision

Wir verwenden **Shared Schema mit Application-Level Filtering** für die Tenant-Isolation innerhalb jeder Service-Datenbank.

### Regeln

1. **Jede Domain-Entity trägt eine `TenantId`** (SWR-003).
2. **Alle Repository-Port-Methoden** enthalten `TenantId` als Pflichtparameter. Es gibt keine Methode, die tenant-übergreifend Daten liefert (Ausnahme: SuperAdmin-Operationen mit expliziter Kennzeichnung).
3. **Datenbank-Indexes** auf `(tenant_id, ...)` für alle häufig abgefragten Tabellen.
4. **Integrationstests** verifizieren Tenant-Isolation: Test mit zwei Tenants prüft, dass Tenant A keine Daten von Tenant B sieht.
5. **Kein direkter DB-Zugriff** zwischen Services (ADR-0019). Jeder Service ist für seine eigene Isolation verantwortlich.

### Optionale Härtung (Phase 2)

PostgreSQL Row-Level Security kann zu einem späteren Zeitpunkt als zusätzliche Absicherungsschicht aktiviert werden. Dies ist eine Defense-in-Depth-Maßnahme und ändert nichts am primären Isolationsmechanismus im Application Layer.

## Consequences

**Positiv:**
- Einfaches Flyway-Migrationsmodell (ein Schema pro Service)
- Kein Schema-Switching oder Session-Variable-Management nötig
- Repository-Interfaces erzwingen Tenant-Filterung bereits auf Architekturebene
- Einfache Testbarkeit mit Testcontainers
- CloudNativePG-kompatibel ohne Einschränkungen
- Onboarding neuer Tenants erfordert nur Daten-INSERTs

**Negativ:**
- Ein Bug im Application Layer kann zu Tenant-übergreifendem Datenzugriff führen
- Keine DB-seitige letzte Verteidigungslinie (bis RLS nachgerüstet wird)
- Entwickler müssen diszipliniert TenantId in allen Queries verwenden

**Mitigationen:**
- Repository-Port-Interfaces erzwingen TenantId als Parameter (Compiletime-Safety)
- ArchUnit-Tests prüfen, dass kein Repository-Methode ohne TenantId existiert
- Integrationstests mit Multi-Tenant-Szenarien in jedem Service

## References

- SWR-003: Tenant-Isolation auf Datenebene
- ADR-0012: Domain-basierte Tenant-Auswahl
- ADR-0019: Database per Service
- STK-002: Multi-Tenant-Fähigkeit
