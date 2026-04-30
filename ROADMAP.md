# Roadmap – Toms Blog

## Übersicht

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        MEILENSTEIN 1                                    │
│              Vollständiger Text-Blog (Thymeleaf)                        │
│                                                                         │
│  Setup → Domain → API → Auth → Feeds → KI → Thymeleaf-UI → Deploy       │
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
```

---

## Meilenstein 1: Vollständiger Text-Blog (Thymeleaf)

**Ziel:** Ein produktionsfähiger, multilingualer Text-Blog mit allen Kernfeatures – deployed auf Kubernetes.

| Schritt | Beschreibung                                                |
| ------- | ----------------------------------------------------------- |
| 1.1     | Projekt-Setup: Monorepo, Build-System, CI                   |
| 1.2     | Blog Content Service (Hexagonale Architektur, Spring Boot)  |
| 1.3     | Benutzerverwaltung & Multi-Tenancy                          |
| 1.4     | Messaging (Kafka) für Event-Driven-Flows                    |
| 1.5     | KI-Integration (Übersetzung, Schreibassistenz, Titelbilder) |
| 1.6     | Feed-Service (RSS/Atom)                                     |
| 1.7     | Web-Snapshots & Attachments                                 |
| 1.8     | Thymeleaf-UI (WYSIWYG-Editor, Admin, Public Blog)           |
| 1.9     | Infrastruktur & Deployment (K8s, GitOps)                    |
| 1.10    | Observability (Prometheus, Grafana, Loki, Tempo)            |

**Ergebnis:** Vollständig nutzbarer Text-Blog mit Admin-UI und öffentlicher Ansicht.

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
