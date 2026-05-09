# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased]

### Added

- Kafka Event-Produktion für Blog-Posts: `KafkaEventPublisher` Outbound-Adapter sendet `PostCreatedEvent`, `PostUpdatedEvent`, `PostPublishedEvent` als JSON an Kafka-Topics. Domain-Events um `title`, `slug`, `locale` erweitert. `DomainEventMapper` mappt interne Domain-Events auf externe Contract-Events. Profil-basierte Steuerung (`kafka` Profil für Produktion, `LoggingEventPublisher` als Fallback). K8s-Deployment mit `SPRING_KAFKA_BOOTSTRAP_SERVERS` konfiguriert. (SWR-009, SWA-005, ADR-0018)
- RabbitMQ-Cluster-Konfiguration für Kubernetes: RabbitMQ Cluster Operator (Bitnami Helm Chart v4.4.34) via Flux HelmRelease, Single-Node-Cluster (RabbitMQ 4.x management-alpine), Management- und Prometheus-Plugin, Quorum-Queue-ready, 2Gi persistenter Storage (hcloud-volumes), Default-VHost "tomsblog", NetworkPolicies mit Default-Deny und Ausnahme für tomsblog-Namespace (INF-016, SWA-014, ADR-0026)
- Kafka-Cluster-Konfiguration für Kubernetes: Strimzi Operator (v0.45.0) via Flux HelmRelease, Single-Node KRaft-Cluster (Kafka 3.9.0), KafkaTopic-CRDs für post.created/post.updated/post.published, NetworkPolicies, Egress-Regel für tomsblog-Namespace (INF-015, SWA-005, ADR-0026)
- RabbitMQ Task-Verteilung: `TaskPublisher` Outbound Port mit `RabbitMqTaskPublisher` Adapter für asynchrone Arbeitsaufträge. Drei Queues (`translation.requests`, `tts.generation`, `snapshot.requests`) als Quorum Queues mit Dead-Letter-Exchange und DLQs. Topic Exchange `tomsblog.tasks` mit Routing Keys. `PostService` dispatcht `SnapshotTaskMessage` für alle Quellen beim Veröffentlichen. Profil-basierte Steuerung (`rabbitmq` Profil, `LoggingTaskPublisher` als Fallback). (SWR-089, SWA-037, ADR-0018)
- Feed Service als Kafka Event Consumer: Neuer Microservice `services/feed/` konsumiert `PostPublishedEvent` und `PostUpdatedEvent` via Kafka (Consumer Group `feed-service`). Hexagonale Architektur mit `FeedEntry` Domain-Entity, `FeedEntryUseCase` Inbound Port, `PostEventConsumer` Kafka-Adapter und JPA/PostgreSQL Persistence-Adapter. Eigene PostgreSQL-Instanz (Database per Service). Profil-basierte Steuerung (`kafka` Profil). (SWR-090, SWA-038, SWR-008)
- MongoDB-Cluster-Konfiguration für Kubernetes: MongoDB Community Operator (v0.11.0) via Flux HelmRelease, Single-Node ReplicaSet (MongoDB 7.0.17), SCRAM-Authentifizierung, 5Gi persistenter Storage (hcloud-volumes), NetworkPolicies mit Default-Deny und Ausnahme für tomsblog-Namespace (ADR-0026)

### Fixed

- `PostService.updatePost()` hat keine Domain-Events publiziert (fehlender `eventPublisher.publish()` Aufruf)

## [0.9.6] - 2026-05-09

### Added

- Deklarative Database CRDs für alle drei Service-Datenbanken (blog_content, user_management, tenant_management) in `infra/k8s/postgres/databases.yaml` (ADR-0029, INF-013, SWA-030)
- `databaseReclaimPolicy: retain` auf allen Database-Ressourcen zum Schutz vor versehentlichem Datenverlust

### Changed

- Rolling-Update-Strategie aller Service-Deployments auf `maxSurge: 0, maxUnavailable: 1` geändert, um Deadlocks bei knappen Cluster-Ressourcen zu vermeiden (blog-content, user-management, tenant-management)
- Cluster-Bootstrap-Datenbank auf neutralen Platzhalter `app` umgestellt (statt `blog_content`)
- `postInitApplicationSQL` aus der Cluster-Definition entfernt (durch Database CRDs ersetzt)

### Fixed

- Fehlendes `group: policy.linkerd.io` in der AuthorizationPolicy `allow-probes-to-tenant-management` ergänzt (blockierte Flux Kustomization-Reconciliation)
- Mermaid-Diagramme in Blog-Posts wurden nicht gerendert, weil Inline-Scripts von der Content-Security-Policy blockiert wurden. JavaScript in externe Dateien (`post-show.js`, `mermaid-init.js`) ausgelagert.

## [0.9.5] - 2026-05-09

## [0.9.4] - 2026-05-09

## [0.9.3] - 2026-05-09

### Added

- Umfangreiche Testabdeckung erhöht (alle Services auf 97+ % Branch Coverage)
  - blog-content: 100.0 % Instruction, 97.9 % Branch, 100.0 % Line Coverage
  - tenant-management: 99.7 % Instruction, 98.0 % Branch, 99.6 % Line Coverage
  - user-management: 99.8 % Instruction, 97.5 % Branch, 99.8 % Line Coverage
  - Neue Tests für: GlobalExceptionHandler, SecurityAuditorAware, WebExceptionHandler,
    GrpcExceptionInterceptor, PostFormData, DefaultTenantFilter, PostService,
    UserManagementClient, TenantSettingsService, TenantManagementService,
    BlogViewController (Serien-Navigation, Tag-Auflösung, Markdown-Excerpt),
    AuthAdminController (Domain-Parsing), JpaPostRepository, JpaUserProfileRepository,
    TenantSettingsJpaEntity, TenantSettingsGrpcService

- Auditlog-Ansicht in der Admin-UI (STK-054, SWR-087, SWR-088, SWA-036)
  - Paginierte Auflistung aller Audit-Einträge pro Tenant (neueste zuerst)
  - Filterung nach Aktionstyp, Entitätstyp und Akteur per Dropdown-Selektor
  - Zeitraum-Filter (von/bis) und Freitextsuche über alle Felder
  - Paginierung mit Vor/Zurück-Navigation
  - Details als zusammenklappbarer Bereich (HTML details/summary)
  - Zugriff nur für ADMIN und SUPERADMIN Rollen
  - Navigationslink "Audit-Logs" im Header
  - AuditLogQueryUseCase (Inbound Port), AuditLogQueryService (Application Service)
  - JpaAuditLogQueryAdapter mit JPA Specifications für dynamische Filterung
  - AuditLogAdminController unter /admin/audit-logs
- Tag-UI-Integration (STK-053, SWR-084, SWR-085, SWR-086)
  - Tag-Verwaltung im Admin-Bereich unter /admin/tags (anlegen, umbenennen, löschen)
  - Tag-Auswahl im Post-Formular per Checkboxen, inline-Anlage neuer Tags
  - Tag-Anzeige in Post-Übersicht und Detailansicht als Labels
  - Neuer TagAdminController für die Web-Administration
  - BlogViewController um Tag-Logik erweitert (syncPostTags, resolvePostTags, buildPostTagMap)
  - PostFormData um tagIds und newTagName Felder erweitert
  - PostUseCase/PostService um syncPostTags-Methode erweitert
  - CSS-Klasse .tag-label für einheitliche Tag-Darstellung
  - Navigation-Link "Tags" im Header für authentifizierte Benutzer
