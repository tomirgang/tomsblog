# TODO – Toms Blog

> Fokus: Zuerst den Text-Blog vollständig umsetzen, dann iterativ erweitern.
> Siehe [ROADMAP.md](ROADMAP.md) für die Gesamtübersicht.

---

## Meilenstein 1: Vollständiger Text-Blog (Thymeleaf)

### Phase 1: Projekt-Setup & Grundlagen

- [x] Monorepo-Struktur festlegen (Services, Infrastruktur, Docs)
- [x] Build-System aufsetzen (Maven, Multi-Module)
- [x] Gemeinsame Libraries definieren (Shared Kernel, Event-Contracts)
- [x] Entwicklungsumgebung dokumentieren (Docker Compose für lokale Deps)
- [x] arc42-Dokumentation initialisieren (AsciiDoc + Antora Skeleton)
- [x] ADR-Verzeichnis anlegen und erste ADRs schreiben
- [x] Doorstop einrichten (Requirement-Hierarchie: STK -> SWR -> SWA -> IMP -> TST)
- [x] Erste Stakeholder-Requirements erfassen (STK001-STK012, SWR001-SWR015, SWA001-SWA006)
- [x] CI-Pipeline einrichten (GitHub Actions: Build, Test, Container-Image)

### Phase 2: Blog Content Service

- [x] Spring Boot Projekt mit hexagonaler Struktur aufsetzen
- [x] Domain-Modell entwerfen (Post, Author, Tenant, Tag, Attachment)
- [x] Ports & Adapters implementieren (Inbound: REST API, Outbound: PostgreSQL)
- [x] CRUD-Operationen für Blog-Posts
- [x] Mehrsprachigkeit im Domain-Modell abbilden
- [x] Quellenverwaltung für Posts
- [x] Unit- und Integrationstests (Testcontainers für PostgreSQL)
- [x] API-Dokumentation (OpenAPI/Swagger)
- [x] Thymeleaf: Layout-Grundgerüst (responsiv, einfaches CSS-Framework)
- [x] Thymeleaf: Öffentliche Blog-Ansicht (Post-Liste, Einzelansicht)
- [x] Thymeleaf: Einfaches Formular zum Erstellen/Bearbeiten von Posts
- [x] Einfacher Admin-Login (Spring Security, formbasiert) zum Schutz der Schreiboperationen

> **MVP nach Phase 2:** Blog-Posts lesen, erstellen und bearbeiten lokal im Browser.

### Phase 3: Infrastruktur & erstes Deployment

