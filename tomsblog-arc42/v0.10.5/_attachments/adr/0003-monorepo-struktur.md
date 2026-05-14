# ADR-0003: Monorepo-Struktur

## Status

Accepted

## Context

Die Plattform besteht aus mehreren Microservices, Shared Libraries, Infrastruktur-Code und Dokumentation. Wir brauchen eine Entscheidung, ob wir ein Monorepo oder separate Repositories verwenden.

**Alternativen:**
- **Multi-Repo**: ein Repository pro Service, unabhängige Deployments
- **Monorepo**: alles in einem Repository, einfache Cross-Cutting-Änderungen

## Decision

Wir verwenden ein **Monorepo** mit folgender Struktur:

```
services/         # Microservices (Maven-Module)
libs/             # Shared Libraries (Maven-Module)
infra/            # Kubernetes, Docker Compose
docs/             # arc42, ADRs
reqs/             # Doorstop Requirements
.github/          # CI/CD, Agent-Customizations
```

Maven Multi-Module mit Parent POM verwaltet die Build-Abhängigkeiten.

## Consequences

**Positiv:**
- Atomare Commits über Service-Grenzen (z.B. Event-Contract + Producer + Consumer)
- Shared Libraries ohne Versionierungs-Overhead (SNAPSHOT im selben Build)
- Einheitliche CI-Pipeline
- Einfache Code-Navigation und Refactoring

**Negativ:**
- Größeres Repository über Zeit
- Alle Services werden bei `mvn verify` gebaut (mitigiert durch `-pl` und `--also-make`)
- Kein unabhängiges Versioning pro Service (akzeptabel für Lernprojekt)
