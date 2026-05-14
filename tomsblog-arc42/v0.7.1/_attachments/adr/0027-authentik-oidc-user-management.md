# ADR-0027: Leichtgewichtiges User Management mit Authentik als OIDC Provider

## Status

Accepted (supersedes ADR-0011)

## Kontext

Die Plattform benötigt eine Benutzerverwaltung mit Login, Rollen, Sessions und Multi-Faktor-Authentifizierung (MFA). ADR-0011 entschied sich für einen komplett eigenen Auth-Service, der OAuth2/OIDC selbst implementiert. Bei genauerer Betrachtung ergeben sich folgende Probleme mit diesem Ansatz:

- **MFA-Implementierung** (TOTP, WebAuthn, Push) ist sicherheitskritisch und komplex
- **Account Recovery**, Passwort-Reset-Flows und Brute-Force-Protection müssen selbst gebaut werden
- **Security Audits** und regelmäßige Vulnerability-Patches für Auth-Code binden dauerhaft Kapazität
- Der Lerneffekt bei OIDC-Flows bleibt auch als Relying Party (Consumer) erhalten

Gleichzeitig ist Keycloak für ein einzelnes Projekt überdimensioniert (Java-Monolith, hoher Speicherverbrauch, komplexes Upgrade-Verfahren).

**Authentik** bietet eine moderne Alternative:

- Leichtgewichtig (Python/Django, geringer Speicherverbrauch)
- Integrierte MFA (TOTP, WebAuthn, Duo, SMS)
- OIDC/OAuth2 Provider out-of-the-box
- Einfache Deployment-Optionen (Docker, Kubernetes via Helm)
- Admin-UI für Benutzer- und Applikationsverwaltung
- Flow-basierte Konfiguration (Login, Registration, Recovery)

## Entscheidung

Wir implementieren eine **zweistufige Architektur**:

### 1. Authentik als externer OIDC Provider

- Eine bestehende Authentik-Instanz (`https://auth.do9ita.de`) wird als externer OIDC Provider genutzt
- Authentik wird **nicht** im Kubernetes-Cluster deployed, sondern extern betrieben
- Verantwortlich für: Login-Flows, MFA, Session-Management, Passwort-Policies
- Stellt OIDC-Tokens (ID Token + Access Token) aus
- Multi-Faktor-Authentifizierung (TOTP + WebAuthn) für alle Benutzer
- Konfiguration der Blog-Plattform als OIDC Application in Authentik
- OIDC Discovery Endpoint: `https://auth.do9ita.de/application/o/<app-slug>/.well-known/openid-configuration`

### 2. Leichtgewichtiger User-Management-Service (intern)

- Eigener Microservice innerhalb der Blog-Plattform
- Verantwortlich für: plattformspezifische Benutzerprofile, Rollen (ADMIN, AUTHOR, READER), Tenant-Zuordnung
- Agiert als OIDC Relying Party (Spring Security OAuth2 Client/Resource Server)
- Synchronisiert Benutzerdaten aus dem OIDC ID Token (Sub, Email, Name)
- Verwaltet plattformspezifische Attribute (Rollen, Tenant-Mitgliedschaften, Einstellungen)
- Stellt interne JWTs aus für Service-zu-Service-Kommunikation (optional, falls nötig)

### Authentifizierungs-Flow

1. Benutzer klickt "Login" auf der Blog-Plattform
2. Redirect zu Authentik (Authorization Code Flow mit PKCE)
3. Authentik authentifiziert (Passwort + MFA)
4. Redirect zurück mit Authorization Code
5. User-Management-Service tauscht Code gegen Tokens
6. User-Management-Service erstellt/aktualisiert lokales Benutzerprofil
7. Session wird etabliert (oder internes JWT ausgestellt)

### Rollen-Mapping

- Authentik-Gruppen werden auf Plattform-Rollen gemappt (konfigurierbar)
- Alternativ: Rollen werden ausschließlich im internen User-Management verwaltet
- RBAC: ADMIN (alles), AUTHOR (eigene Posts), READER (nur lesen)

## Konsequenzen

**Positiv:**

- MFA, Brute-Force-Protection und Account-Recovery sind sofort verfügbar (Authentik)
- Geringerer Implementierungsaufwand für sicherheitskritische Flows
- Volle Kontrolle über plattformspezifische Benutzerdaten und Rollen
- Lerneffekt bei OIDC Relying Party Implementierung bleibt erhalten
- Authentik ist leichtgewichtiger als Keycloak
- Klare Trennung: Authentifizierung (Authentik) vs. Autorisierung (intern)
- Einfacher Providerwechsel möglich (Standard OIDC)

**Negativ:**

- Abhängigkeit von externer Authentik-Instanz (Verfügbarkeit)
- Abhängigkeit von Authentik-Projekt (Community-driven)
- Netzwerk-Roundtrip zu Authentik bei jedem Login
- Zwei Systeme zu pflegen (Authentik + interner Service)

## Referenzen

- ADR-0011: Custom Identity Provider mit OIDC (superseded)
- STK001: Blog-Posts erstellen und veröffentlichen (benötigt Autorenrolle)
- SWR-007: Rollenbasierte Zugriffskontrolle
- SWR-016: Custom Identity Provider mit OIDC
- SWA-010: Custom Auth Service als eigener Microservice
