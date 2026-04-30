# Entwicklungsumgebung

## Voraussetzungen

| Tool                    | Version | Zweck                                            |
| ----------------------- | ------- | ------------------------------------------------ |
| Java (JDK)              | 25+     | Backend-Entwicklung                              |
| Docker & Docker Compose | v2+     | Lokale Infrastruktur (PostgreSQL, Redis, Kafka)  |
| Git                     | 2.x     | Versionierung                                    |
| Python + pip            | 3.10+   | Doorstop (Requirement Management)                |
| Node.js                 | 20+     | Antora (Doku-Generierung), spaeter Angular/React |

### Optional

| Tool                                | Zweck                          |
| ----------------------------------- | ------------------------------ |
| IntelliJ IDEA / VS Code             | IDE                            |
| `doorstop` (`pip install doorstop`) | Requirement Management CLI     |
| `npx antora`                        | arc42-Dokumentation generieren |

## Schnellstart

```bash
# 1. Repository klonen
git clone <repo-url> && cd tomsblog

# 2. Infrastruktur starten
docker compose -f infra/docker/docker-compose.yml up -d

# 3. Build prüfen
./mvnw verify

# 4. Blog Content Service starten
./mvnw spring-boot:run -pl services/blog-content -Dspring-boot.run.profiles=local

# 5. (Optional) Dokumentation generieren
npx antora docs/antora-playbook.yml
```

## Lokale Services (Docker Compose)

```bash
# Starten
docker compose -f infra/docker/docker-compose.yml up -d

# Status
docker compose -f infra/docker/docker-compose.yml ps

# Stoppen (Daten behalten)
docker compose -f infra/docker/docker-compose.yml down

# Stoppen (Clean Slate)
docker compose -f infra/docker/docker-compose.yml down -v
```

| Service    | Port | Zugangsdaten                                           |
| ---------- | ---- | ------------------------------------------------------ |
| PostgreSQL | 5432 | User: `tomsblog`, Passwort: `tomsblog`, DB: `tomsblog` |
| Redis      | 6379 | kein Passwort                                          |
| Kafka      | 9092 | kein Auth                                              |
| Kafka UI   | 8080 | http://localhost:8080                                  |

## Maven-Befehle

```bash
./mvnw verify                    # Build + alle Tests
./mvnw test                      # Nur Tests
./mvnw spotless:apply            # Code formatieren
./mvnw spotless:check            # Format prüfen (CI)
./mvnw spring-boot:run -pl services/blog-content -Dspring-boot.run.profiles=local
```

## Spring-Profile

| Profil      | Verwendung                                       |
| ----------- | ------------------------------------------------ |
| `local`     | Lokale Entwicklung (Docker Compose Dependencies) |
| `test`      | Testcontainers (automatisch in Tests)            |
| _(default)_ | Produktion (Umgebungsvariablen fuer Secrets)     |

## Requirement Management (Doorstop)

```bash
pip install doorstop              # Einmalig installieren
doorstop                          # Validierung aller Links
doorstop publish all docs/requirements/  # HTML-Report
```

## Dokumentation generieren

```bash
# arc42 (Antora)
npx antora docs/antora-playbook.yml
# Output: build/site/

# ADRs lesen
ls docs/adr/
```

## Projekt-Struktur

```
tomsblog/
├── services/           # Microservices (Maven-Module)
│   └── blog-content/   # Erster Service
├── libs/               # Shared Libraries
│   ├── shared-kernel/  # Domain Primitives (framework-frei)
│   └── event-contracts/# Kafka Event Records
├── infra/
│   ├── docker/         # Docker Compose (lokale Deps)
│   └── k8s/            # Kubernetes Manifeste
├── docs/
│   ├── arc42/          # Architekturdokumentation (AsciiDoc/Antora)
│   ├── adr/            # Architecture Decision Records
│   └── antora-playbook.yml
├── reqs/               # Doorstop Requirements (STK, SWR, SWA, TST)
├── .github/            # CI, Agent-Customizations
├── pom.xml             # Root Parent POM
└── mvnw               # Maven Wrapper
```
