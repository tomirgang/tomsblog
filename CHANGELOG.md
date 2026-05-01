# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased]

### Added

- Upgrade spotless-maven-plugin from 2.44.0 to 3.4.0 (JDK 25 compatibility)
- Build & Test verification rule in AGENTS.md

### Fixed

- Spotless/Palantir Java Format crash on JDK 25
- Compiler warnings (null safety, unused imports)

## [0.0.1-SNAPSHOT] - 2026-05-01

### Added

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