- E2E-Testing mit Selenium WebDriver (STK-052, SWR-078 bis SWR-083, SWA-035, ADR-0033)
  - Separates Maven-Modul e2e-tests/ mit Profil 'e2e' (mvn verify -Pe2e)
  - Testcontainers-Selenium (Chrome-Container, kein lokaler Browser nötig)
  - Page Object Model mit 12 Page Objects für alle UI-Seiten
  - 5 Testklassen: BlogPublicView, BlogAuthorWorkflow, Authentication, AdminWorkflow, SuperAdmin
  - Screenshot-on-Failure JUnit 5 Extension (target/screenshots/)
  - E2E-Tests als Pflichtschritt im Release-Script (scripts/release.sh)
  - Lokale Ausführung: ./mvnw verify -Pe2e
- Vollständige Vorschau für unveröffentlichte Posts (STK-051, SWR-076, SWR-077)
  - Neuer Endpoint GET /posts/{id}/preview zeigt DRAFT-Posts vollständig gerendert (Markdown, Syntax-Highlighting, Mermaid)
  - Vorschau-Button im Post-Bearbeitungsformular (öffnet in neuem Tab)
  - Vorschau-Banner in der Detailansicht zur Unterscheidung von veröffentlichten Posts
  - Zugriff nur für authentifizierte Benutzer (AUTHOR, ADMIN, SUPERADMIN)
- Tenant Management Service (services/tenant-management): Eigenständiger Microservice für die Verwaltung von Tenants mit eigener Admin UI (ADR-0031, STK-050, SWR-072, SWR-073, SWR-074, SWR-075, SWA-034)
  - Domain-Modell: Tenant-Aggregate mit Lifecycle (ACTIVE, SUSPENDED, DEACTIVATED), Login-Modi (INTERNAL, OIDC, BOTH), OIDC-Konfiguration, Auto-Approve-Regeln, Impressum/Datenschutz
  - gRPC-API (Port 9090) für Service-zu-Service-Kommunikation: GetTenant, CreateTenant, UpdateGeneralSettings, UpdateOidcSettings, UpdateLegalSettings, ListTenants
  - Break-Glass Admin UI unter /tenant/admin mit InMemoryUserDetailsManager, Tenant-Liste, Einstellungs-Tabs (Allgemein, OIDC, Legal), Tenant-Wechsel
  - PostgreSQL-Persistenz mit Flyway-Migrationen (Port 5434), Audit-Logging aller Tenant-Mutationen
  - Kubernetes-Manifeste: Deployment (2 Replicas), Service, Ingress, NetworkPolicy, Linkerd Server/AuthorizationPolicy (gRPC HTTP/2), ServiceAccount, PodDisruptionBudget
  - DefaultTenantFilter: Injiziert X-Tenant-Id Header wenn fehlend, Session-basierter Tenant-Override
  - Dockerfile für Container-Image (eclipse-temurin:25-jre-alpine, Ports 8082/9090)
  - CI-Pipeline: Container-Build und Release-Assets für tenant-management
  - Flux GitOps: ImageRepository, ImagePolicy und ImageUpdateAutomation für automatische Deployments
  - Flux Kustomization: Health-Check für tenant-management Deployment
  - CloudNativePG: tenant_management Datenbank in postInitApplicationSQL
  - docker-compose: postgres-tenant-management Service (Port 5434) für lokale Entwicklung

### Fixed

- Standard-LoginMode bei neuen Tenants/TenantSettings von BOTH auf INTERNAL geändert: Bei Neuanlage ohne explizite OIDC-Konfiguration schlug die Validierung fehl, da BOTH die Felder oidcIssuerUrl und oidcClientId erfordert (SWR-044, SWR-073)
- Template-Namenskollision zwischen Tenant-Management und User-Management: admin-login.html und admin/settings.html in tenant-management umbenannt zu tenant-admin-login.html und tenant-admin/settings.html, da beide Services die Shared UI Library nutzen und identische Template-Namen zu Konflikten führten (SWR-073)
- E2E-Tests repariert: 64 Tests in 6 Testklassen laufen stabil (SWR-078 bis SWR-086)
  - CDP-basierte HTTP-Basic-Authentifizierung statt URL-Credentials (Chrome 135+ entfernt Credentials aus URLs)
  - Locale-Auswahl in Post-Formularen ergänzt (Pflichtfeld, ohne Auswahl schlug Validierung fehl)
  - Submit-Button-Selektoren auf main/tab-content eingeschränkt (Logout-Button im Header wurde fälschlich geklickt)
  - XPath-Selektoren für Post-spezifische Edit/Publish/Preview-Links (Mehrfach-Posts in Liste)
  - Test-Reihenfolge mit @Order für zustandsabhängige Tag-Tests
  - JS-confirm-Dialog bei Tag-Löschung per form.submit() umgangen (Chrome auto-dismiss)
  - Login-Formular: Collapsed details-Element wird vor Eingabe expandiert
  - AdminLoginPage wartet nach Login auf URL-Änderung
- Mermaid-Diagramme in Blog-Posts wurden als Roh-Codeblöcke angezeigt: Flexmark rendert Mermaid-Blöcke als `<pre><code class="language-mermaid">`, aber Mermaid.js erwartet `<pre class="mermaid">`. JavaScript wandelt die Elemente jetzt vor der Mermaid-Initialisierung um und highlight.js verarbeitet Mermaid-Blöcke nicht mehr.
- Doorstop-Referenzen in IMP021 und IMP025 zeigten auf alte Pfade unter services/blog-content, obwohl layout/default.html, fragments/footer.html und static/css/custom.css nach libs/shared-ui verschoben wurden

## [0.9.2] - 2026-05-08

### Added

- Shared UI Library (libs/shared-ui): Gemeinsame Thymeleaf-Templates (Layout, Footer) und CSS-Styles als Maven-Modul (STK-048, SWR-065, SWR-066, SWR-067, SWR-068, SWA-033)
- Blog Content und User Management Service nutzen die Shared UI Library statt duplizierter Layouts und Styles
- Tenant-Einstellungen in separaten Formularen pro Einstellungsgruppe: Allgemein, OIDC und Impressum/Datenschutz mit Tab-Navigation und unabhängiger Speicherung (STK-049, SWR-069, SWR-070, SWR-071)

### Changed

- Admin-Settings-Endpunkte aufgeteilt: POST /auth/admin/settings ersetzt durch /settings/general, /settings/oidc und /settings/legal

### Fixed

- Neu registrierte Benutzer wurden im Admin-Backend nicht angezeigt: Bei der Registrierung (register, syncFromOidc, syncFromInternal) wurde keine Tenant-Membership angelegt, sodass der INNER JOIN in findByTenantId keine Ergebnisse lieferte
- OIDC-Login-Button (Authentik) funktionslos: `/oauth2/authorization/**` fehlte im Ingress-Routing und im Security-Filter-Matcher, sodass der OAuth2-Autorisierungs-Request nicht zum user-management Service gelangte

### Removed

- Duplizierte layout/default.html, fragments/footer.html und css/custom.css aus Blog Content und User Management Service (ersetzt durch libs/shared-ui)

## [0.9.1] - 2026-05-08

### Added

- Kubernetes Ingress für User Management Service: Pfad-basiertes Routing von `/auth/**` und `/login/oauth2/code/**` zu user-management (ADR-0032)
- NetworkPolicy für Traefik-zu-User-Management Ingress-Traffic (Port 8081)
- Linkerd AuthorizationPolicy für Traefik-zu-User-Management HTTP-Zugriff
- Redis-Session-Konfiguration (spring.data.redis.*) im user-management k8s-Profil (SWR-064)
- Forward-Headers-Strategy im user-management k8s-Profil für korrekte Redirect-URLs hinter Traefik

### Fixed

- Redirect-Loop bei `/auth/login`: Fehlende Ingress-Konfiguration leitete Auth-Requests an blog-content statt user-management, was eine Endlosumleitungsschleife verursachte
- Fehlende Umgebungsvariablen im user-management Deployment (BLOG_ADMIN_PASSWORD, SPRING_REDIS_HOST/PORT/PASSWORD)

