# Lokale Entwicklungsumgebung

Alle externen Abhängigkeiten laufen via Docker Compose. Die Spring-Boot-Services selbst werden lokal (IDE oder `./mvnw`) gestartet.

## Voraussetzungen

- Docker & Docker Compose (v2)
- Java 25+ (für `./mvnw`)
- Optional: `doorstop` (pip install doorstop)

## Starten

```bash
# Alle Dependencies starten
docker compose -f infra/docker/docker-compose.yml up -d

# Status prüfen
docker compose -f infra/docker/docker-compose.yml ps

# Logs anschauen
docker compose -f infra/docker/docker-compose.yml logs -f kafka
```

## Stoppen

```bash
docker compose -f infra/docker/docker-compose.yml down

# Mit Volume-Löschung (Clean Slate)
docker compose -f infra/docker/docker-compose.yml down -v
```

## Services & Ports

Database per Service (ADR-0019): Jeder Service erhält eine eigene Datenbank-Instanz.

| Service                  | Port  | Zugangsdaten                                     |
| ------------------------ | ----- | ------------------------------------------------ |
| PostgreSQL (Blog Content)| 5432  | `tomsblog` / `tomsblog` / DB: `blog_content`    |
| Redis                    | 6379  | kein Passwort                                    |
| Kafka                    | 9092  | -                                                |
| Kafka UI                 | 8080  | -                                                |
| RabbitMQ                 | 5672  | `tomsblog` / `tomsblog` / VHost: `tomsblog`     |
| RabbitMQ Mgmt            | 15672 | `tomsblog` / `tomsblog`                          |

Weitere Service-Datenbanken werden bei Bedarf hinzugefügt (eigene Container, eigene Ports).

Die Zugangsdaten sind über Umgebungsvariablen konfigurierbar (z.B. `BLOG_CONTENT_DB_USER`,
`BLOG_CONTENT_DB_PASSWORD`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD`). Ohne gesetzte Variablen
gelten die oben angegebenen Defaults.

## Spring-Profil

Die Services verwenden das Profil `local` für die Verbindung zu den Docker-Containern:

```bash
./mvnw spring-boot:run -pl services/blog-content -Dspring-boot.run.profiles=local
```

Oder in der IDE: VM-Option `-Dspring.profiles.active=local`
