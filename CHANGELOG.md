# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased]

### Added

- REST-API für Quellenverwaltung (SWR-012): POST/GET/DELETE /api/posts/{postId}/sources
- SourceController, AddSourceCommand, RemoveSourceCommand, AddSourceRequest, RemoveSourceRequest, SourceResponse
- PostUseCase um addSource(), removeSource(), listSources() erweitert
- PostService implementiert Quellenverwaltung mit Persistence
- Unit-Tests für SourceController (10 Tests) und PostService-Quellenverwaltung (5 Tests)
- Software Detail Design Dokumentation um SourceController-Endpunkte erweitert
- Jazzer Fuzz Tests für alle externen REST-Endpoints (PostController, TagController, TranslationController)
- Software-Requirement SWR-023: Fuzz-Testing externer Interfaces
- Jazzer-JUnit 0.24.0 als Fuzz-Testing-Framework
- JaCoCo Code-Coverage-Plugin mit 95% Minimum-Schwellwert (Line + Branch Coverage)
- GitHub Actions CI: Coverage-Report als Artifact und Job Summary
- ADR-0019: Database per Service (PostgreSQL / MongoDB, dedizierte Instanz pro Service)
- ADR-0018: Dual-Broker-Strategie (Kafka für Event-Streaming, RabbitMQ für Task-Queues)
- RabbitMQ in Docker Compose für lokale Entwicklung (Port 5672, Management UI 15672)
- Architektur-Requirement SWA-014: RabbitMQ als Task-Queue-Broker
- Architektur-Requirement SWA-015: Database per Service
- RabbitMQ-Konfiguration in application-local.yml

### Changed

- Docker Compose: PostgreSQL-Container auf service-spezifischen Namen und DB umgestellt (blog_content)
- ADR-0006 auf Status "Superseded by ADR-0018" gesetzt
- SWA-005 aktualisiert: Kafka-Verantwortung auf Event-Streaming eingegrenzt
- Arc42-Dokumentation um RabbitMQ- und Database-per-Service-Abschnitte erweitert
- ROADMAP Phase 5 um RabbitMQ-Tasks erweitert
- Tech-Stack-Beschreibung in AGENTS.md, README.md und libs/README.md aktualisiert

- Hexagonal architecture for blog-content service with ports and adapters
- Domain model: Post, Translation, Tag, Attachment, Source, PostLocale
- Application layer: PostUseCase, TagUseCase, TranslationUseCase with service implementations
- Inbound REST adapters: PostController, TagController, TranslationController
- Outbound JPA persistence adapters with PostgreSQL
- Shared Kernel library (DomainEvent, AggregateRoot, Auditable, TenantId, AuthorId)
- Event Contracts library for Kafka domain events
- Integration tests with Testcontainers (PostgreSQL)
- CI pipeline with GitHub Actions (build, test, container image)
- Software Detail Design documentation (AsciiDoc + Antora)
- arc42 architecture documentation
- ADRs for key decisions (hexagonal architecture, REST/GraphQL, Kafka, gRPC, etc.)
- Doorstop requirements tracing (STK, SWR, SWA, IMP, TST)
- Flyway database migrations
- Docker Compose for local development (PostgreSQL, Redis, Kafka)
- Roadmap with 6 milestones
- Maven multi-module project structure (parent, shared-kernel, event-contracts, blog-content)
- Upgrade spotless-maven-plugin from 2.44.0 to 3.4.0 (JDK 25 compatibility)
- Build & Test verification rule in AGENTS.md
- CI job to generate Antora documentation and attach as workflow artifact
- CI job to validate Doorstop requirements and attach coverage report as workflow artifact
- CI build triggers on tags and releases
- CI release-assets job attaches JAR, documentation, and requirements report to GitHub releases
- Antora versioning: docs are versioned from tags (v*), main branch produces dev prerelease
- CI deploys generated documentation to GitHub Pages via docs branch
- Stakeholder requirement STK-013: zeitgesteuerte Veröffentlichung
- Software requirement SWR-022: time-based auto publishing API
- ADR-0016: Backend for Frontend (BFF) pattern
- ADR-0017: API Gateway (Spring Cloud Gateway)
- Milestone 7: Android-App für Autoren (Kotlin, Jetpack Compose, Mobile BFF)

### Fixed

- Removed default "Products" and "Services" entries from Antora documentation header
- Added navigation header with links to Architektur (arc42), Software Detail Design, and Requirements
- Doorstop requirements report integrated into GitHub Pages docs site
- PlantUML diagrams: moved puml files to partials/ directory for correct Antora partial$ resolution
- ADR links: fixed broken attachmentsdir paths, symlinked ADRs into Antora attachments
- ADR index updated with missing ADRs (0009, 0015, 0016, 0017)
- Doorstop HTML pages: injected navigation bar with back link to main documentation
- CI Node.js 20 deprecation warnings by opting into Node.js 24 for actions
- Spotless/Palantir Java Format crash on JDK 25
- Compiler warnings (null safety, unused imports)
