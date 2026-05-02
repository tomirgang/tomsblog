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
- [ ] Ingress-Controller + TLS (Let's Encrypt)
- [ ] Container-Image bauen und deployen (CI/CD Pipeline)
- [ ] Linkerd Service Mesh installieren (Helm via Flux, siehe ADR-0021)
- [ ] mTLS für alle Service-zu-Service-Verbindungen aktivieren
- [ ] Default-Deny AuthorizationPolicies konfigurieren
- [ ] Trust Anchor und Issuer Certificates einrichten (cert-manager)

> **MVP nach Phase 3:** Blog ist öffentlich lesbar, Schreibzugriff nur mit Admin-Login. Alle internen Verbindungen mTLS-gesichert.

### Phase 4: Benutzerverwaltung & Multi-Tenancy

- [ ] Identity-Provider evaluieren (Keycloak vs. eigener Service)
- [ ] Auth-Service oder Keycloak-Integration
- [ ] Multi-Tenant-Isolation (Schema-basiert vs. Row-Level-Security)
- [ ] Rollen- und Berechtigungsmodell (Admin, Autor, Leser)
- [ ] Audit-Logging für alle relevanten Aktionen
- [ ] Thymeleaf: Login-/Registrierungsseiten
- [ ] Thymeleaf: Multi-Tenant-Navigation (Tenant-Branding, Umschaltung)
- [ ] Thymeleaf: Admin-Oberfläche (Benutzer, Tenants, Einstellungen)
- [ ] Infra: Keycloak-Deployment auf K8s

> **MVP nach Phase 4:** Mehrbenutzerfähiger Blog mit Login, Tenant-Trennung und Admin-UI.

### Phase 5: Messaging & Event-Driven Architecture

- [ ] Kafka-Cluster-Konfiguration definieren
- [ ] RabbitMQ-Cluster-Konfiguration definieren
- [ ] Event-Schema-Design (Avro/Protobuf oder JSON Schema)
- [ ] Events produzieren (Post erstellt, aktualisiert, veröffentlicht) via Kafka
- [ ] RabbitMQ-Queues für Task-Verteilung (Übersetzung, TTS, Snapshots)
- [ ] Consumer für nachgelagerte Prozesse (Feeds)
- [ ] Infra: Kafka Deployment (Strimzi Operator)
- [ ] Infra: RabbitMQ Deployment (RabbitMQ Cluster Operator)

> **MVP nach Phase 5:** Asynchrone Verarbeitung aktiv, Events fließen zwischen Services.

### Phase 6: KI-Integration (Text-Blog)

- [ ] OpenRouter-Adapter implementieren
- [ ] KI-gestützte Übersetzung (Author-getriggert via UI, Ergebnis muss geprüft werden)
- [ ] Schreibassistenz-API
- [ ] Titelbild-Generierung (optional, Author-getriggert)
- [ ] User-Kontext-Propagierung: KI-Endpoints nur mit Rolle AUTHOR/ADMIN (SWR-030)
- [ ] AuthorizationPolicy: nur blog-content darf ai-service aufrufen
- [ ] Thymeleaf: WYSIWYG-Editor für Autoren (ersetzt einfaches Formular)
- [ ] Thymeleaf: Übersetzungs-UI (Review/Approve-Workflow)

> **MVP nach Phase 6:** Autoren schreiben mit WYSIWYG-Editor und nutzen KI-Übersetzung mit Review-Schritt.

### Phase 7: Feed-Service & Chaos Testing

- [ ] RSS/Atom Feed-Generierung
- [ ] Tag-basierte Feeds
- [ ] Single Content Type Feeds
- [ ] Thymeleaf: Tag-basierte Navigation und Filteransicht
- [ ] Chaos Testing: Resilience-Szenarien definieren (Netzwerkausfälle, Pod-Crashes, Latenz)
- [ ] Chaos Testing: Chaos Mesh oder Litmus auf K8s einrichten
- [ ] Chaos Testing: Steady-State-Hypothesen und Experimente formulieren
- [ ] Chaos Testing: Automatisierte Chaos-Experimente in CI/CD integrieren
- [ ] Chaos Testing: Ergebnisse auswerten und Resilience-Verbesserungen umsetzen

> **MVP nach Phase 7:** Blog mit RSS-Feeds, navigierbaren Tag-Seiten und nachgewiesener Resilience durch Chaos Testing.

### Phase 8: Web-Snapshots & Attachments

- [ ] Automatische Archivierung referenzierter Webseiten
- [ ] Attachment-Upload und -Verwaltung
- [ ] Storage-Backend (S3/Garage)
- [ ] Thymeleaf: Attachment-Upload im Editor (Drag & Drop)
- [ ] Thymeleaf: Quellen-Verwaltung mit Vorschau archivierter Seiten
- [ ] Infra: S3-kompatibles Storage (Garage auf Netcup VM)

> **MVP nach Phase 8:** Vollständiges Content-Management mit Dateianhängen und Quellenarchiv.

### Phase 9: Observability

- [ ] Prometheus + Grafana aufsetzen
- [ ] Loki für Log-Aggregation
- [ ] Tempo für Distributed Tracing
- [ ] Dashboards und Alerting
- [ ] Health-Checks und Readiness/Liveness Probes
- [ ] Infra: Redis Deployment

> **MVP nach Phase 9:** Produktionsreife Observability mit Monitoring, Logging und Tracing.

---

## Meilenstein 2: Kommentare & Interaktion

- [ ] Kommentar-Domain-Modell & Service
- [ ] Moderation (Spam-Filter, Freigabe-Workflow)
- [ ] Thymeleaf-Integration (Kommentarformular, Anzeige)
- [ ] Event: Kommentar erstellt → Benachrichtigung

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
- [ ] Backup-Strategie für PostgreSQL
- [ ] Backup-Storage auf Netcup VM (Garage/S3)
- [ ] Disaster-Recovery-Plan dokumentieren
- [ ] Security-Reviews (OWASP)
- [ ] Dokumentation pflegen (arc42, ADRs)
