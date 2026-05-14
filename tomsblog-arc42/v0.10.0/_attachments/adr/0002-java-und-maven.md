# ADR-0002: Java und Maven als Technologie-Basis

## Status

Accepted

## Context

Für die Backend-Services brauchen wir eine Programmiersprache und ein Build-System. Das Projekt dient als Lernplattform für Cloud-Native-Architekturen.

**Sprach-Alternativen:**
- **Kotlin**: moderner, weniger Boilerplate, aber zusätzliche Lernkurve
- **Java**: ausgereiftes Ökosystem, breite Tooling-Unterstützung, Java Records reduzieren Boilerplate

**Build-Alternativen:**
- **Gradle (Kotlin DSL)**: flexibel, schneller bei inkrementellen Builds
- **Maven**: konventionsbasiert, deklarativ, breiter IDE-Support, einfachere Reproduzierbarkeit

## Decision

Wir verwenden **Java 25 (LTS)** als Programmiersprache und **Maven** als Build-System mit Multi-Module-Struktur.

- Java Records für DTOs, Events und Value Objects
- Maven Parent POM mit Spring Boot Starter Parent
- Maven Wrapper (`mvnw`) im Repository für reproduzierbare Builds

## Consequences

**Positiv:**
- Breite Tooling-Unterstützung (IDE, CI, Analyse-Tools)
- Deklaratives Build-System, weniger "Build-Logik-Bugs"
- Java 25 bringt Records, Pattern Matching, Virtual Threads
- Maven Central hat das breiteste Ökosystem

**Negativ:**
- Java ist verbosier als Kotlin (gemildert durch Records und moderne Features)
- Maven ist langsamer als Gradle bei großen Projekten
- XML-basierte Konfiguration (POM) ist weniger elegant als Kotlin DSL
