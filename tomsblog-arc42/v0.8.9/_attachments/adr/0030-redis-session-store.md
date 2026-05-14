# ADR-0030: Redis als externer Session-Store für horizontale Skalierung

## Status

Proposed

## Context

Der Blog Content Service nutzt aktuell HTTP-Sessions für die Authentifizierung (OIDC Login, Break-Glass SuperAdmin Login, Tenant-Umschaltung). Die Sessions werden im Tomcat Servlet Container im Speicher gehalten (In-Memory). Das bedeutet:

1. **Kein horizontales Scaling**: Bei mehreren Replicas gehen Sessions verloren, wenn ein Request an einen anderen Pod geroutet wird
2. **Session-Verlust bei Restart**: Jeder Pod-Neustart (Deployment, Rolling Update) führt zum Logout aller Benutzer
3. **Kein Zero-Downtime-Deployment**: Obwohl die Rolling-Update-Strategie konfiguriert ist, verlieren Benutzer beim Switch auf den neuen Pod ihre Session

Die Session speichert aktuell:
- Spring Security Authentication (OIDC oder formbasiert)
- Benutzerrollen als Spring Security Authorities
- SuperAdmin Tenant-Auswahl (SWR-053)

### Evaluierte Alternativen

**Sticky Sessions (sessionAffinity: ClientIP):**
- Einfachste Lösung, kein zusätzlicher Infrastruktur-Aufwand
- Nachteil: Sessions gehen bei Pod-Restart trotzdem verloren
- Nachteil: Ungleichmäßige Lastverteilung bei wenigen Clients
- Nachteil: Hinter NAT/Proxies landen viele Clients am selben Pod

**Spring Session mit JDBC (PostgreSQL):**
- Kein zusätzlicher Infrastruktur-Service nötig
- Nachteil: Höhere Latenz pro Request (DB-Roundtrip für jede Session-Operation)
- Nachteil: Zusätzliche Last auf der PostgreSQL-Instanz
- Nachteil: Regelmäßiges Aufräumen abgelaufener Sessions nötig

**Spring Session mit Redis:**
- Sub-Millisekunden-Latenz für Session-Operationen
- Automatisches Ablaufen von Sessions (TTL)
- Bewährt in Spring-Ökosystem (Spring Session Data Redis)
- Redis bereits in Docker Compose für lokale Entwicklung konfiguriert
- Nachteil: Zusätzlicher Infrastruktur-Service auf Kubernetes

**Stateless Sessions (JWT in Cookie):**
- Kein serverseitiger Zustand nötig
- Nachteil: Inkompatibel mit Spring Security OAuth2 Client (Session-basierter Authorization Code Flow)
- Nachteil: Token-Revocation schwierig
- Nachteil: Cookie-Größe wächst mit Claims

## Decision

Wir verwenden **Spring Session Data Redis** als externen Session-Store. Redis wird als Single-Instance Deployment auf Kubernetes betrieben.

### Konfiguration

| Parameter           | Wert                                      |
| ------------------- | ----------------------------------------- |
| Redis-Version       | 7.x (Alpine)                              |
| Instanzen           | 1 (Single Instance, kein Sentinel/Cluster)|
| Namespace           | redis                                     |
| Port                | 6379                                      |
| Persistenz          | Keine (RDB/AOF deaktiviert)               |
| Authentifizierung    | Passwort via Kubernetes Secret           |
| Memory Limit        | 128 Mi                                    |
| Session-Namespace   | spring:session:blog-content               |
| Session-Timeout     | 30 Minuten (Spring Default)               |

### Warum kein Redis Sentinel/Cluster?

Session-Daten sind kurzlebig und reproduzierbar:
- Bei Redis-Ausfall loggen sich Benutzer erneut ein (wenige Sekunden Unterbrechung)
- Kein Datenverlust-Risiko für geschäftskritische Daten
- Single Instance ist ausreichend für die erwartete Last
- Sentinel/Cluster kann bei Bedarf nachgerüstet werden

### Warum keine Persistenz?

- Sessions haben eine TTL und werden automatisch gelöscht
- Bei Redis-Neustart müssen sich Benutzer nur neu einloggen
- Keine RDB/AOF-Snapshots spart Disk-I/O und vereinfacht das Deployment

### Verantwortlichkeiten

| Aufgabe                              | Wo                                   |
| ------------------------------------ | ------------------------------------ |
| Redis Deployment (K8s)               | infra/k8s/redis/                     |
| Spring Session Konfiguration         | blog-content application-k8s.yml     |
| Network Policy (Egress zu Redis)     | infra/k8s/blog-content/              |
| Docker Compose (lokale Entwicklung)  | infra/docker/docker-compose.yml      |

## Consequences

**Vorteile:**
- Horizontale Skalierung des Blog Content Service möglich (Replicas > 1)
- Zero-Downtime-Deployments: Sessions überleben Rolling Updates
- Sub-Millisekunden Session-Zugriff
- Spring Boot Auto-Configuration: minimaler Code-Aufwand (Dependency + YAML)
- Lokale Entwicklung nutzt bestehenden Redis-Container aus Docker Compose

**Nachteile:**
- Zusätzlicher Infrastruktur-Service (Redis) auf Kubernetes
- Single Point of Failure: bei Redis-Ausfall Session-Verlust (akzeptables Risiko)
- Netzwerk-Latenz zwischen Blog-Pod und Redis-Pod (vernachlässigbar im Cluster)

**Sicherheit:**
- Redis wird mit Passwort-Authentifizierung betrieben (requirepass)
- Network Policy beschränkt Zugriff auf Redis auf den tomsblog Namespace
- Linkerd mTLS verschlüsselt die Verbindung zwischen Blog-Pod und Redis
- Security Context: non-root, read-only Filesystem, keine Privilege Escalation

## References

- [Spring Session Data Redis](https://docs.spring.io/spring-session/reference/guides/boot-redis.html)
- ADR-0021: Zero Trust mit Linkerd (mTLS für Redis-Verbindung)
- ADR-0019: Database per Service (Redis als zusätzlicher Infrastruktur-Service)
- SWR-053: SuperAdmin Tenant-Umschaltung (Session-basiert)