## [0.9.0] - 2026-05-07

### Added

- ADR-0032: Eigenständige UI für Authentifizierung und Benutzerverwaltung im User Management Service
- SWR-064: Shared Redis Session zwischen Blog Content und User Management Service
- OIDC-Konfiguration pro Tenant über Admin-Interface (STK-047, SWR-061)
- Admin-UI-Felder für OIDC Issuer URL, Client ID und Client Secret in den Tenant-Einstellungen
- gRPC-Felder oidcIssuerUrl, oidcClientId, oidcClientSecret im TenantSettings-Protokoll
- Dynamische OAuth2-Client-Registrierung basierend auf Tenant-OIDC-Konfiguration (TenantAwareClientRegistrationRepository)
- Passwort-Maskierung: bestehende Client Secrets werden bei Anzeige maskiert und bei Rückgabe von "***" beibehalten
- Validierung: OIDC- und BOTH-Modus erfordern vollständige OIDC-Konfiguration (Issuer URL + Client ID)
- Lokale Benutzerregistrierung mit Benutzername und Passwort (STK-046, SWR-059, SWR-060)
- REST-Endpunkt POST /api/users/register im User Management Service mit BCrypt-Passwort-Hashing
- gRPC RegisterUser-Operation im UserManagementService
- Thymeleaf-Registrierungsseite unter /register im Blog Content Service
- Registrierungslink auf der Login-Seite (sichtbar bei LoginMode INTERNAL oder BOTH)
- Erfolgsseite nach erfolgreicher Registrierung mit Hinweis auf Admin-Freigabe
- Doorstop-Anforderungen: STK-046, SWR-059, SWR-060
- Software Detail Design Dokumentation für lokale Registrierung
- User Management Service: SecurityConfiguration mit 4 Filter Chains (API, Admin, Auth UI, Default)
- User Management Service: AuthLoginController, AuthRegistrationController, AuthAdminController für Thymeleaf-UI
- User Management Service: SyncingOidcUserService für lokale OIDC-Benutzersynchronisation (ohne gRPC-Umweg)
- User Management Service: LoginRateLimitFilter zum Schutz gegen Brute-Force-Angriffe auf Login-Endpunkte
- User Management Service: DefaultTenantFilter für automatische X-Tenant-Id-Injektion
- User Management Service: AdminProperties für Break-Glass-SUPERADMIN-Konfiguration
- User Management Service: Thymeleaf-Templates (login, admin-login, register, registration-success, admin/users, admin/settings)
- Blog Content Service: Benutzerdefinierter AuthenticationEntryPoint (401 für API, Redirect für Web)

### Changed

- SWR-028, SWR-044, SWR-051, SWR-052, SWR-053, SWR-060, SWR-063: Zuständigkeit für Login-, Registrierungs- und Admin-UI vom Blog Content Service zum User Management Service verschoben (ADR-0032)
- User Management Service Detail Design: Neuer Abschnitt "Inbound: Thymeleaf Web-UI" mit Login-, Registrierungs- und Admin-Seiten
- Blog Content Service Detail Design: Anforderungsabdeckung und Sicherheitssektion aktualisiert (ADR-0032 Verweise)

### Removed

- Blog Content Service: LoginController, RegistrationController, AdminController (verschoben zum User Management Service)
- Blog Content Service: SyncingOidcUserService, TenantAwareClientRegistrationRepository, LoginRateLimitFilter, AdminProperties
- Blog Content Service: Alle Auth-Templates (login, register, admin-login, registration-success, admin/users, admin/settings)
- Blog Content Service: OIDC-Konfiguration und Break-Glass-Admin-Properties (jetzt im User Management Service)

## [0.8.9] - 2026-05-07

### Fixed

- Markdown-Tabellen werden jetzt korrekt als HTML-Tabellen gerendert (flexmark-ext-tables Extension hinzugefügt, SWR-035)
- Ändern des Content-Types (HTML/Markdown) beim Bearbeiten eines Posts wird jetzt korrekt gespeichert (contentType-Feld in UpdatePostCommand ergänzt, SWR-035)
- Chronologische Post-Navigation zeigt direkte Links statt aufklappbare Details-Elemente (STK-025, SWR-039)

## [0.8.8] - 2026-05-04

### Added

- Redis als externer Session-Store für horizontale Skalierung des Blog Content Service (ADR-0030, STK-045, SWR-057, SWR-058, SWA-032, INF-014)
- Spring Session Data Redis Integration im Blog Content Service (spring-session-data-redis, spring-boot-starter-data-redis)
- Kubernetes-Manifeste für Redis Deployment im Namespace redis (Deployment, Service, NetworkPolicy, Secret)
- Network Policy: Egress vom tomsblog Namespace zu Redis erlaubt
- Infrastruktur-Dokumentation für Redis (docs/infra/modules/ROOT/pages/data/redis.adoc)
- Arc42 Querschnittliche Konzepte: Abschnitt Session Management
- Arc42 Verteilungssicht: Abschnitt Redis als Session-Store
- Software Detail Design: Session Management Abschnitt mit Sequenzdiagramm im Blog Content Service

### Changed

- Blog Content Service Deployment auf 2 Replicas erhöht (horizontale Skalierung)

### Fixed

- CI: pandoc als Abhängigkeit in docs- und release-Jobs installieren (fehlte nach ADR-Rendering-Umstellung)

## [0.8.7] - 2026-05-04

### Added

- Audit-Logging für alle relevanten Aktionen (SWR-056, SWA-031): `AuditLogEntry` und `AuditLogger`-Port im Shared Kernel, `JpaAuditLogger`-Adapter in Blog Content und User Management Service, Flyway-Migrationen (V8 bzw. V5) für `audit_log`-Tabelle
- Doorstop-Anforderungen SWR-056 (Audit-Logging) und SWA-031 (Audit-Logging-Architektur)

### Changed

- ADR-Rendering auf pandoc umgestellt (ersetzt selbstgeschriebenen Python-Markdown-Konverter)
- ADRs unterstuetzen jetzt Code-Syntax-Highlighting (pandoc, Tango-Theme), Mermaid-Diagramme (client-seitig via mermaid.js) und PlantUML-Diagramme (via plantuml CLI)

### Fixed

- AdminController: GET /admin/users lieferte JSON-Fehler (500 ProblemDetail) statt HTML, wenn der User-Management-Service nicht erreichbar war. Jetzt mit Fallback auf leere Liste und Log-Warnung
- GlobalExceptionHandler: Scope auf REST-Controller-Package eingeschränkt (`basePackages`), damit Thymeleaf-Controller nicht fälschlich JSON-Fehlerseiten erhalten
- WebExceptionHandler: Erweitert auf AdminController und generischen 500-Fehler-Handler mit HTML-Fehlerseite
- Dedizierte ServiceAccounts für blog-content und user-management Deployments, damit Linkerd mTLS-Identity korrekt zur AuthorizationPolicy passt (gRPC-Calls schlugen mit PERMISSION_DENIED fehl, weil Pods mit `default` SA liefen)
- arc42 Kontextdiagramm: Authentik (OIDC Provider), Garage (S3) und Administrator als fehlende externe Akteure ergänzt
- arc42 Bausteinsicht L1: RabbitMQ, Garage (S3) ergänzt, Database-per-Service-Verletzung bei Podcast/Video korrigiert, gRPC-Protokoll an blog-usermgmt-Verbindung annotiert
- arc42 Bausteinsicht L2: Fehlende Klassen ergänzt (TagUseCase, TranslationUseCase, TagService, TranslationService, MarkdownRenderer, alle Web-Adapter, alle Tag/Translation-Persistence-Adapter, FlexmarkMarkdownRenderer, UserManagementGrpcClient, ContentType Enum)
- arc42 Deployment-Diagramm: User Management Service, eigene PostgreSQL-Instanz, RabbitMQ und Authentik ergänzt
- arc42 Quality-Tree: Fehlende Äste Sicherheit und Skalierbarkeit ergänzt
- arc42 Runtime-Create: Falschen Interface-Namen `CreatePostUseCase` zu `PostUseCase` korrigiert, `Kafka` durch `EventPublisher` ersetzt
- arc42 Bausteinsicht Text: Flyway-Migrationen V5-V7 ergänzt, User Management Adapter-Tabelle um TenantSettings-Zweig, gRPC-Adapter, Security und alle fehlenden Persistence-Klassen erweitert
- arc42 Technischer Kontext: Authentik, gRPC, RabbitMQ und Garage als fehlende Schnittstellen ergänzt
- Blog Content Design Doc: Fehlende Enums ContentType und TranslationSource im Domain-Modell-Diagramm ergänzt
- User Management Design Doc: TenantSettingsUseCase, TenantSettingsRepository, TenantSettingsService und UserProfileNotFoundException im Application-Layer-Diagramm ergänzt, SyncUserCommand zu SyncOidcUserCommand korrigiert, gRPC-Adapter-Abschnitt hinzugefügt

