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

| Service    | Port | Zugangsdaten                             |
| ---------- | ---- | ---------------------------------------- |
| PostgreSQL | 5432 | `tomsblog` / `tomsblog` / DB: `tomsblog` |
| Redis      | 6379 | kein Passwort                            |
| Kafka      | 9092 | –                                        |
| Kafka UI   | 8080 | –                                        |

## Spring-Profil

Die Services verwenden das Profil `local` für die Verbindung zu den Docker-Containern:

```bash
./mvnw spring-boot:run -pl services/blog-content -Dspring-boot.run.profiles=local
```

Oder in der IDE: VM-Option `-Dspring.profiles.active=local`
