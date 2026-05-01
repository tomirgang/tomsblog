# Entwicklungsumgebung

## Voraussetzungen

| Tool                    | Version | Zweck                                            |
| ----------------------- | ------- | ------------------------------------------------ |
| Java (JDK)              | 25+     | Backend-Entwicklung                              |
| Docker & Docker Compose | v2+     | Lokale Infrastruktur (PostgreSQL, Redis, Kafka, RabbitMQ) |
| Git                     | 2.x     | Versionierung                                    |
| Python + pip            | 3.10+   | Doorstop (Requirement Management)                |
| Node.js                 | 20+     | Antora (Doku-Generierung), spaeter Angular/React |
| PlantUML (via Docker)   |         | UML-Diagramme in arc42                           |
| Graphviz                |         | Layout-Engine fuer PlantUML                      |

### Empfohlene Version-Manager

| Tool   | Verwaltet      | Installation                            |
| ------ | -------------- | --------------------------------------- |
| SDKMAN | Java, Maven    | https://sdkman.io                       |
| nvm    | Node.js, npm   | https://github.com/nvm-sh/nvm          |
| venv   | Python-Pakete  | Bestandteil von Python 3 (Standardlib) |

### Optional

| Tool                                | Zweck                              |
| ----------------------------------- | ---------------------------------- |
| IntelliJ IDEA / VS Code             | IDE                                |
| `doorstop` (`pip install doorstop`) | Requirement Management CLI         |
| `npx antora`                        | arc42-Dokumentation generieren     |
| VS Code Extensions                  | siehe `.vscode/extensions.json`    |

## Devcontainer (empfohlen)

Der schnellste Weg zur vollstaendigen Entwicklungsumgebung ist der Devcontainer.
Er enthaelt alle Tools vorkonfiguriert (JDK 25, Node 20, Python, Graphviz, PlantUML-Server).

**Voraussetzungen:** Docker und VS Code mit der Extension "Dev Containers"
(oder JetBrains Gateway).

```bash
# Repository klonen und im Devcontainer oeffnen
git clone <repo-url> && cd tomsblog
code .
# VS Code: Ctrl+Shift+P -> "Dev Containers: Reopen in Container"
```

Der Devcontainer startet automatisch:
- Infrastruktur (PostgreSQL, Redis, Kafka, RabbitMQ) via Docker Compose
- PlantUML-Server auf Port 8180
- Python venv mit Doorstop
- Maven Dependency Cache

Danach ist das Projekt sofort build-faehig (`./mvnw verify`).

## Manuelle Toolchain einrichten

Falls kein Devcontainer genutzt wird, muessen die Tools manuell installiert werden.

### 1. SDKMAN (Java + Maven)

SDKMAN verwaltet parallele JDK- und Maven-Versionen.

```bash
# SDKMAN installieren
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# Verfuegbare Java-Versionen anzeigen
sdk list java

# JDK 25 installieren (Beispiel: Eclipse Temurin)
sdk install java 25-tem

# Als Standard setzen
sdk default java 25-tem

# Maven installieren (optional, Projekt nutzt Maven Wrapper)
sdk install maven

# Pruefung
java -version
mvn -version
```

Hinweis: Das Projekt enthaelt einen Maven Wrapper (`./mvnw`), sodass Maven
nicht zwingend separat installiert werden muss. SDKMAN ist aber nuetzlich,
um schnell zwischen JDK-Versionen zu wechseln.

### 2. nvm (Node.js)

nvm verwaltet parallele Node.js-Versionen.

```bash
# nvm installieren
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.3/install.sh | bash

# Shell neu laden oder manuell aktivieren
export NVM_DIR="$HOME/.nvm"
[ -s "$NVM_DIR/nvm.sh" ] && . "$NVM_DIR/nvm.sh"

# Node.js 20 LTS installieren
nvm install 20

# Als Standard setzen
nvm alias default 20

# Pruefung
node --version
npm --version
npx --version
```

### 3. Python venv (Doorstop)

Das Projekt nutzt ein lokales Virtual Environment fuer Python-Abhaengigkeiten.

```bash
# venv erstellen (einmalig)
python3 -m venv .venv

# venv aktivieren
source .venv/bin/activate

# Abhaengigkeiten installieren
pip install -r requirements.txt

# Pruefung
doorstop --version
```

Das venv muss vor jeder Nutzung von `doorstop` aktiviert werden.
Alternativ kann man `.venv/bin/doorstop` direkt aufrufen.

### 4. PlantUML und Graphviz (Diagramme)

Die arc42-Dokumentation nutzt PlantUML-Diagramme. Fuer die lokale Vorschau
gibt es mehrere Optionen:

**Option A: Docker (empfohlen)**

```bash
# PlantUML-Server lokal starten
docker run -d --name plantuml -p 8180:8080 plantuml/plantuml-server:jetty
```

VS Code Extension `jebbs.plantuml` in `.vscode/settings.json` konfigurieren:

```json
{
  "plantuml.server": "http://localhost:8180",
  "plantuml.render": "PlantUMLServer"
}
```

**Option B: Lokale Installation**

```bash
# Ubuntu/Debian
sudo apt install graphviz plantuml

# macOS
brew install graphviz plantuml
```

### 5. VS Code Extensions

Empfohlene Extensions sind in `.vscode/extensions.json` definiert.
VS Code bietet beim Oeffnen des Projekts automatisch die Installation an.

Manuell installieren:

```bash
code --install-extension asciidoctor.asciidoctor-vscode
code --install-extension yzhang.markdown-all-in-one
code --install-extension jebbs.plantuml
code --install-extension bierner.markdown-mermaid
code --install-extension tomoyukim.vscode-mermaid-editor
```

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
npx antora antora-playbook.yml
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
| Kafka UI   | 9080 | http://localhost:9080                                  |

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

### Automatische Header-Injection (DefaultTenantFilter)

Der `DefaultTenantFilter` injiziert automatisch Default-Werte fuer `X-Tenant-Id`
und `X-Author-Id`, wenn diese HTTP-Header fehlen. Dies ermoeglicht das Testen der
Thymeleaf-UI im Browser ohne manuelle Header-Konfiguration und macht den MVP
produktiv nutzbar (Single-Tenant-Betrieb).

Die Werte sind per Property konfigurierbar (`application.yml` oder Umgebungsvariablen):

```yaml
blog:
  default-tenant-id: "00000000-0000-0000-0000-000000000001"
  default-author-id: "00000000-0000-0000-0000-000000000001"
```

Umgebungsvariablen: `BLOG_DEFAULT_TENANT_ID`, `BLOG_DEFAULT_AUTHOR_ID`

In einer spaeteren Phase wird dieser Filter durch domain-basierte Tenant-Resolution
(ADR-0012) und Authentifizierung (Phase 4) ersetzt.

## Requirement Management (Doorstop)

```bash
pip install doorstop              # Einmalig installieren
doorstop                          # Validierung aller Links
doorstop publish all docs/requirements/  # HTML-Report
```

## Dokumentation generieren

```bash
# arc42 (Antora)
npx antora antora-playbook.yml
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
│   └── adr/            # Architecture Decision Records
├── reqs/               # Doorstop Requirements (STK, SWR, SWA, TST)
├── .github/            # CI, Agent-Customizations
├── antora-playbook.yml # Antora Playbook (Doku-Build)
├── pom.xml             # Root Parent POM
└── mvnw               # Maven Wrapper
```
