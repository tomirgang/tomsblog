# Toms Blog

> Eine multi-tenant Blog- und Podcast-Plattform mit KI-Unterstützung, gebaut als Lernprojekt für moderne Cloud-Native-Architekturen.

## Überblick

Toms Blog ist eine vollwertige Content-Plattform, die als Lernprojekt für moderne Software-Architektur und Cloud-Native-Technologien dient. Die Plattform unterstützt mehrsprachige Blog-Posts, Audio- und Video-Podcasts sowie KI-gestützte Content-Erstellung – betrieben auf einer produktionsnahen Kubernetes-Infrastruktur.

## Architektur

Das System folgt einer **hexagonalen Architektur** (Ports & Adapters) und ist als **Microservice-Landschaft** umgesetzt. Die Architektur ist **audit-fähig** konzipiert, sodass alle relevanten Aktionen nachvollziehbar protokolliert werden.

**Backend:** Spring Boot (Java/Kotlin)

### Infrastruktur

| Komponente     | Technologie                                          |
| -------------- | ---------------------------------------------------- |
| Orchestrierung | Hetzner Kubernetes (3-Node-Cluster via kube-hetzner) |
| Messaging      | Apache Kafka                                         |
| Relationale DB | PostgreSQL (CloudNativePG Operator)                  |
| Dokument-DB    | MongoDB                                              |
| Caching        | Redis                                                |
| Observability  | Prometheus, Grafana, Loki, Tempo                     |
| GitOps         | ArgoCD oder Flux                                     |
| Backup-Storage | Netcup VM mit Garage (S3-kompatibel)                 |

### Deployment & GitOps

Das Deployment erfolgt vollständig über GitOps-Prinzipien. Infrastruktur- und Anwendungskonfiguration werden deklarativ in Git verwaltet und automatisch über ArgoCD oder Flux synchronisiert.

## Features

### Content Management

- **Multilang Blog** – Beiträge in mehreren Sprachen verfassen und verwalten
- **WYSIWYG-Editor** – Komfortables Erstellen und Bearbeiten von Inhalten
- **Attachments** – Dateianhänge für Blog-Posts
- **Quellenverwaltung** – Quellenangaben für Posts mit integriertem Quellenmanagement
- **Automatische Web-Snapshots** – Referenzierte Webseiten werden automatisch archiviert

### Podcast & Media

- **Audio-Podcast-Support** – Veröffentlichung von Audio-Episoden
- **Video-Podcast-Support** – Veröffentlichung von Video-Episoden
- **Automatische Podcast-Generierung** – TTS-basierte Audio-Versionen von Blog-Posts

### Feeds

- **Mixed Content Feeds** – Kombinierte Feeds über verschiedene Content-Typen
- **Single Content Feeds** – Dedizierte Feeds pro Content-Typ
- **Tag-basierte Feeds** – Feeds basierend auf Tags und Kategorien

### KI-Integration (via OpenRouter)

- **Automatische Übersetzung** – Blog-Posts automatisch in andere Sprachen übersetzen
- **Schreibassistenz** – KI-gestützte Unterstützung beim Verfassen von Inhalten
- **Titelbild-Generierung** – Optionale automatische Erstellung von Titelbildern

### Benutzerverwaltung

- **Multi-User** – Mehrere Autoren und Rollen
- **Multi-Tenant** – Mandantenfähigkeit für getrennte Blog-Instanzen
- **Admin-UI** – Administrationsoberfläche zur Verwaltung der Plattform
- **Kommentare** – Kommentarfunktion für Leser

## Dokumentation

Die Projektdokumentation folgt dem **arc42-Template** und wird mit folgenden Tools gepflegt:

- **Architekturdokumentation:** AsciiDoc + Antora, Diagramme mit PlantUML
- **Architecture Decision Records (ADRs):** Markdown mit Mermaid-Diagrammen, gerendert via MkDocs

## Lizenz

Siehe [LICENSE](LICENSE).
