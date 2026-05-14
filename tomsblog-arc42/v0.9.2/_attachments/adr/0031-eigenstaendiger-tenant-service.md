# ADR-0031: Eigenständiger Tenant-Management-Service

## Status

Proposed

## Context

Die Plattform ist multi-tenant-fähig (STK-002). Aktuell sind Tenant-bezogene Konzepte über mehrere Stellen verteilt:

1. **Shared Kernel**: `TenantId` als Value Object (`libs/shared-kernel`)
2. **user-management**: `TenantSettings` (Login-Mode, Branding, OIDC-Config, Impressum, Datenschutzerklärung), `TenantMembership` (User-Tenant-Zuordnung mit Rolle), `TenantSettingsUseCase`, `TenantSettingsRepository`
3. **blog-content**: `DefaultTenantFilter` als provisorischer Workaround, der einen Default-`X-Tenant-Id`-Header injiziert

Was fehlt:

- **Tenant-Aggregate**: Es gibt keine `Tenant`-Entity mit Lifecycle (erstellen, aktivieren, deaktivieren, löschen)
- **Domain-Mapping**: Das in ADR-0012 definierte Domain-zu-Tenant-Mapping (FQDN, is_primary, verified) ist nicht implementiert
- **Domain-Resolution**: Die Auflösung Host-Header zu TenantId (ADR-0012) fehlt. Der `DefaultTenantFilter` im blog-content ist ein expliziter Platzhalter
- **Tenant-Onboarding**: Kein Prozess zum Anlegen neuer Tenants mit DNS-Verifikation

Die Frage ist, ob diese Verantwortlichkeiten in den bestehenden user-management Service integriert oder in einen eigenständigen Service ausgelagert werden sollen.

### Bewertung

| Kriterium                      | Im user-management                          | Eigenständiger tenant-management              |
| ------------------------------ | ------------------------------------------- | --------------------------------------------- |
| Bounded Context Kohäsion       | Mischung aus Identity und Tenant-Verwaltung | Klare Domänentrennung                         |
| Unabhängiges Deployment        | Tenant-Änderungen erfordern User-Deployment | Unabhängig deploybar                          |
| Skalierbarkeit                 | Domain-Resolution skaliert mit User-Service | Domain-Resolution unabhängig skalierbar       |
| Domain-Resolution Performance  | Hot Path koppelt an User-Service-Last       | Isoliert, optimierbar (Cache, Replikation)    |
| Komplexität                    | Weniger Services, weniger Netzwerk-Hops     | Zusätzlicher Service und Datenbank            |
| DDD-Konformität                | Tenant ist kein User-Subdomain-Konzept      | Eigene Bounded Context                        |
| Evolutionsfähigkeit            | Refactoring bei wachsender Komplexität      | Von Anfang an sauber getrennt                 |

**Tenant-Verwaltung** (Lifecycle, Domain-Mapping, DNS-Verifikation, Konfiguration) und **User-Verwaltung** (Profile, Authentifizierung, Rollen) sind unterschiedliche Bounded Contexts. Ein Tenant existiert unabhängig von einzelnen Benutzern. Die Domain-Resolution ist ein Hot Path, der bei jedem eingehenden Request durchlaufen wird und unabhängig von der User-Service-Last performant bleiben muss.

## Decision

Wir führen einen eigenständigen **tenant-management** Service ein (`services/tenant-management/`).

### Verantwortlichkeiten des tenant-management Service

| Verantwortlichkeit             | Beschreibung                                                              |
| ------------------------------ | ------------------------------------------------------------------------- |
| **Tenant-Lifecycle**           | Erstellen, Aktivieren, Deaktivieren, Löschen von Tenants                 |
| **Domain-Mapping**             | CRUD für Domain-zu-Tenant-Zuordnungen (ADR-0012)                         |
| **DNS-Verifikation**           | Verifizierung von Custom Domains via DNS TXT Record                       |
| **Tenant-Konfiguration**       | Login-Mode, Auto-Approval, Branding, Impressum, Datenschutzerklärung     |
| **OIDC-Provider-Config**       | Tenant-spezifische OIDC-Konfiguration (Issuer, Client-ID, Client-Secret) |
| **Domain-Resolution**          | Auflösung Host-Header zu TenantId (gRPC-Endpunkt für API Gateway)        |