### Added

- ADR-0029: Deklarative Datenbankverwaltung mit CloudNativePG Database CRD (Migration von postInitApplicationSQL auf Database CRD, geplant für Phase 5)
- Infrastructure Requirement INF-013: Deklarative Datenbankverwaltung mit CloudNativePG Database CRD
- Architecture Requirement SWA-030: Deklarative Datenbank-Ressourcen pro Service
- TODO Phase 5: CNPG-Upgrade und Database-CRD-Migration eingeplant

## [0.8.6] - 2026-05-03

### Fixed

- CloudNativePG Cluster: `postInitApplicationSQL` ergänzt, um `user_management` Datenbank beim Bootstrap automatisch anzulegen (fehlende DB verursachte CrashLoopBackOff des user-management Pods)

## [0.8.5] - 2026-05-03

### Added

- GitHub Actions CI: Container-Build als Matrix-Job für alle Services (blog-content, user-management)
- Dockerfile für user-management Service
- Kubernetes-Manifeste für user-management (Deployment, Service, Secret, Linkerd Server/AuthzPolicy)
- Flux Image-Automation für user-management (ImageRepository, ImagePolicy, ImageUpdateAutomation)
- application-k8s.yml für user-management mit Health-Probe-Gruppen

### Changed

- CI-Workflow: Release-Assets enthalten nun auch user-management JAR
- Infra-Dokumentation (image-automation.adoc, ADMINS.md) um user-management erweitert
- ADMINS.md: Health-Check-Dokumentation aktualisiert (liveness/readiness Probe-Gruppen)
- ADMINS.md: gRPC-Verbindungskonfiguration statt REST-URL für User Management

### Fixed

- S3 Backup: backup-s3-credentials Secret und ScheduledBackup in Kustomization aufgenommen (waren nicht deployed)
- S3 Backup: Garage s3_region von "garage" auf "us-east-1" korrigiert (AuthorizationHeaderMalformed)
- S3 Backup: Garage von v1.0.1 auf v1.3.1 aktualisiert (fehlende STREAMING-AWS4-HMAC-SHA256-PAYLOAD Unterstützung)

## [0.8.4] - 2026-05-03

## [0.8.3] - 2026-05-03

### Changed

- Flux Image-Automation auf SemVer-basierte Tags umgestellt (zuvor SHA-basiert)
- Doppelte ImageUpdateAutomation aus kubernetes-playground entfernt (Race-Condition behoben)
- Infra-Dokumentation (image-automation.adoc, ADMINS.md) an SemVer-Strategie angepasst

### Fixed

- ACME HTTP-01 Challenge-Pfad von HTTPS-Redirect ausgenommen (IngressRoute)
- blog-content Pod CrashLoopBackOff: emptyDir-Volume für /tmp bei readOnlyRootFilesystem
- Liveness-Probe initialDelaySeconds auf 60s erhöht (App-Startzeit ~40s)

## [0.8.2] - 2026-05-03

### Added

- Standard-Impressum und Datenschutzerklärung als statische Seiten unter /impressum und /privacy (SWR-054)
- Tenant-spezifisches Impressum und Datenschutzerklärung über Admin-Einstellungen konfigurierbar (SWR-055)
- Footer-Links zu Impressum und Datenschutz auf allen Seiten
- DB-Migration V4: impressum_content und privacy_policy_content Felder in tenant_settings
- gRPC-Erweiterung: impressum_content und privacy_policy_content in TenantSettingsResponse/UpdateTenantSettingsRequest

## [0.8.1] - 2026-05-03

### Added

- Infra: S3-kompatibler Objektspeicher (Garage) auf Netcup VM bereitgestellt (Backup-Storage für Kubernetes)
- PostgreSQL Backup-Strategie: CloudNativePG Barman Object Store mit täglichen Base Backups und WAL-Streaming auf Garage S3
- ScheduledBackup-Ressource für automatische tägliche Backups um 02:00 UTC (30 Tage Retention)
- Disaster-Recovery-Plan dokumentiert (Restore, PITR, Ablauf) in ADMINS.md und Infra-Doku
- Thymeleaf: Drop-down-Vorschau für vorherigen und nächsten Post auf der Detailseite (Titel, Datum, Textauszug)

## [0.8.0] - 2026-05-03

### Added

- OWASP Security Review Agent und initialer Security-Audit-Bericht (`docs/audits/2026-05-03_security-review.md`)
- Login-Rate-Limiting: `LoginRateLimitFilter` begrenzt POST-Anfragen auf Login-Endpunkte (10 Versuche/5 Min pro IP)
- HTML-Sanitisierung: OWASP Java HTML Sanitizer für Markdown-Ausgabe (`FlexmarkMarkdownRenderer`)
- JPA-Auditing: `@CreatedBy`/`@LastModifiedBy` in `PostJpaEntity` via `SecurityAuditorAware`
- API-Key-Authentifizierung für User-Management-Service (`ApiKeyAuthenticationFilter`, `SecurityConfiguration`)
- gRPC-Exception-Interceptor: `GrpcExceptionInterceptor` verhindert Stack-Trace-Leaks
- Globale Fehlerbehandlung: `MethodArgumentNotValidException`, `HttpMessageNotReadableException`, Catch-All-Handler
- Eingabegrößenbeschränkungen: `@Size`-Annotationen auf `CreatePostRequest` und `AddSourceRequest`
- URL-Validierung in `Source` Domain-Modell (nur http/https, URI-Syntaxprüfung)
- UUID-Validierung in `DefaultTenantFilter` für `X-Tenant-Id` Header
- Security-Header: CSP, `X-Frame-Options: DENY`, HSTS in `SecurityConfiguration`
- Kubernetes-Hardening: Pod Security Context (non-root, read-only FS, drop ALL capabilities)
- `PodDisruptionBudget` für blog-content Deployment
- Swagger/OpenAPI in Produktion deaktiviert (`application-k8s.yml`)
- OWASP Dependency-Check Maven-Plugin (failBuildOnCVSS=7) in Parent-POM
- Stakeholder Requirements STK040/STK041/STK042: Tenant-Branding, Admin-Benutzerverwaltung UI, Admin-Einstellungen UI
- Software Requirements SWR050-053: Tenant-Branding-Header, Admin-Benutzerliste, Admin-Einstellungen, SuperAdmin-Tenant-Umschaltung
- Tenant-Branding: dynamischer Tenant-Name und Tagline im Header (SWR-050)
- Admin-Oberfläche: Benutzerliste mit Approve/Reject/Rollenzuweisung (SWR-051)
- Admin-Oberfläche: Tenant-Einstellungen (Login-Modus, Auto-Approval, Branding) (SWR-052)
- SuperAdmin-Tenant-Umschaltung: Dropdown im Header zum Wechsel des aktiven Tenants (SWR-053)
- gRPC-Erweiterungen: `ListUsersByTenant`, `ApproveUser`, `RejectUser`, `ChangeUserRole`, `UpdateTenantSettings`, `ListTenants`
- Thymeleaf-Templates: `admin/users.html`, `admin/settings.html`
- `TenantBrandingAdvice`: ControllerAdvice für globale Tenant-Branding-Attribute
- `AdminController`: Controller für Admin-UI-Seiten
- Flyway-Migration V3: `display_name` und `tagline` Spalten in `tenant_settings`

