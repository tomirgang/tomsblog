# TODO – Toms Blog

> Fokus: Zuerst den Text-Blog vollständig umsetzen, dann iterativ erweitern.
> Siehe [ROADMAP.md](ROADMAP.md) für die Gesamtübersicht.

---

## Meilenstein 1: Vollständiger Text-Blog (Thymeleaf)

### Phase 1: Projekt-Setup & Grundlagen

- [ ] Monorepo-Struktur festlegen (Services, Infrastruktur, Docs)
- [ ] Build-System aufsetzen (Maven, Multi-Module)
- [ ] Gemeinsame Libraries definieren (Shared Kernel, Event-Contracts)
- [ ] CI-Pipeline einrichten (GitHub Actions: Build, Test, Container-Image)
- [ ] Entwicklungsumgebung dokumentieren (Docker Compose für lokale Deps)
- [ ] arc42-Dokumentation initialisieren (AsciiDoc + Antora Skeleton)
- [ ] ADR-Verzeichnis anlegen und erste ADRs schreiben
- [ ] Doorstop einrichten (Requirement-Hierarchie: STK → SWR → SWA → IMP → TST)
- [ ] Erste Stakeholder-Requirements erfassen

### Phase 2: Blog Content Service

- [ ] Spring Boot Projekt mit hexagonaler Struktur aufsetzen
- [ ] Domain-Modell entwerfen (Post, Author, Tenant, Tag, Attachment)
- [ ] Ports & Adapters implementieren (Inbound: REST API, Outbound: PostgreSQL)
- [ ] CRUD-Operationen für Blog-Posts
- [ ] Mehrsprachigkeit im Domain-Modell abbilden
- [ ] Quellenverwaltung für Posts
- [ ] Unit- und Integrationstests (Testcontainers für PostgreSQL)
- [ ] API-Dokumentation (OpenAPI/Swagger)

### Phase 3: Benutzerverwaltung & Multi-Tenancy

- [ ] Identity-Provider evaluieren (Keycloak vs. eigener Service)
- [ ] Auth-Service oder Keycloak-Integration
- [ ] Multi-Tenant-Isolation (Schema-basiert vs. Row-Level-Security)
- [ ] Rollen- und Berechtigungsmodell (Admin, Autor, Leser)
- [ ] Audit-Logging für alle relevanten Aktionen

### Phase 4: Messaging & Event-Driven Architecture

- [ ] Kafka-Cluster-Konfiguration definieren
- [ ] Event-Schema-Design (Avro/Protobuf oder JSON Schema)
- [ ] Events produzieren (Post erstellt, aktualisiert, veröffentlicht)
- [ ] Consumer für nachgelagerte Prozesse (Feeds, Übersetzung)

### Phase 5: KI-Integration (Text-Blog)

- [ ] OpenRouter-Adapter implementieren
- [ ] Automatische Übersetzung (Event-getriggert)
- [ ] Schreibassistenz-API
- [ ] Titelbild-Generierung (optional, Event-getriggert)

### Phase 6: Feed-Service

- [ ] RSS/Atom Feed-Generierung
- [ ] Tag-basierte Feeds
- [ ] Single Content Type Feeds

### Phase 7: Web-Snapshots & Attachments

- [ ] Automatische Archivierung referenzierter Webseiten
- [ ] Attachment-Upload und -Verwaltung
- [ ] Storage-Backend (S3/Garage)

### Phase 8: Thymeleaf-UI

- [ ] Layout & Navigation (Multi-Tenant-fähig)
- [ ] Öffentliche Blog-Ansicht (Post-Liste, Einzelansicht, Tags)
- [ ] WYSIWYG-Editor für Autoren
- [ ] Admin-Oberfläche (Benutzer, Tenants, Einstellungen)
- [ ] Responsives Design

### Phase 9: Infrastruktur & Deployment

- [ ] Hetzner Kubernetes Cluster aufsetzen (kube-hetzner / Terraform)
- [ ] CloudNativePG Operator für PostgreSQL
- [ ] Redis Deployment
- [ ] Kafka Deployment (Strimzi Operator)
- [ ] GitOps einrichten (ArgoCD oder Flux)
- [ ] Helm Charts oder Kustomize für alle Services

### Phase 10: Observability

- [ ] Prometheus + Grafana aufsetzen
- [ ] Loki für Log-Aggregation
- [ ] Tempo für Distributed Tracing
- [ ] Dashboards und Alerting
- [ ] Health-Checks und Readiness/Liveness Probes

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

## Querschnitt (laufend)

- [ ] Backup-Strategie für PostgreSQL
- [ ] Backup-Storage auf Netcup VM (Garage/S3)
- [ ] Disaster-Recovery-Plan dokumentieren
- [ ] Security-Reviews (OWASP)
- [ ] Dokumentation pflegen (arc42, ADRs)