### Domain-Modell

**Tenant** (Aggregate Root):

| Feld                    | Typ             | Beschreibung                                      |
| ----------------------- | --------------- | ------------------------------------------------- |
| tenantId                | TenantId (UUID) | Eindeutige Kennung                                |
| slug                    | String          | URL-freundlicher Kurzname (eindeutig)             |
| displayName             | String          | Anzeigename des Tenants                           |
| tagline                 | String          | Optionaler Untertitel                             |
| status                  | TenantStatus    | ACTIVE, SUSPENDED, DEACTIVATED                    |
| loginMode               | LoginMode       | INTERNAL, OIDC, BOTH                              |
| autoApproveOidc         | boolean         | OIDC-Nutzer automatisch freischalten              |
| autoApproveEmailDomains | Set\<String\>   | E-Mail-Domains für Auto-Approval                  |
| impressumContent        | String          | Impressum (Markdown)                              |
| privacyPolicyContent    | String          | Datenschutzerklärung (Markdown)                   |
| oidcIssuerUrl           | String          | Tenant-spezifischer OIDC Issuer                   |
| oidcClientId            | String          | OIDC Client-ID                                    |
| oidcClientSecret        | String          | OIDC Client-Secret (verschlüsselt gespeichert)    |
| createdAt               | Instant         | Erstellungszeitpunkt                              |
| updatedAt               | Instant         | Letzter Änderungszeitpunkt                        |

**TenantDomain** (Entity, Teil des Tenant-Aggregates):

| Feld              | Typ             | Beschreibung                              |
| ----------------- | --------------- | ----------------------------------------- |
| domainId          | DomainId (UUID) | Eindeutige Kennung                        |
| domain            | String          | FQDN (global eindeutig)                  |
| isPrimary         | boolean         | Primärdomain für Canonical-URLs           |
| verified          | boolean         | DNS-Verifikation abgeschlossen            |
| verificationToken | String          | DNS TXT Record Token für Verifikation     |

### Was bleibt im user-management Service

- `TenantMembership` (Zuordnung User zu Tenant mit Rolle) bleibt im user-management, da es ein User-Konzept ist
- `UserProfile`, Rollen, Authentifizierung bleiben unverändert
- user-management konsumiert `TenantCreated`/`TenantDeactivated` Events, um zu wissen, welche Tenants existieren

### Was wird migriert

Die folgenden Artefakte werden aus dem user-management in den tenant-management Service verschoben:

- `TenantSettings` (wird zum `Tenant`-Aggregate erweitert)
- `TenantSettingsUseCase` (wird zu `TenantManagementUseCase`)
- `TenantSettingsRepository` (wird zu `TenantRepository`)
- `TenantSettingsService` (wird zu `TenantManagementService`)
- `LoginMode` (Enum)
- Zugehörige REST-Endpunkte und Persistence-Adapter

### Technische Umsetzung

| Aspekt                 | Entscheidung                                                             |
| ---------------------- | ------------------------------------------------------------------------ |
| Datenbank              | Eigene PostgreSQL-Instanz (ADR-0019), CloudNativePG (ADR-0023)          |
| Migrationen            | Flyway (ADR-0015)                                                        |
| Architektur            | Hexagonal (ADR-0001): domain/, application/, adapter/                    |
| Sync-Kommunikation     | gRPC (ADR-0010) für Domain-Resolution und Tenant-Abfragen               |
| Async-Kommunikation    | Kafka (ADR-0018) für Domain Events (TenantCreated, TenantUpdated, etc.) |
| Tenant-Isolation       | Entfällt: Der Tenant-Service verwaltet alle Tenants (SuperAdmin-Zugriff) |
| API (extern)           | REST für Admin-Oberfläche (Tenant-CRUD, Domain-Verwaltung)              |

### Domain-Resolution Flow

```
Browser ──► API Gateway (Spring Cloud Gateway)
                │
                │  1. Host-Header extrahieren
                │  2. gRPC-Call: ResolveDomain(host) → TenantId
                │     (mit lokalem Cache, TTL 60s)
                │  3. X-Tenant-Id Header setzen
                │
                ├──► blog-content (mit X-Tenant-Id)
                ├──► user-management (mit X-Tenant-Id)
                └──► weitere Services (mit X-Tenant-Id)
```

