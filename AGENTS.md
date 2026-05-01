# Project Guidelines

## Overview

Multi-tenant blog & podcast platform as a learning project for cloud-native architectures.
Current focus: **Meilenstein 1 – vollständiger Text-Blog mit Thymeleaf-UI**.

See [ROADMAP.md](ROADMAP.md) for the full milestone plan and [TODO.md](TODO.md) for actionable tasks.

## Architecture

- **Hexagonale Architektur** (Ports & Adapters) for each service
- **Microservice-Landschaft** with event-driven communication via Kafka
- **Audit-fähig**: all relevant actions must be traceable
- **Multi-Tenant**: tenant isolation at the data layer

## Tech Stack

| Layer         | Technology                                                                   |
| ------------- | ---------------------------------------------------------------------------- |
| Backend       | Spring Boot (Java), Maven                                                    |
| Database      | PostgreSQL (CloudNativePG), Redis                                            |
| Messaging     | Apache Kafka                                                                 |
| UI (Phase 1)  | Thymeleaf (SSR)                                                              |
| UI (later)    | Angular + Nx + Native Federation (REST), React + Module Federation (GraphQL) |
| Infra         | Kubernetes (Hetzner), ArgoCD/Flux, Helm/Kustomize                            |
| Observability | Prometheus, Grafana, Loki, Tempo                                             |
| AI            | OpenRouter                                                                   |
| Requirements  | Doorstop (YAML in Git, ASPICE-oriented traceability)                         |
| Docs          | arc42 (AsciiDoc + Antora), Detail Design (AsciiDoc + Antora + Mermaid), ADRs (Markdown + MkDocs) |

## Conventions

- Sprache im Code und Commits: **Englisch**
- Dokumentation und Planungsdateien: **Deutsch**
- Hexagonal structure per service: `domain/`, `application/` (ports), `adapter/` (inbound + outbound)
- Tests: Unit tests + Integration tests with Testcontainers
- API documentation: OpenAPI/Swagger for REST, GraphQL schema for GraphQL services
- Events: publish domain events for cross-service communication (Post created/updated/published)
- Requirements: Doorstop for multi-level tracing (Stakeholder → Software Req → Design → Implementation → Test)
- Requirement IDs in test names (`@DisplayName("SWR-042: ...")`) and Javadoc (`@req SWR-042`)
- Diagrams in arc42: PlantUML as separate `.puml` files in `docs/arc42/modules/ROOT/images/plantuml/`, referenced from AsciiDoc via `plantuml::partial$...`
- Diagrams in design docs: Mermaid diagrams inline in AsciiDoc via `[mermaid]` blocks
- Only use plain ASCII chars in PlantUML (no umlauts, use ae/oe/ue instead)
- All other files (AsciiDoc, Markdown, YAML, Java Javadoc) use proper German umlauts (ä, ö, ü, ß)
- Software Detail Design documentation in `docs/design/` using AsciiDoc + Antora (separate Antora component)
- All Doorstop requirements (SWR, SWA) must be referenced from the architecture docs (arc42) and software detail design docs at the appropriate locations
- Requirement references and diagram includes must be accompanied by explaining text that provides context (never a bare include/reference without surrounding prose)

## Build & Test

```bash
# (will be configured once the Maven project is set up)
./mvnw verify
./mvnw test
```

## Requirements (Doorstop)

```bash
doorstop publish all docs/requirements/   # Export HTML traceability report
doorstop                                   # Validate all links & coverage
```

Requirement document hierarchy:
- `STK` – Stakeholder Requirements
- `SWR` – Software Requirements
- `SWA` – Software Architecture/Design
- `TST` – Test Specifications

## Key Decisions

- REST for the text-blog APIs (consumed by Thymeleaf and later Angular)
- GraphQL for podcast/video services (consumed by React UIs)
- gRPC for synchronous inter-service communication
- Keycloak or custom auth (to be evaluated) for identity
- S3-compatible storage (Garage on Netcup VM) for media and backups

## Formatting remarks

- Never use – as separator in texts.
- All tables in Markdown files shall be formatted readable in the source.