### Changed

- `TenantSettings` Domain-Modell: erweitert um `displayName` und `tagline`
- `DefaultTenantFilter`: berücksichtigt Session-basierte Tenant-Überschreibung (SuperAdmin), UUID-Validierung
- `SecurityConfiguration`: Admin-Endpunkte erfordern ADMIN/SUPERADMIN, Tenant-Umschaltung erfordert SUPERADMIN, rollenbasierte API-Zugriffskontrolle
- Header-Fragment: dynamischer Tenant-Name, optionaler Tagline, Admin-Link, Tenant-Dropdown
- `docker-compose.yml`: Credentials über Umgebungsvariablen statt Hardcoded, kafka-ui Version gepinnt
- `AdminProperties`: wirft `IllegalStateException` wenn Passwort nicht konfiguriert
- `application.yml` (blog-content): Admin-Passwort ohne Default-Wert (erzwingt Konfiguration)
- highlight.js CDN-Einbindungen mit `crossorigin="anonymous"` und `referrerpolicy="no-referrer"`

### Security

- OWASP Security Review durchgeführt: 16 Findings identifiziert und behoben
- XSS-Schutz: HTML-Sanitisierung für nutzergenerierte Markdown-Inhalte
- Brute-Force-Schutz: Rate-Limiting auf Login-Endpunkten
- Eingabevalidierung: Größenbeschränkungen, URL-Protokoll-Validierung, UUID-Format-Validierung
- Container-Hardening: non-root User, read-only Filesystem, keine Capabilities
- Service-zu-Service-Authentifizierung: API-Key für User-Management-Service
- Fehlerbehandlung: keine Stack-Traces in API-Antworten (REST und gRPC)

## [0.7.3] - 2026-05-03

### Added

- Stakeholder Requirements STK037/STK038/STK039: Inter-Service-Kommunikationsstrategie (gRPC synchron, Kafka/RabbitMQ asynchron, REST/GraphQL nur für Clients)
- Software Requirements SWR046-049: Konkretisierung der Kommunikationsprotokolle zwischen Services
- gRPC-Contracts-Modul (`libs/grpc-contracts`): Protocol Buffer Definitionen und generierte Stubs für UserManagementService und TenantSettingsService
- gRPC-Server-Adapter im User-Management-Service: `UserManagementGrpcService` und `TenantSettingsGrpcService` (Port 9090)
- gRPC-Client-Adapter im Blog-Content-Service: `UserManagementGrpcClient` ersetzt den bisherigen REST-basierten Client

### Changed

- `UserManagementClient` von konkreter REST-Implementierung zu Interface refactored (hexagonale Architektur)
- Inter-Service-Kommunikation blog-content → user-management von REST auf gRPC migriert (SWR-046, SWR-049)

### Deprecated

- `UserManagementProperties`: gRPC-Konfiguration erfolgt nun über `grpc.client.user-management.*`
- `SyncOidcUserDto`: Ersetzt durch Protocol Buffer Messages

## [0.7.2] - 2026-05-03

### Added

- Featured Posts: Posts können für einen Zeitraum hervorgehoben und auf der Landing Page prominent dargestellt werden (STK027, SWR-041, SWR-042)
- Domain-Modell: `featuredFrom`/`featuredUntil` Felder auf Post, `isFeatured(LocalDate)` Methode, `updateFeatured()` mit Validierung
- Neuer Outbound-Port: `PostRepository.findFeaturedByTenantId(TenantId, LocalDate)`
- Neuer Inbound-Port: `PostUseCase.listFeaturedPosts(TenantId, LocalDate)`
- REST-API: `featuredFrom`/`featuredUntil` in CreatePostRequest, UpdatePostRequest und PostResponse
- Thymeleaf-UI: Featured-Sektion auf der Landing Page, Featured-Felder im Post-Formular
- Flyway-Migration V7: `featured_from` und `featured_until` Spalten in der Posts-Tabelle
- Stakeholder Requirements STK035/STK036: HTTPS-Redirect und Default-Domain-Redirect
- Infrastructure Requirements INF011/INF012: Traefik IngressRoute-Konfiguration
- Traefik IngressRoute: Catch-all HTTP-zu-HTTPS-Redirect (Priorität 1, ACME-Challenges ausgenommen)
- Traefik IngressRoute: Catch-all Redirect unbekannter Domains auf https://blog.tomirgang.de

## [0.7.1] - 2026-05-03

### Added

- Flux Image Automation Manifeste: ImageRepository, ImagePolicy und ImageUpdateAutomation für automatische Deployment-Updates
- GitRepository und Kustomization Ressourcen für Flux GitOps Reconciliation
- Konfigurierbare Login-Methode pro Tenant: INTERNAL, OIDC oder BOTH (STK031, SWR-044)
- Auto-Approval: optionale automatische Freigabe für OIDC-Benutzer und/oder E-Mail-Domain-Whitelist (STK032, SWR-045)
- TenantSettings Domain-Modell mit LoginMode, autoApproveOidc und autoApproveEmailDomains
- TenantSettingsUseCase und TenantSettingsService (Application Layer)
- REST-API für Tenant-Einstellungen unter /api/tenants/{tenantId}/settings
- Flyway-Migration V2: tenant_settings und tenant_auto_approve_domains Tabellen
- ADR-0028: Multi-Tenant-Isolation auf Datenebene (Shared Schema + Application-Level Filtering)
- Login-Seite zeigt dynamisch nur die konfigurierten Login-Methoden an (OIDC-Button, Formular oder beides)
- Auto-Approval-Logik bei OIDC-Sync und internem Login im UserProfileService

### Changed

- SyncOidcUserCommand und SyncInternalUserCommand erfordern jetzt TenantId
- UserManagementClient.syncOidcUser() akzeptiert zusätzlich tenantId-Parameter
- SyncingOidcUserService propagiert TenantId an den User-Management-Service
- LoginController fragt LoginMode vom User-Management-Service ab

## [0.7.0] - 2026-05-03

### Added

- OIDC Login mit Authentik (Spring Security OAuth2 Client, Authorization Code Flow)
- Leichtgewichtiger User-Management-Service (Benutzerverwaltung, Rollen, Tenant-Zuordnung, Freigabe-Workflow)
- Break-Glass SuperAdmin-Login unter /admin/login (formbasiert, nur SUPERADMIN)
- SuperAdmin-Account: Credentials über Umgebungsvariable/Kubernetes Secret konfigurierbar
- Rollen- und Berechtigungsmodell (SuperAdmin, Admin, Autor, Reviewer, Leser)
- Benutzer-Freigabe-Workflow: Neue User starten mit Status PENDING, Approve/Reject über REST-API
- OIDC-Gruppen-Mapping: Gruppen aus dem `groups`-Claim werden an den User Management Service weitergeleitet
- Thymeleaf Login-Seite mit OIDC-Button und optionalem Formular-Login
- Admin-Login-Seite für SuperAdmin Break-Glass Zugang
- Administrationshandbuch (docs/ADMINS.md) mit Kubernetes-Deployment und OIDC-Konfiguration
- OIDC-Konfiguration vollständig über Umgebungsvariablen/Infrastruktur-Config steuerbar (kein Rebuild nötig)
- Stakeholder-Requirement STK-033 (OIDC Konfiguration über Infrastruktur oder Admin UI)
- Stakeholder-Requirement STK-034 (Administrationsdokumentation für generisches Kubernetes)

