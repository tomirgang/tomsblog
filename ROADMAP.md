# Roadmap – Toms Blog

## Übersicht

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        MEILENSTEIN 1                                    │
│              Vollständiger Text-Blog (Thymeleaf)                        │
│                                                                         │
│  Setup → Domain+UI → Deploy → Auth → Kafka → KI → Feeds → Attach → Obs │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        MEILENSTEIN 2                                    │
│                    Kommentare & Interaktion                             │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        MEILENSTEIN 3                                    │
│         Angular-UI für Text-Blog (Native Federation + Nx)               │
│                        Kommunikation: REST                              │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        MEILENSTEIN 4                                    │
│                      Audio-Podcast                                      │
│         React-UI (Module Federation) + GraphQL-API                      │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        MEILENSTEIN 5                                    │
│                       Video-Podcast                                     │
│         React-UI (Module Federation) + GraphQL-API                      │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        MEILENSTEIN 6                                    │
│                Social Media Promotion                                   │
│       Automatisierte Bewerbung auf Mastodon & LinkedIn                  │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Meilenstein 1: Vollständiger Text-Blog (Thymeleaf)

**Ziel:** Ein produktionsfähiger, multilingualer Text-Blog mit allen Kernfeatures auf Kubernetes. Infrastruktur wächst inkrementell mit jedem Feature.

| Phase | Beschreibung                                                         | MVP danach                                                      |
| ----- | -------------------------------------------------------------------- | --------------------------------------------------------------- |
| 1     | Projekt-Setup: Monorepo, Build-System, CI, Docs, Requirements        | Build + Tests laufen                                            |
| 2     | Blog Content Service + Thymeleaf-Grundgerüst (Post CRUD, Layout)     | Blog-Posts lesen, erstellen, bearbeiten lokal im Browser        |
| 3     | Infrastruktur & erstes Deployment (K8s, PostgreSQL, GitOps, Ingress) | Blog ist öffentlich erreichbar                                  |
| 4     | Benutzerverwaltung & Multi-Tenancy (Auth, Rollen, Admin-UI)          | Mehrbenutzerfähiger Blog mit Login und Tenant-Trennung          |
| 5     | Messaging & Event-Driven (Kafka, Event-Produktion)                   | Asynchrone Verarbeitung aktiv, Events fließen zwischen Services |
| 6     | KI-Integration (Übersetzung, Schreibassistenz, WYSIWYG-Editor)       | WYSIWYG-Editor + KI-Übersetzung mit Review-Schritt              |
| 7     | Feed-Service (RSS/Atom, Tag-Navigation)                              | RSS-Feeds und navigierbare Tag-Seiten                           |
| 8     | Web-Snapshots & Attachments (Upload, Archivierung, S3-Storage)       | Vollständiges Content-Management mit Anhängen                   |
| 9     | Observability (Prometheus, Grafana, Loki, Tempo, Redis)              | Produktionsreife Observability                                  |

**Ergebnis:** Vollständig nutzbarer Text-Blog mit Admin-UI, öffentlicher Ansicht und professionellem Betrieb.

---

## Meilenstein 2: Kommentare & Interaktion

**Ziel:** Leser können Beiträge kommentieren; Moderation durch Autoren/Admins.

| Schritt | Beschreibung                                       |
| ------- | -------------------------------------------------- |
| 2.1     | Kommentar-Domain-Modell & Service                  |
| 2.2     | Moderation (Spam-Filter, Freigabe-Workflow)        |
| 2.3     | Thymeleaf-Integration (Kommentarformular, Anzeige) |
| 2.4     | Event: Kommentar erstellt → Benachrichtigung       |

---

## Meilenstein 3: Angular-UI für Text-Blog

**Ziel:** Moderne SPA als Alternative/Ablösung der Thymeleaf-UI für den Text-Blog.

| Schritt | Beschreibung                                                   |
| ------- | -------------------------------------------------------------- |
| 3.1     | Nx-Workspace aufsetzen (Monorepo für Angular-Apps & Libraries) |
| 3.2     | Native Federation konfigurieren (Shell + Remote-Modules)       |
| 3.3     | Blog-Lese-Ansicht (Public)                                     |
| 3.4     | Blog-Editor (Admin, WYSIWYG)                                   |
| 3.5     | User-Management-UI                                             |
| 3.6     | REST-API-Anbindung (bestehende Spring Boot APIs)               |
| 3.7     | Deployment als statische Assets auf K8s (Nginx/Caddy)          |

**Technologie-Stack:**
- Angular (aktuelle Version)
- Nx Monorepo
- Native Federation (@angular-architects/native-federation)
- Kommunikation: REST

---

## Meilenstein 4: Audio-Podcast

**Ziel:** Audio-Podcast-Feature inkl. automatischer TTS-Generierung aus Blog-Posts.

