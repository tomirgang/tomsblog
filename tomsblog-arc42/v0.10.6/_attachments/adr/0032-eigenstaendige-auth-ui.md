# ADR-0032: Eigenständige UI für Authentifizierung und Benutzerverwaltung

## Status

Proposed

## Context

Der Blog Content Service hostet aktuell die gesamte Authentifizierungs- und Benutzerverwaltungs-UI: LoginController, RegistrationController, AdminController (User/Settings), SyncingOidcUserService, TenantAwareClientRegistrationRepository, SecurityConfiguration (OIDC-Flows) sowie sechs Thymeleaf-Templates (login, register, admin-login, admin/users, admin/settings, registration-success). Diese Komponenten machen ca. 50% des Web-Adapter-Layers aus, gehören aber fachlich zur User-Management-Domäne.

### Probleme

1. **Bounded Context Verletzung**: Blog Content kennt LoginMode, Passwort-Policies, User-Approval-Workflows und OIDC-Provider-Konfiguration. Das sind Konzepte der User-Management-Domäne.

2. **Synchrone gRPC-Kopplung für eigene UI**: Für jede Login-Seite muss der Blog Content Service per gRPC die TenantSettings abfragen, obwohl der User Management Service diese Daten lokal hat.

3. **Deployment-Kopplung**: Änderungen am Registrierungsflow oder Login-Mode erfordern ein Re-Deployment des Blog Content Service.

4. **Widerspruch zu ADR-0016 (BFF)**: Die User-UI im Blog Content Service ist ein implizites BFF für User Management.

5. **Widerspruch zu ADR-0017 (API Gateway)**: Das Gateway-Routing sieht `/auth/**` als eigene Route vor, aber die Auth-UI lebt unter Blog Content Routen (`/login`, `/register`, `/admin/users`).

### Evaluierte Alternativen

**Status Quo (UI im Blog Content Service):**

- Vorteil: Kein Migrationsaufwand
- Nachteil: Wachsende Kopplung, Blog Content wird zur Sammelstelle für fremde Domänenlogik

**Eigene UI im User Management Service:**

- Vorteil: Saubere Bounded Contexts, unabhängige Deployments, keine gRPC-Umwege für eigene Daten
- Nachteil: Redirect-basierter Login-Flow, Shared Session Store (Redis) wird Pflicht

**Separate Auth-UI als eigener BFF-Service:**

- Vorteil: Maximale Trennung
- Nachteil: Dritter Service für reine UI-Orchestrierung, Over-Engineering für Meilenstein 1

## Decision

Die **Authentifizierungs- und Benutzerverwaltungs-UI wird in den User Management Service verschoben**. Der User Management Service erhält eine eigene Thymeleaf-basierte Web-Oberfläche.

### Was zum User Management Service wandert

| Komponente                              | Neuer Pfad                   |
| --------------------------------------- | ---------------------------- |
| LoginController + login.html            | /auth/login                  |
| Admin-Login + admin-login.html          | /auth/admin/login            |
| RegistrationController + register.html  | /auth/register               |
| registration-success.html               | /auth/registration-success   |
| AdminController (Users) + users.html    | /auth/admin/users            |
| AdminController (Settings) + settings.html | /auth/admin/settings      |
| SyncingOidcUserService                  | Intern im User Mgmt Service  |
| TenantAwareClientRegistrationRepository | Intern im User Mgmt Service  |
| SecurityConfiguration (OIDC/Form-Login) | Intern im User Mgmt Service  |
| LoginRateLimitFilter                    | Intern im User Mgmt Service  |

### Was im Blog Content Service bleibt

- BlogViewController + Blog-Templates (Post-Lese- und Schreibansichten)
- PostFormData + Post-Formulare
- LegalController + Impressum/Privacy (bis ADR-0031 aktiv, dann zum Tenant Service)
- Vereinfachte SecurityConfiguration: nur Session-Validierung via Redis, kein OIDC-Flow
- TenantBrandingAdvice, DefaultTenantFilter, VersionModelAdvice

