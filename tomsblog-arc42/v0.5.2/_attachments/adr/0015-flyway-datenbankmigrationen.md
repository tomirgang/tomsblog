# ADR-0015: Flyway für Datenbank-Migrationen

## Status

Accepted

## Context

Der Blog Content Service nutzt PostgreSQL als primäre Datenbank. Bisher wurde das Schema über Hibernate's `ddl-auto: update` (lokal) bzw. `create-drop` (Tests) verwaltet. Dieses Vorgehen hat folgende Probleme:

- Keine reproduzierbaren, versionierten Schema-Änderungen
- Kein Audit-Trail über Schema-Entwicklung
- Ungeeignet für Produktionsbetrieb (Datenverlust bei `create-drop`, unvorhersehbare Änderungen bei `update`)
- Kein Rollback-Mechanismus

Alternativen:
- **Hibernate ddl-auto**: einfach, aber für Produktion ungeeignet
- **Liquibase**: XML/YAML/JSON-basiert, mächtig aber komplex
- **Flyway**: SQL-basiert, einfach, konventionsgetrieben, gut in Spring Boot integriert

## Decision

Wir verwenden **Flyway** für alle Datenbank-Schema-Migrationen.

### Konventionen

- Migrationsdateien liegen unter `src/main/resources/db/migration/`
- Benennung: `V{nummer}__{beschreibung}.sql` (z.B. `V1__create_posts_schema.sql`)
- Jede Migration ist eine atomare, idempotente Änderung
- Hibernate `ddl-auto` wird auf `validate` gesetzt (Schema-Prüfung ohne Änderung)
- Flyway wird in Tests über dynamische Properties aktiviert (`spring.flyway.enabled=true`)

### Aktuelle Migrationen

| Migration | Beschreibung |
|-----------|-------------|
| V1__create_posts_schema.sql | posts, post_tags, post_sources, post_attachments |
| V2__create_tags_schema.sql | tags mit Unique-Index auf (tenant_id, name) und (tenant_id, slug) |
| V3__create_translations_schema.sql | translations mit FK auf posts, Unique auf (post_id, locale) |

### Spring-Konfiguration

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
    locations: classpath:db/migration
```

## Consequences

**Positiv:**

- Reproduzierbare Schema-Änderungen in allen Umgebungen
- Versionskontrolle über Git (Schema-History nachvollziehbar)
- Automatische Migration beim Service-Start
- Validierung durch Hibernate stellt Konsistenz zwischen Entity-Klassen und Schema sicher
- Testcontainers-kompatibel (frisches Schema pro Test-Run)

**Negativ:**

- Zusätzliche Abhängigkeiten (`flyway-core`, `flyway-database-postgresql`)
- Migrationen müssen manuell geschrieben werden (kein Auto-Generate)
- Rollbacks erfordern eigene Migrations-Skripte (Flyway Community hat kein `undo`)

## References

- SWR-001: Post CRUD API (Schema für Posts)
- SWR-003: Tenant-Isolation (tenant_id Spalten)
- SWA-004: Spring Boot Applikation
- ADR-0002: Java und Maven
