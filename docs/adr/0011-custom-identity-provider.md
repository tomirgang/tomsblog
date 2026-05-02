# ADR-0011: Custom Identity Provider mit OIDC

## Status

Superseded by [ADR-0027](0027-authentik-oidc-user-management.md)

## Context

Die Plattform benötigt eine Benutzerverwaltung mit Login, Rollen und Sessions. Zwei Optionen stehen zur Auswahl:

1. **Keycloak**: Mächtiger, etablierter Identity Provider mit umfangreichen Features
2. **Custom Auth Service**: Eigener Microservice mit gezielter Funktionalität

Keycloak bringt hohe Betriebskomplexität mit (Java-Monolith, eigene DB, regelmäßige Updates, Theme-Anpassungen). Für ein Lernprojekt bietet ein eigener Service mehr Einblick in OAuth2/OIDC-Mechanismen.

## Decision

Wir implementieren einen **eigenen Auth Service** als Microservice mit folgenden Eigenschaften:

- Unterstützung als OIDC Relying Party (Login via externe Provider wie Google, GitHub)
- Eigene Benutzerverwaltung (lokale Accounts + OIDC-verknüpfte Accounts)
- JWT-Ausstellung für die interne Service-Kommunikation
- RBAC mit den Rollen ADMIN, AUTHOR, READER
- Token-Validierung durch andere Services lokal via Public Key (kein Introspection-Roundtrip)

**Kein Keycloak** aufgrund:
- Geringerer Betriebsaufwand (kein separater Keycloak-Cluster)
- Volle Kontrolle über das Datenmodell
- Besserer Lerneffekt (OAuth2/OIDC-Implementierung verstehen)
- Schlankerer Footprint auf dem Kubernetes-Cluster

## Consequences

**Positiv:**
- Vollständige Kontrolle über Authentifizierung und Autorisierung
- Geringerer Infrastruktur-Overhead
- Tiefes Verständnis von OIDC/OAuth2-Flows
- Einfache Integration der Tenant-Zuordnung in den Auth-Flow

**Negativ:**
- Höherer Implementierungsaufwand (vs. Keycloak out-of-the-box)
- Sicherheitskritischer Service muss selbst korrekt implementiert werden
- Features wie 2FA, Account Recovery müssen selbst gebaut werden
- Keine Community-Plugins oder Admin-UI von Keycloak

## References

- SWR-016: Custom Identity Provider mit OIDC
- SWR-007: Rollenbasierte Zugriffskontrolle
- SWA-010: Custom Auth Service als eigener Microservice