### Authentifizierungsflow (Ziel)

```
Benutzer          Blog Content Service        User Mgmt Service          Redis
   │                     │                          │                      │
   │── GET /posts ──────►│                          │                      │
   │◄── Öffentliche ─────│                          │                      │
   │    Seite            │                          │                      │
   │                     │                          │                      │
   │── Klick "Login" ───►│                          │                      │
   │◄── Redirect ────────│─── /auth/login ─────────►│                      │
   │                     │                          │                      │
   │── GET /auth/login ────────────────────────────►│                      │
   │◄── Login-Formular ─────────────────────────────│                      │
   │    (oder OIDC-Redirect)                        │                      │
   │                     │                          │                      │
   │── POST /auth/login ───────────────────────────►│                      │
   │                     │                          │── Session ──────────►│
   │◄── Redirect /posts ────────────────────────────│                      │
   │                     │                          │                      │
   │── GET /posts ──────►│                          │                      │
   │                     │── Session lesen ───────────────────────────────►│
   │                     │◄── Rollen ──────────────────────────────────────│
   │◄── Geschützte ──────│                          │                      │
   │    Seite            │                          │                      │
```

### Gateway-Routing (Ergänzung zu ADR-0017)

| Route            | Ziel                    |
| ---------------- | ----------------------- |
| /auth/**         | User Management Service |
| /api/**          | Web BFF / Blog Content  |
| /posts/**, /     | Blog Content Service    |
| /impressum, /privacy | Blog Content Service |

### Abhängigkeiten

- **ADR-0030 (Redis Session Store)**: Wird zur Voraussetzung, da beide Services die gleiche Session lesen/schreiben müssen (SWR-064).
- **ADR-0031 (Tenant Service)**: Wenn umgesetzt, wandern die Tenant-Settings-Teile der Admin-UI perspektivisch weiter zum Tenant Service. Die User-Verwaltungs-UI bleibt beim User Management Service.
- **Shared CSS/Layout**: Ein gemeinsames Thymeleaf-Layout-Fragment oder CSS-Paket sorgt für konsistentes Branding über Service-Grenzen hinweg.

## Consequences

**Positiv:**

- Saubere Bounded Contexts: Jeder Service hostet nur UI für seine eigene Domäne
- Unabhängige Deployments: Login-Flow-Änderungen erfordern kein Blog Content Deployment
- User Management Service greift für seine UI direkt auf die eigene DB zu (kein gRPC-Umweg)
- Konsistent mit ADR-0016 (BFF) und ADR-0017 (Gateway Routing)
- Blog Content Service wird deutlich schlanker (ca. 50% weniger Web-Adapter-Code)
- Folgt dem Standard-OIDC-Pattern (Auth-Server hat eigene UI)

**Negativ:**

- Initialer Migrationsaufwand (Code + Templates verschieben, Tests anpassen)
- Redirect-basierter Login-Flow (zusätzlicher HTTP-Roundtrip)
- Redis wird zur harten Abhängigkeit für Mehrbenutzerbetrieb
- Template-Styling muss über Service-Grenzen konsistent bleiben
- Zwei Services brauchen Thymeleaf-Konfiguration

## References

- SWR-016: OIDC Login
- SWR-028: Break-Glass Admin-Login
- SWR-044: Konfigurierbare Login-Methode
- SWR-051: Admin-UI Benutzerliste
- SWR-052: Admin-UI Tenant-Einstellungen
- SWR-053: SuperAdmin Tenant-Umschaltung
- SWR-059: REST-API lokale Benutzerregistrierung
- SWR-060: Thymeleaf-Registrierungsseite
- SWR-063: Dynamische OIDC-Client-Registrierung
- SWR-064: Shared Redis Session
- ADR-0016: Backend for Frontend
- ADR-0017: API Gateway
- ADR-0027: Authentik OIDC
- ADR-0030: Redis Session Store
- ADR-0031: Eigenständiger Tenant Service