## [0.6.0] - 2026-05-03

### Added

- Volltextsuche für Blog Posts über Titel und Inhalt mit PostgreSQL tsvector/tsquery (STK-022, SWR-038)
- Chronologische Previous/Next-Navigation auf Post-Detailseiten (STK-025, SWR-039)
- Serien-Navigation für thematisch zusammenhängende Posts mit optionalen Vorgänger/Nachfolger-Verknüpfungen (STK-026, SWR-040)
- GIN-Index für Volltextsuche (V5-Migration)
- Serien-Navigationsfelder in Posts-Tabelle (V6-Migration)
- Suchformular in der Post-Liste und Suchergebnis-Anzeige
- Chronologische und Serien-Navigationslinks auf Post-Detailseiten
- REST-Endpunkt GET /api/posts/search für Volltextsuche

## [0.5.4] - 2026-05-03

### Added

- Landing Page: nur die neuesten 3 Posts anzeigen (STK-017, SWR-033)
- Footer: Versionsnummer der Anwendung anzeigen (STK-018, SWR-034)
- Blog Content: HTML und Markdown unterstützen, Markdown gerendert darstellen inkl. Mermaid-Diagramme und Syntax-Highlighting (STK-019, SWR-035)
- Vorschau in Post-Liste: gerendert darstellen statt Plaintext (STK-020, SWR-036)
- Vorschau in Post-Liste: ersten Abschnitt anzeigen (STK-021, SWR-037)
- ContentType-Feld im Domain-Modell (HTML/MARKDOWN) mit Datenmigration V4
- Flexmark-basierter Markdown-Renderer als Outbound-Adapter
- VersionModelAdvice (ControllerAdvice) für globale appVersion-Modellvariable
- highlight.js und Mermaid.js (CDN) für clientseitiges Syntax-Highlighting und Diagramm-Rendering

## [0.5.3] - 2026-05-03

### Changed

- cert-manager Duplikat entfernt: k3s-HelmChart deaktiviert (`enable_cert_manager = false`), nur Flux-verwaltete Installation bleibt
- Infra-Dokumentation aktualisiert: cert-manager jetzt als Flux-verwaltet dokumentiert, Netzwerk-Diagramm Control-Plane auf CX33 korrigiert
- Deployment Image-Tag von `latest` auf versionierten Commit-SHA (`sha-<hash>`) umgestellt (INF-008)

### Added

- Flux Image Automation: ImageRepository, ImagePolicy und ImageUpdateAutomation für automatische Deployment-Manifest-Updates bei neuen Container-Images

## [0.5.2] - 2026-05-03

### Changed

- Control-Plane-Node von CX23 (2 vCPU, 4 GB) auf CX33 (4 vCPU, 8 GB) aufgerüstet (k3s-Server benötigt allein 1,3 Gi RAM)
- Cluster-Kosten aktualisiert: ca. 30 EUR/Monat (CX33 + 2x CX23 + LB11 + Volumes + IPs)
- Dokumentation angepasst: ADR-0022, arc42 Verteilungssicht, Infra-Doku, SWA-017, INF-001

### Added

- Infrastruktur-Dokumentation als eigene Antora-Komponente (`docs/infra/`): Cluster-Setup, GitOps, Service Mesh, Networking, Datenhaltung, Operations
- Doorstop-Requirement-Typen `INF` (Infrastructure Requirements) und `OPS` (Operations Requirements) mit eigener Hierarchie unter STK
- Initiale Infrastruktur-Requirements INF-001 bis INF-010 (Cluster, Flux, TLS, Secrets, PostgreSQL HA, Linkerd, Network Policies, Image-Automation, Backup, Monitoring)
- Initiale Operations-Requirements OPS-001 bis OPS-003 (Rollback, Disaster Recovery, Alerting)
- Navigations-Link "Infrastruktur" im Antora-Header
- Stakeholder-Requirement STK-022 (Volltextsuche über Tenant-Inhalte)

## [0.5.1] - 2026-05-02

### Changed

- Landing Page (`/`) zeigt nun die Post-Liste statt einer statischen Willkommensseite
- Post-Vorschau in der Liste zeigt reinen Text (HTML-Tags werden entfernt) statt rohem HTML

## [0.5.0] - 2026-05-02

### Added

- Linkerd Trust-Roots ConfigMap als GitOps-Manifest (`trust-roots-configmap.yaml`)

### Fixed

- Linkerd Server CRD API-Version korrigiert (`v1beta3` → `v1beta1`)
- Linkerd CRDs: Gateway API HTTPRoute CRD-Konflikt behoben (temporäres `enableHttpRoutes: false`, dann Neuinstallation)
- Egress-NetworkPolicy: Zugriff auf Linkerd-Namespace ergänzt (Sidecar-Proxy benötigt Verbindung zu Identity und Destination)
- Blog-Content Deployment auf 1 Replica reduziert (Session-Affinität ohne gemeinsamen Session-Store nicht gewährleistet)

## [0.4.6] - 2026-05-02

## [0.4.5] - 2026-05-02

### Added

- Default-Deny AuthorizationPolicies für Linkerd: Server-Ressource, MeshTLSAuthentication für Traefik, NetworkAuthentication für Kubelet-Probes (SWA023)

### Fixed

- CI: Race Condition bei parallelem docs-Deploy (main + tag) durch Beschränkung auf main-Branch behoben
- CI: Redundanten `release: [published]` Trigger entfernt (tag push reicht)
- CI: `release-assets` Job war nicht lauffähig (abhängig von `docs` Job der bei Release-Events übersprungen wurde)

### Changed

- CI: Concurrency-Group hinzugefügt um superseded Runs abzubrechen
- CI: `spotless:check` und `verify` in einen Maven-Aufruf zusammengefasst
- CI: Container-Image wird auch bei Tag-Push gebaut (mit Semver-Tags)
- CI: `release-assets` Job ist jetzt self-contained (generiert Docs und Requirements selbst)

## [0.4.4] - 2026-05-02

### Added

- mTLS für alle Service-zu-Service-Verbindungen: Traefik-Namespace mit Linkerd Mesh-Injection annotiert (`linkerd.io/inject: enabled`)
- Trust Anchor und Identity Issuer Certificates als erledigt markiert (waren bereits konfiguriert)

## [0.4.3] - 2026-05-02

### Fixed

- Release-Script: Build und Tests laufen jetzt vor Versions- und Changelog-Änderungen, damit bei Build-Fehlern das Working Directory sauber bleibt

## [0.4.2] - 2026-05-02

### Fixed

- Actuator Health-Endpoints (`/actuator/health/**`) von Spring Security ausgenommen, damit Kubernetes Readiness-Probe nicht mit 401 fehlschlägt

## [0.4.1] - 2026-05-02

### Fixed

- TLS-Zertifikat-Ausstellung: NetworkPolicies korrigiert (Traefik-Namespace statt kube-system)
- TLS-Zertifikat-Ausstellung: Ingress auf websecure-Entrypoint beschränkt, damit HTTP-01 ACME-Challenges nicht durch Redirect-Middleware blockiert werden
- NetworkPolicy für cert-manager ACME HTTP-01 Solver-Pod hinzugefügt (Port 8089)

### Added