Der `DefaultTenantFilter` im blog-content wird obsolet und entfernt, sobald die Domain-Resolution im API Gateway aktiv ist.

### Domain Events

| Event                  | Payload                     | Konsumenten                       |
| ---------------------- | --------------------------- | --------------------------------- |
| TenantCreated          | tenantId, slug, displayName | user-management, blog-content     |
| TenantActivated        | tenantId                    | API Gateway (Cache invalidieren)  |
| TenantSuspended        | tenantId                    | API Gateway, alle Services        |
| TenantDeactivated      | tenantId                    | API Gateway, alle Services        |
| TenantSettingsUpdated  | tenantId, geänderte Felder  | user-management (Login-Mode-Sync) |
| TenantDomainAdded      | tenantId, domain, isPrimary | API Gateway (Cache invalidieren)  |
| TenantDomainVerified   | tenantId, domain            | API Gateway                       |
| TenantDomainRemoved    | tenantId, domain            | API Gateway (Cache invalidieren)  |

### gRPC-Service-Definition (Entwurf)

```protobuf
service TenantResolution {
  rpc ResolveDomain(ResolveDomainRequest) returns (ResolveDomainResponse);
  rpc GetTenant(GetTenantRequest) returns (TenantInfo);
  rpc GetTenantSettings(GetTenantSettingsRequest) returns (TenantSettingsInfo);
}
```

Die Proto-Datei wird in `libs/grpc-contracts/` gepflegt (ADR-0010).

## Consequences

**Positiv:**

- Klare Bounded Context Trennung: Tenant-Verwaltung ist eine eigenständige Domäne
- Domain-Resolution ist unabhängig skalierbar und optimierbar (eigener Cache, eigene Replicas)
- Tenant-Lifecycle (erstellen, suspendieren, deaktivieren) ist sauber modellierbar
- user-management wird schlanker und fokussiert sich auf Identity & Access
- Domain-Mapping und DNS-Verifikation haben einen klaren Owner
- Neue Services müssen nur den `X-Tenant-Id`-Header lesen, die Auflösung ist zentral im Gateway
- Unabhängiges Deployment: Tenant-Konfigurationsänderungen erfordern kein User-Service-Deployment

**Negativ:**

- Zusätzlicher Service mit eigener Datenbank, eigenen Migrationen, eigenem Deployment
- user-management muss `TenantSettings`-bezogene Logik abgeben (Migration bestehenden Codes)
- Inter-Service-Kommunikation: user-management muss per gRPC oder Kafka-Event prüfen, ob ein Tenant existiert
- Domain-Resolution wird zum kritischen Pfad (Mitigation: Caching im API Gateway)
- Komplexeres lokales Entwicklungssetup (ein Service mehr in Docker Compose)

**Mitigationen:**

- API Gateway cached Domain-Resolution-Ergebnisse (TTL-basiert + Event-basierte Invalidierung)
- Der tenant-management Service publiziert alle Zustandsänderungen als Kafka Events
- Lokale Entwicklung: Default-Tenant bleibt konfigurierbar für Einzelservice-Starts ohne Gateway
- ArchUnit-Tests stellen sicher, dass kein Service direkt auf die Tenant-Datenbank zugreift

## References

- STK-002: Multi-Tenant-Fähigkeit
- SWR-003: Tenant-Isolation auf Datenebene
- SWR-017: Domain-basierte Tenant-Auswahl
- SWR-044: Tenant-spezifische Login-Konfiguration
- SWR-045: Auto-Approval-Regeln
- SWR-050: Tenant-Branding
- SWR-061: OIDC-Konfiguration pro Tenant
- ADR-0001: Hexagonale Architektur
- ADR-0010: gRPC für synchrone Service-Kommunikation
- ADR-0012: Domain-basierte Tenant-Auswahl
- ADR-0015: Flyway für Datenbank-Migrationen
- ADR-0017: API Gateway (Spring Cloud Gateway)
- ADR-0018: Dual-Broker-Strategie (Kafka + RabbitMQ)
- ADR-0019: Database per Service
- ADR-0023: CloudNativePG für PostgreSQL
- ADR-0028: Multi-Tenant-Isolation auf Datenebene
