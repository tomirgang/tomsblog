# ADR-0007: Doorstop für Requirement Tracing (ASPICE-orientiert)

## Status

Accepted

## Context

Das Projekt soll nachvollziehbare Requirements mit durchgängiger Traceability haben, orientiert am ASPICE-Prozessmodell. Wir brauchen ein Tool, das:
- Git-native ist (Requirements im Repository, versioniert)
- Multi-Level-Hierarchie unterstützt (Stakeholder → Software → Design → Test)
- Bidirektionale Links zwischen Requirement-Ebenen erlaubt
- CI-integrierbar ist (Validierung von Coverage und Links)

**Alternativen:**
- **StrictDoc**: moderner, aber weniger verbreitet
- **Doorstop**: etabliert, YAML-basiert, CLI-getrieben
- **Eigenbau**: maximale Kontrolle, hoher Aufwand

## Decision

Wir verwenden **Doorstop** für das Requirement Management.

**Dokumenthierarchie:**

| Prefix | ASPICE-Level | Beschreibung             |
| ------ | ------------ | ------------------------ |
| STK    | SWE.1        | Stakeholder Requirements |
| SWR    | SWE.2        | Software Requirements    |
| SWA    | SWE.3        | Architecture/Design      |
| TST    | SWE.5/6      | Test Specifications      |

**Traceability in Code:**
- Javadoc: `@req SWR-042`
- Tests: `@DisplayName("SWR-042: ...")`
- CI validiert: `doorstop` (Link-Integrität + Coverage)

## Consequences

**Positiv:**
- Requirements leben im Git-Repo (versioniert, reviewbar, mergebar)
- Kein externer Server nötig
- Automatische Coverage-Reports und Link-Validierung
- ASPICE-orientierte Struktur als Lernerfahrung

**Negativ:**
- YAML-Dateien sind weniger komfortabel als GUI-Tools (mitigiert durch VS Code Extension)
- Doorstop-Community ist kleiner als kommerzielle Tools
- Manuelle Pflege der Requirement-Links nötig