- Linkerd Service Mesh Installation via Flux Helm Charts (linkerd-crds 1.8.0, linkerd-control-plane 1.16.11)
- cert-manager Trust Anchor (self-signed CA, 10 Jahre) und Identity Issuer (48h, auto-rotiert) für Linkerd
- Flux HelmRepository und HelmRelease CRs für GitOps-verwaltetes Linkerd Deployment
- Mesh-Injection-Annotation am tomsblog Namespace (linkerd.io/inject: enabled)
- SWA-022: Architektur-Requirement für Linkerd Deployment via Flux und cert-manager

## [0.4.0] - 2026-05-02

### Added

- Ingress + TLS: Kubernetes Ingress-Ressource für blog.tomirgang.de mit automatischem Let's Encrypt TLS
- cert-manager ClusterIssuer (Staging + Production) für ACME HTTP-01 Challenges
- Traefik Middleware für HTTP→HTTPS Redirect
- SWR-032: HTTPS-Verschlüsselung für externe Zugriffe
- SWA-021: TLS-Terminierung am Ingress mit cert-manager und Let's Encrypt
- arc42 Verteilungssicht: Abschnitt zu TLS und Ingress ergänzt
- SOPS + age Verschlüsselung für Secrets im Git-Repository (.sops.yaml, Flux Decryption)
- SWA-020: SOPS+age Architektur-Requirement
- SWR-031: Software-Requirement für verschlüsselte Secrets und automatische Synchronisation
- infra/k8s/README.md: Secrets-Management-Dokumentation (Architektur, Workflow, Schlüsselverwaltung)
- ADR-0025: Secrets Management Strategie (Reflector jetzt, ESO in Meilenstein 2)
- ADR-0026: Migration auf Helm Charts in Phase 5 (Kafka, RabbitMQ, MongoDB Operatoren)
- STK-016: Stakeholder Requirement für sichere Verwaltung von Secrets im Cluster
- SWA-019: Cross-Namespace Secret-Synchronisation mit Reflector
- Reflector Installation via Flux HelmRelease im IaC-Repository
- Automatische Secret-Spiegelung von postgres-cluster-app nach tomsblog Namespace
- ADR-0024: Kustomize für Kubernetes-Manifeste (Entscheidung gegen Helm)
- NetworkPolicies für Namespace `tomsblog` (Default-Deny-Ingress, Ingress von Traefik, Intra-Namespace, Egress zu PostgreSQL/DNS/Internet)
- Kustomize-Manifeste für blog-content Service (Deployment, Service, Secret, Namespace)
- Spring-Profil `application-k8s.yml` für Kubernetes-Deployment
- CloudNativePG initdb-Konfiguration (Datenbank `blog_content`, Owner `blog_content`)

### Changed

- Deployment nutzt reflektiertes Secret `postgres-cluster-app` statt Placeholder `blog-content-db`
- ADR-0024 um Referenz auf geplante Helm-Migration (ADR-0026) ergänzt
- TODO.md: Phase 5 um Helm-basierte Operator-Deployments erweitert
- TODO.md: Meilenstein 2 um External Secrets Operator ergänzt

## [0.3.4] - 2026-05-02

### Added

- ADR-0022: Kubernetes-Plattform mit kube-hetzner und OpenTofu
- ADR-0023: CloudNativePG Operator für PostgreSQL auf Kubernetes
- SWA-017: Kubernetes-Cluster-Provisionierung mit kube-hetzner (OpenTofu, Hetzner Cloud)
- SWA-018: PostgreSQL-Betrieb mit CloudNativePG Operator
- Applikationsspezifische K8s-Manifeste unter infra/k8s/ (PostgreSQL Cluster, PVC)
- Flux GitOps-Setup: Bootstrap in kubernetes-playground Repo, tomsblog als GitRepository-Source
- arc42 Verteilungssicht um Cluster-Provisionierung und CloudNativePG-Details erweitert
- ADR-0021: Zero Trust mit Linkerd Service Mesh (mTLS, AuthorizationPolicies, User-Kontext-Propagierung)
- STK-014: Stakeholder Requirement für sichere Service-zu-Service-Kommunikation
- STK-015: Stakeholder Requirement für KI-Features nur für autorisierte Benutzer
- SWR-029: Software Requirement für Linkerd mTLS und Default-Deny-Policies
- SWR-030: Software Requirement für User-Kontext-Propagierung
- TODO Phase 3 um Linkerd/Zero-Trust-Aufgaben erweitert
- TODO Phase 6 um User-Kontext-Prüfung und AuthorizationPolicy für AI-Service ergänzt
- ADR-0020: Flux als GitOps-Tool ausgewählt (statt ArgoCD oder Flux offen)

## [0.3.3] - 2026-05-01

### Changed

- Release-Skript: Antora-Versionsupdates werden erst nach erfolgreichem Build/Test durchgeführt
- Release-Skript: neuer --rebuild-Modus zum erneuten Testen ohne Versionsänderungen

## [0.3.2] - 2026-05-01

### Fixed

- Release-Skript: doppelte Testausführung (clean install + verify) zu einem einzigen clean verify zusammengefasst, um Testcontainers-Ressourcenkonflikte zu vermeiden

## [0.3.1] - 2026-05-01

### Fixed

- Release-Skript aktualisiert jetzt die Antora-Dokumentationsversionen (prerelease-Marker und Versionsnummer in arc42 index.adoc)

## [0.3.0] - 2026-05-01

### Added

- Einfacher Admin-Login mit Spring Security zum Schutz aller Schreiboperationen (SWR-028)
- SecurityConfiguration: Formular-Login für Web-UI, HTTP Basic für REST API, CSRF-Schutz
- AdminProperties: Konfigurierbare Admin-Credentials via Spring Properties (blog.admin.username/password)
- Login-Template (login.html) im PicoCSS-Layout mit Fehler- und Logout-Meldungen
- Header-Fragment zeigt Login/Logout-Link je nach Authentifizierungsstatus
- Post-Liste zeigt nur veröffentlichte Posts für anonyme Besucher, alle Posts nach Login
- SecurityConfigurationTest: Tests für öffentliche, geschützte und API-Endpunkte
- AdminPropertiesTest: Tests für Default-Werte bei null/blank Credentials
- Veröffentlichen-Button in Post-Liste (POST /posts/{id}/publish) für Draft-Posts
- DefaultTenantFilter: Property-basierter Filter (blog.default-tenant-id, blog.default-author-id) injiziert Default-Header in allen Profilen (ersetzt LocalDevHeaderFilter)
- Post-Liste zeigt alle Posts (inkl. Drafts) mit Status-Anzeige und Publish-Aktion
- Thymeleaf-Formular zum Erstellen (GET /posts/new, POST /posts) und Bearbeiten (GET /posts/{id}/edit, POST /posts/{id}) von Posts
- PostFormData: Form-Backing-Bean mit Bean Validation für serverseitige Validierung
- SWR-027: Anforderung für Thymeleaf-Formular zum Erstellen/Bearbeiten von Posts
- IMP-023: Implementierungsverweis für Thymeleaf-Formular

### Changed

- Post-Liste (/posts) zeigt nun alle Posts des Tenants (nicht nur veröffentlichte) mit Status-Badge
- LocalDevHeaderFilter umbenannt zu DefaultTenantFilter und von @Profile("local") zu @ConfigurationProperties umgestellt

### Fixed