| Schritt | Beschreibung                                        |
| ------- | --------------------------------------------------- |
| 4.1     | Podcast-Domain-Service (Spring Boot + GraphQL-API)  |
| 4.2     | Media-Storage (S3/Garage)                           |
| 4.3     | TTS-basierte Podcast-Generierung (Event-getriggert) |
| 4.4     | Podcast-Feed (RSS für Podcast-Clients)              |
| 4.5     | React-UI: Workspace-Setup (Module Federation)       |
| 4.6     | React-UI: Podcast-Player & Verwaltung               |
| 4.7     | GraphQL-Anbindung (Apollo Client)                   |

**Technologie-Stack:**
- React
- Module Federation (Webpack/Rspack)
- Kommunikation: GraphQL (Spring for GraphQL)
- Apollo Client

---

## Meilenstein 5: Video-Podcast

**Ziel:** Video-Podcast-Feature mit Upload, Verwaltung und Streaming.

| Schritt | Beschreibung                                                   |
| ------- | -------------------------------------------------------------- |
| 5.1     | Video-Service (Spring Boot + GraphQL-API)                      |
| 5.2     | Video-Storage & Transcoding-Pipeline                           |
| 5.3     | Video-Feed                                                     |
| 5.4     | React-UI: Video-Player & Verwaltung (Module Federation Remote) |
| 5.5     | GraphQL-Anbindung                                              |

**Technologie-Stack:**
- React (shared mit Podcast-UI via Module Federation)
- Kommunikation: GraphQL

---

## Meilenstein 6: Social Media Promotion

**Ziel:** Beim Publizieren eines Blog-Posts wird dieser automatisiert auf konfigurierten Social-Media-Plattformen beworben. Unterstützte Plattformen: Mastodon und LinkedIn.

| Schritt | Beschreibung                                                                           |
| ------- | -------------------------------------------------------------------------------------- |
| 6.1     | Domain-Erweiterung: Social Media Title und Social Media Summary am Post                |
| 6.2     | Fallback-Logik: Social Media Title = Post-Titel, Summary = erster Absatz des Textes    |
| 6.3     | Social Media Connection Service (OAuth-Anbindung Mastodon & LinkedIn)                  |
| 6.4     | Backend-UI: Autor konfiguriert Plattform-Verbindungen einmalig im Profil               |
| 6.5     | Post-Editor: Auswahl der Ziel-Plattformen pro Post                                    |
| 6.6     | Event-basierte Promotion: PostPublishedEvent triggert Social-Media-Adapter             |
| 6.7     | Mastodon-Adapter (Mastodon API, Toot mit Link + Summary)                               |
| 6.8     | LinkedIn-Adapter (LinkedIn Share API, Post mit Link + Summary)                         |
| 6.9     | Fehlerbehandlung & Retry (Graceful Degradation bei Plattform-Ausfall)                  |
| 6.10    | Status-Anzeige: Autor sieht Promotion-Status pro Post und Plattform                    |

**Funktionsweise:**

- Jeder Post hat optionale Felder `socialMediaTitle` und `socialMediaSummary`
- Wenn `socialMediaTitle` leer ist, wird der normale Post-Titel verwendet
- Wenn `socialMediaSummary` leer ist, wird der erste Absatz des Post-Textes verwendet
- Autoren konfigurieren ihre Plattform-Verbindungen einmalig im Backend (OAuth-Flow)
- Pro Post wählt der Autor aus, auf welchen Plattformen beworben werden soll
- Beim Publizieren wird ein `PostPublishedEvent` ausgelöst, das die Social-Media-Adapter triggert

---

## Querschnittsthemen (laufend)

| Thema         | Beschreibung                                              |
| ------------- | --------------------------------------------------------- |
| Observability | Metriken, Tracing, Logging von Anfang an in jeden Service |
| Security      | Auth, RBAC, Input-Validation, OWASP-Checks                |
| Testing       | Unit, Integration (Testcontainers), E2E                   |
| Dokumentation | arc42, ADRs, OpenAPI/GraphQL-Schema-Docs                  |
| Backup & DR   | Backup-Strategie, Recovery-Tests                          |

---

## UI-Architektur (Zielzustand)

```
┌─────────────────────────────────────────────────────┐
│                   App Shell                         │
├──────────────────────┬──────────────────────────────┤
│   Angular (Nx +      │   React (Module Federation)  │
│   Native Federation) │                              │
├──────────────────────┼──────────────────────────────┤
│   Text-Blog UI       │   Podcast-UI  │  Video-UI    │
│   (REST)             │   (GraphQL)   │  (GraphQL)   │
└──────────────────────┴──────────────────────────────┘
```

> Die Thymeleaf-UI bleibt als Fallback/SSR-Variante bestehen oder wird nach vollständiger Angular-Migration abgelöst.