- [x] Hetzner Kubernetes Cluster aufsetzen (kube-hetzner / OpenTofu)
- [x] CloudNativePG Operator für PostgreSQL
- [x] GitOps einrichten (Flux, siehe ADR-0020)
- [x] Kustomize-Manifeste für blog-content Service
- [x] Reflector für Cross-Namespace Secret-Synchronisation (ADR-0025)
- [x] Ingress-Controller + TLS (Let's Encrypt)
- [x] Container-Image bauen und deployen (CI/CD Pipeline)
- [x] Linkerd Service Mesh installieren (Helm via Flux, siehe ADR-0021)
- [x] mTLS für alle Service-zu-Service-Verbindungen aktivieren
- [x] Default-Deny AuthorizationPolicies konfigurieren
- [x] Trust Anchor und Issuer Certificates einrichten (cert-manager)

> **MVP nach Phase 3:** Blog ist öffentlich lesbar, Schreibzugriff nur mit Admin-Login. Alle internen Verbindungen mTLS-gesichert.

### Phase 4: Benutzerverwaltung & Multi-Tenancy

- [x] CI/CD: Versionierte Image-Tags (Commit-SHA) statt `latest`, Deployment-Manifest automatisch aktualisieren (Flux GitOps)
- [x] Landing Page: nur die neuesten 3 Posts anzeigen (STK017)
- [x] Footer: Versionsnummer der Anwendung anzeigen (STK018)
- [x] Blog Content: HTML und Markdown unterstützen, Markdown gerendert darstellen inkl. Mermaid-Diagramme und Syntax-Highlighting (STK019)
- [x] Vorschau in Post-Liste: gerendert darstellen statt Plaintext (STK020)
- [x] Vorschau in Post-Liste: ersten Abschnitt anzeigen (STK021)
- [x] Volltextsuche für Blog Posts (STK022)
- [x] Chronologische Previous/Next-Navigation auf Post-Detailseiten (STK025)
- [x] Serien-Navigation für thematisch zusammenhängende Posts (STK026)
- [x] Identity-Provider evaluieren (ADR-0027: Authentik + internes User Management)
- [x] Featured Posts: Posts für einen Zeitraum hervorheben und auf der Landing Page prominent darstellen (STK027)
- [x] OIDC Login integrieren (Spring Security OAuth2 Client, Authentik unter auth.do9ita.de)
- [x] Leichtgewichtiger User-Management-Service (OIDC Relying Party, Rollen, Tenant-Zuordnung)
- [x] Konfigurierbare Login-Methode pro Tenant: intern, OIDC oder beides (STK031)
- [x] Break-Glass SuperAdmin-Login unter /admin/login (immer formbasiert, nur SUPERADMIN)
- [x] Auto-Approval: optional für OIDC-Benutzer und/oder E-Mail-Domain-Whitelist (STK032)
- [x] Multi-Tenant-Isolation (Schema-basiert vs. Row-Level-Security) (ADR-0028: Shared Schema + App Filtering)
- [x] Rollen- und Berechtigungsmodell (SuperAdmin, Admin, Autor, Reviewer, Leser; anonyme Besucher = Leser)
- [x] SuperAdmin-Account: Credentials über Infrastruktur (Umgebungsvariable/Secret), automatisch beim Start angelegt
- [x] Benutzer-Freigabe: Neue User müssen durch Tenant-Admin approved werden (STK028)
- [x] Optionales OIDC-Gruppen-Mapping auf plattforminterne Rollen
- [x] Thymeleaf: Login-/Registrierungsseiten
- [x] Thymeleaf: Multi-Tenant-Navigation (Tenant-Branding, Umschaltung)
- [x] Thymeleaf: Admin-Oberfläche (Benutzer, Tenants, Einstellungen)
- [x] Infra: Authentik-Anbindung konfigurieren (OIDC Application unter auth.do9ita.de)
- [x] OIDC Konfiguration über Infrastruktur Config oder Admin UI (STK033)
- [x] Administrationsdokumentation für generisches Kubernetes in docs/ADMINS.md (STK034)
- [x] Infra: S3-kompatibles Storage (Garage auf Netcup VM)
- [x] Backup-Strategie für PostgreSQL
- [x] Backup-Storage auf Netcup VM (Garage/S3)
- [x] Disaster-Recovery-Plan dokumentieren
- [x] Standard Impressum and Privacy Policy
- [x] Optional Tenant spezifisches Impressum and Privacy Policy
- [x] Security-Reviews (OWASP)

> **MVP nach Phase 4:** Mehrbenutzerfähiger Blog mit Login, Tenant-Trennung und Admin-UI.

### Phase 5: Messaging & Event-Driven Architecture

- [x] Fix Admin UI
- [x] Fix ADR Markdown rendering
- [x] Audit-Logging für alle relevanten Aktionen
- [x] Thymeleaf: Drop-down für vorherigen und nächsten Post
- [x] Infra: Redis Deployment
- [x] 2 Blog Deployments und Shared Session über Redis
- [x] Fix: Table Rendering
- [x] Fix: Ändern zwischen Markdown und HTML
- [x] Fix: Post Navigation sollen direkte Links sein
- [x] Benutzer Registrierung
- [x] OIDC Config pro Tennant über Admin Interface
- [x] Bessere Architektur für User UI
- [x] UI Library
- [x] Tenant Einstellungen eigene Formulare pro Einstellungsgruppe, z.B. OIDC Settings, Impressum und Privacy, ... (STK-049, SWR-069, SWR-070, SWR-071)
- [x] Tenant Service
- [x] Der Superadmin soll neue Tenants über die Admin UI anlegen können.
- [x] Vorschau für Posts
- [ ] E2E-Selenium-Tests für alle UI-Use-Cases (STK-052, SWR-078 bis SWR-083, ADR-0033)
- [ ] Tags
- [ ] Auditlog View
- [ ] Kafka-Cluster-Konfiguration definieren
- [ ] RabbitMQ-Cluster-Konfiguration definieren
- [ ] Event-Schema-Design (Avro/Protobuf oder JSON Schema)
- [ ] Events produzieren (Post erstellt, aktualisiert, veröffentlicht) via Kafka
- [ ] RabbitMQ-Queues für Task-Verteilung (Übersetzung, TTS, Snapshots)
- [ ] Consumer für nachgelagerte Prozesse (Feeds)
- [ ] Infra: Kafka Deployment (Strimzi Operator via Helm, siehe ADR-0026)
- [ ] Infra: RabbitMQ Deployment (RabbitMQ Cluster Operator via Helm, siehe ADR-0026)
- [ ] Infra: MongoDB Deployment (Community Operator via Helm, siehe ADR-0026)
- [ ] Migration auf Helm Charts für Operator-managed Services (ADR-0026)
- [ ] CloudNativePG auf ≥ 1.25 upgraden (Voraussetzung für Database CRD, ADR-0029)
- [ ] Migration auf deklarative Database CRD für alle Service-Datenbanken (INF-013, SWA-030)
- [ ] Dedizierte Rollen pro Service evaluieren (nach Verfügbarkeit DatabaseRole CRD)
- [ ] Security-Reviews (OWASP)
- [ ] Architecture Review

> **MVP nach Phase 5:** Asynchrone Verarbeitung aktiv, Events fließen zwischen Services.

### Phase 6: Observability

- [ ] Prometheus + Grafana aufsetzen
- [ ] Loki für Log-Aggregation
- [ ] Tempo für Distributed Tracing
- [ ] Dashboards und Alerting
- [ ] Health-Checks und Readiness/Liveness Probes
- [ ] Chaos Testing: Resilience-Szenarien definieren (Netzwerkausfälle, Pod-Crashes, Latenz)
- [ ] Chaos Testing: Chaos Mesh oder Litmus auf K8s einrichten
- [ ] Chaos Testing: Steady-State-Hypothesen und Experimente formulieren
- [ ] Chaos Testing: Automatisierte Chaos-Experimente in CI/CD integrieren
- [ ] Chaos Testing: Ergebnisse auswerten und Resilience-Verbesserungen umsetzen
- [ ] Security-Reviews (OWASP)
- [ ] Architecture Review

> **MVP nach Phase 6:** Produktionsreife Observability mit Monitoring, Logging und Tracing.

### Phase 7: KI-Integration (Text-Blog)

- [ ] OpenRouter-Adapter implementieren
- [ ] KI-gestützte Übersetzung (Author-getriggert via UI, Ergebnis muss geprüft werden)
- [ ] Schreibassistenz-API
- [ ] Titelbild-Generierung (optional, Author-getriggert)
- [ ] User-Kontext-Propagierung: KI-Endpoints nur mit Rolle AUTHOR/ADMIN (SWR-030)
- [ ] AuthorizationPolicy: nur blog-content darf ai-service aufrufen
- [ ] Thymeleaf: WYSIWYG-Editor für Autoren (ersetzt einfaches Formular)
- [ ] Thymeleaf: Übersetzungs-UI (Review/Approve-Workflow)
- [ ] Security-Reviews (OWASP)
- [ ] Architecture Review

> **MVP nach Phase 7:** Autoren schreiben mit WYSIWYG-Editor und nutzen KI-Übersetzung mit Review-Schritt.

### Phase 8: Feed-Service & Chaos Testing

- [ ] RSS/Atom Feed-Generierung
- [ ] Tag-basierte Feeds
- [ ] Single Content Type Feeds
- [ ] Thymeleaf: Tag-basierte Navigation und Filteransicht
- [ ] Security-Reviews (OWASP)
- [ ] Architecture Review

> **MVP nach Phase 8:** Blog mit RSS-Feeds, navigierbaren Tag-Seiten und nachgewiesener Resilience durch Chaos Testing.

### Phase 9: Web-Snapshots & Attachments

- [ ] Automatische Archivierung referenzierter Webseiten
- [ ] Attachment-Upload und -Verwaltung
- [ ] Storage-Backend (S3/Garage)
- [ ] Thymeleaf: Attachment-Upload im Editor (Drag & Drop)
- [ ] Thymeleaf: Quellen-Verwaltung mit Vorschau archivierter Seiten
- [ ] Security-Reviews (OWASP)
- [ ] Architecture Review

> **MVP nach Phase 9:** Vollständiges Content-Management mit Dateianhängen und Quellenarchiv.

---

## Meilenstein 2: Kommentare, E-Mail & Interaktion

- [ ] External Secrets Operator (ESO) für externe Secret-Stores (ADR-0025)
- [ ] Kommentar-Domain-Modell & Service
- [ ] Moderation (Spam-Filter, Freigabe-Workflow)
- [ ] Thymeleaf-Integration (Kommentarformular, Anzeige)
- [ ] Event: Kommentar erstellt → Benachrichtigung
- [ ] Passwort-Reset per E-Mail
- [ ] Passwortloser Login durch E-Mail-Bestätigung (Magic Link)
- [ ] Optionale E-Mail-Verifizierung für nicht-OIDC-Benutzer (interne Accounts)
- [ ] E-Mail-Benachrichtigungen: Neuer Post veröffentlicht
- [ ] E-Mail-Benachrichtigungen: Review angefordert
- [ ] Abonnement-System pro Tenant (optionales Feature):
    - Alle neuen Posts abonnieren
    - Bestimmte Tags abonnieren
    - Suchbegriff abonnieren
    - Bestimmte Autoren abonnieren
- [ ] Für neue Abonnenten wir ein Reader Acount angelegt, optional mit Passwort.
- [ ] Registrierte Benutzer können ihre Abonnements verwalten
- [ ] DSGVO Auskunft für Benutzer
- [ ] PostgreSQL Row-Level Security

---

## Meilenstein 3: Angular-UI für Text-Blog

> Technologie: Angular + Nx + Native Federation | Kommunikation: REST

- [ ] Nx-Workspace aufsetzen (Monorepo für Angular-Apps & Libraries)
- [ ] Native Federation konfigurieren (Shell + Remote-Modules)
- [ ] Blog-Lese-Ansicht (Public)
- [ ] Blog-Editor (Admin, WYSIWYG)
- [ ] User-Management-UI
- [ ] REST-API-Anbindung (bestehende Spring Boot APIs)
- [ ] Deployment als statische Assets auf K8s (Nginx/Caddy)

---

## Meilenstein 4: Audio-Podcast

> Technologie: React + Module Federation | Kommunikation: GraphQL

- [ ] Podcast-Domain-Service (Spring Boot + GraphQL-API via Spring for GraphQL)
- [ ] Media-Storage (S3/Garage)
- [ ] TTS-basierte Podcast-Generierung (Event-getriggert aus Blog-Posts)
- [ ] Podcast-Feed (RSS für Podcast-Clients)
- [ ] React-UI: Workspace-Setup (Module Federation)
- [ ] React-UI: Podcast-Player & Verwaltung
- [ ] GraphQL-Anbindung (Apollo Client)

---

## Meilenstein 5: Video-Podcast

> Technologie: React + Module Federation | Kommunikation: GraphQL

- [ ] Video-Service (Spring Boot + GraphQL-API)
- [ ] Video-Storage & Transcoding-Pipeline
- [ ] Video-Feed
- [ ] React-UI: Video-Player & Verwaltung (Module Federation Remote)
- [ ] GraphQL-Anbindung (Apollo Client)

---

## Meilenstein 6: Social Media Promotion

> Automatisierte Bewerbung von Blog-Posts auf Mastodon und LinkedIn beim Publizieren.

- [ ] Domain-Erweiterung: `socialMediaTitle` und `socialMediaSummary` am Post
- [ ] Fallback-Logik: Titel = Post-Titel wenn leer, Summary = erster Absatz wenn leer
- [ ] Social Media Connection Service (OAuth-Anbindung für Mastodon & LinkedIn)
- [ ] Backend-UI: Plattform-Verbindungen pro Autor konfigurieren (einmalig im Profil)
- [ ] Post-Editor: Auswahl der Ziel-Plattformen pro Post
- [ ] Post-Editor: Optionale Felder für Social Media Title und Summary
- [ ] Event-basierte Promotion: PostPublishedEvent triggert Social-Media-Adapter
- [ ] Mastodon-Adapter (Mastodon API, Toot mit Link + Summary)
- [ ] LinkedIn-Adapter (LinkedIn Share API, Post mit Link + Summary)
- [ ] Fehlerbehandlung & Retry (Graceful Degradation bei Plattform-Ausfall)
- [ ] Status-Anzeige: Promotion-Status pro Post und Plattform im Backend sichtbar

---

## Querschnitt (laufend)

- [x] PostgreSQL HA: Auf mindestens 2 Instanzen erhöhen (nach Phase 3 MVP)
- [x] Backup-Strategie für PostgreSQL
- [x] Backup-Storage auf Netcup VM (Garage/S3)
- [x] Disaster-Recovery-Plan dokumentieren
- [ ] Security-Reviews (OWASP)
- [ ] Dokumentation pflegen (arc42, ADRs)