- Kafka-Listener-Konfiguration: 0.0.0.0 durch Kurzform ersetzt (apache/kafka:3.9.0 Kompatibilität)
- Kafka UI Port von 8080 auf 9080 geändert (Konflikt mit Spring Boot)
- Öffentliche Blog-Ansicht mit Post-Liste (GET /posts) und Einzelansicht (GET /posts/{slug})
- PostUseCase: listPublishedPosts() und getPublishedPostBySlug() für öffentliche Leseansicht
- PostRepository: findPublishedByTenantId() und findBySlugAndTenantId() für Status-Filter und Slug-Suche
- Thymeleaf Templates: posts/list.html, posts/show.html, error/404.html
- WebExceptionHandler für 404-Fehlerseiten im Web-Layer
- Navigation: Posts-Link im Header-Fragment
- SWR-026: Anforderung für öffentliche Blog-Ansicht
- IMP-022: Implementierungsverweis für öffentliche Blog-Ansicht
- Thymeleaf Layout-Grundgeruest mit PicoCSS (responsive, Dark/Light Mode)
- Thymeleaf Layout Dialect fuer Template-Vererbung (layout:decorate)
- BlogViewController als Inbound Web Adapter (GET /)
- Base Layout Template (header, nav, main, footer)
- Wiederverwendbare Fragments (header.html, footer.html)
- Custom CSS mit Sticky-Footer-Layout und Tenant-Branding-Hooks
- SWR-025: Anforderung fuer Thymeleaf Layout-Grundgeruest
- IMP-021: Implementierungsverweis fuer Thymeleaf Layout

## [0.2.1] - 2026-05-01

### Fixed

- GitHub Pages Deployment: Race Condition bei konkurrierenden Pushes auf docs-Branch behoben (force_orphan: true)

## [0.2.0] - 2026-05-01

### Added

- Integrationstests mit Testcontainers (PostgreSQL 17) fuer Blog Content Service
- Vollstaendiger SpringBootTest (BlogContentApplicationTest) mit Testcontainers
- Audit-Feld-Tests fuer JPA Entities (createdAt, updatedAt, createdBy, updatedBy)
- PreUpdate-Callback-Tests fuer alle JPA Entities (Post, Tag, Translation)
- Delete-Integrationstest fuer JpaPostRepository
- PostController-Test fuer Posts mit Sources und Attachments (PostResponse.SourceResponse, AttachmentResponse)
- TagController-Test fuer GET /api/tags/{id} Happy Path
- PostService-Test fuer removeSource wenn Post nicht gefunden
- Exception-Accessor-Tests (getPostId, getTagId, getTranslationId)
- Code Coverage von 95.4% auf 99.9% Line Coverage und 97.3% Branch Coverage verbessert
- OpenAPI/Swagger API-Dokumentation fuer Blog Content Service (springdoc-openapi 2.8.8)
- Swagger UI unter /swagger-ui.html, OpenAPI Spec unter /api-docs
- OpenAPI-Annotationen an allen REST-Controllern (PostController, TagController, TranslationController, SourceController)
- OpenApiConfiguration mit Metadaten und automatischer X-Tenant-Id Header-Dokumentation
- Integrationstest fuer OpenAPI-Verfuegbarkeit (OpenApiIntegrationTest)

## [0.1.0] - 2026-05-01

### Added

- REST-API für Quellenverwaltung (SWR-012): POST/GET/DELETE /api/posts/{postId}/sources
- SourceController, AddSourceCommand, RemoveSourceCommand, AddSourceRequest, RemoveSourceRequest, SourceResponse
- PostUseCase um addSource(), removeSource(), listSources() erweitert
- PostService implementiert Quellenverwaltung mit Persistence
- Unit-Tests für SourceController (10 Tests) und PostService-Quellenverwaltung (5 Tests)
- Software Detail Design Dokumentation um SourceController-Endpunkte erweitert
- Jazzer Fuzz Tests für alle externen REST-Endpoints (PostController, TagController, TranslationController)
- Software-Requirement SWR-023: Fuzz-Testing externer Interfaces
- Jazzer-JUnit 0.24.0 als Fuzz-Testing-Framework
- JaCoCo Code-Coverage-Plugin mit 95% Minimum-Schwellwert (Line + Branch Coverage)
- GitHub Actions CI: Coverage-Report als Artifact und Job Summary
- ADR-0019: Database per Service (PostgreSQL / MongoDB, dedizierte Instanz pro Service)
- ADR-0018: Dual-Broker-Strategie (Kafka für Event-Streaming, RabbitMQ für Task-Queues)
- RabbitMQ in Docker Compose für lokale Entwicklung (Port 5672, Management UI 15672)
- Architektur-Requirement SWA-014: RabbitMQ als Task-Queue-Broker
- Architektur-Requirement SWA-015: Database per Service
- RabbitMQ-Konfiguration in application-local.yml

### Changed

- Docker Compose: PostgreSQL-Container auf service-spezifischen Namen und DB umgestellt (blog_content)
- ADR-0006 auf Status "Superseded by ADR-0018" gesetzt
- SWA-005 aktualisiert: Kafka-Verantwortung auf Event-Streaming eingegrenzt
- Arc42-Dokumentation um RabbitMQ- und Database-per-Service-Abschnitte erweitert
- ROADMAP Phase 5 um RabbitMQ-Tasks erweitert
- Tech-Stack-Beschreibung in AGENTS.md, README.md und libs/README.md aktualisiert

- Hexagonal architecture for blog-content service with ports and adapters
- Domain model: Post, Translation, Tag, Attachment, Source, PostLocale
- Application layer: PostUseCase, TagUseCase, TranslationUseCase with service implementations
- Inbound REST adapters: PostController, TagController, TranslationController
- Outbound JPA persistence adapters with PostgreSQL
- Shared Kernel library (DomainEvent, AggregateRoot, Auditable, TenantId, AuthorId)
- Event Contracts library for Kafka domain events
- Integration tests with Testcontainers (PostgreSQL)
- CI pipeline with GitHub Actions (build, test, container image)
- Software Detail Design documentation (AsciiDoc + Antora)
- arc42 architecture documentation
- ADRs for key decisions (hexagonal architecture, REST/GraphQL, Kafka, gRPC, etc.)
- Doorstop requirements tracing (STK, SWR, SWA, IMP, TST)
- Flyway database migrations
- Docker Compose for local development (PostgreSQL, Redis, Kafka)
- Roadmap with 6 milestones
- Maven multi-module project structure (parent, shared-kernel, event-contracts, blog-content)
- Upgrade spotless-maven-plugin from 2.44.0 to 3.4.0 (JDK 25 compatibility)
- Build & Test verification rule in AGENTS.md
- CI job to generate Antora documentation and attach as workflow artifact
- CI job to validate Doorstop requirements and attach coverage report as workflow artifact
- CI build triggers on tags and releases
- CI release-assets job attaches JAR, documentation, and requirements report to GitHub releases
- Antora versioning: docs are versioned from tags (v*), main branch produces dev prerelease
- CI deploys generated documentation to GitHub Pages via docs branch
- Stakeholder requirement STK-013: zeitgesteuerte Veröffentlichung
- Software requirement SWR-022: time-based auto publishing API
- ADR-0016: Backend for Frontend (BFF) pattern
- ADR-0017: API Gateway (Spring Cloud Gateway)
- Milestone 7: Android-App für Autoren (Kotlin, Jetpack Compose, Mobile BFF)

### Fixed

- Removed default "Products" and "Services" entries from Antora documentation header
- Added navigation header with links to Architektur (arc42), Software Detail Design, and Requirements
- Doorstop requirements report integrated into GitHub Pages docs site
- PlantUML diagrams: moved puml files to partials/ directory for correct Antora partial$ resolution
- ADR links: fixed broken attachmentsdir paths, symlinked ADRs into Antora attachments
- ADR index updated with missing ADRs (0009, 0015, 0016, 0017)
- Doorstop HTML pages: injected navigation bar with back link to main documentation
- CI Node.js 20 deprecation warnings by opting into Node.js 24 for actions
- Spotless/Palantir Java Format crash on JDK 25
- Compiler warnings (null safety, unused imports)
