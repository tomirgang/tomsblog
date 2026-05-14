# ADR-0017: API Gateway

## Status

Accepted

## Context

Mit wachsender Anzahl an Services (Domain-Services, BFFs, Auth-Service) wird ein zentraler Einstiegspunkt für externe Clients notwendig. Ohne Gateway müssen Clients die Adressen mehrerer Services kennen, und Querschnittsthemen wie Rate-Limiting, TLS-Terminierung und Authentifizierung müssen in jedem Service einzeln gelöst werden.

Optionen:
- **Nginx/Envoy als Reverse Proxy**: Leichtgewichtig, aber wenig Logik-Möglichkeiten
- **Spring Cloud Gateway**: Java-basiert, gut integriert ins Spring-Ökosystem, reaktiv
- **Kong/APISIX**: Feature-reich, aber zusätzliche Infrastruktur-Komplexität
- **Kubernetes Ingress + Service Mesh (Istio)**: Infrastruktur-nah, aber hohe Komplexität

## Decision

Wir verwenden **Spring Cloud Gateway** als API Gateway.

### Verantwortlichkeiten

- **Routing**: Eingehende Requests an den korrekten BFF oder Service weiterleiten
- **TLS-Terminierung**: HTTPS am Gateway terminieren
- **Rate-Limiting**: Schutz vor Überlastung (Token-Bucket pro Client/Tenant)
- **Authentication-Propagation**: JWT-Token validieren und an BFFs/Services weiterreichen
- **CORS**: Zentrale CORS-Konfiguration für alle Clients
- **Request/Response-Logging**: Zentrale Zugriffsprotokolle
- **Circuit Breaking**: Schutz bei Service-Ausfällen

### Routing-Schema

| Route              | Ziel                  |
| ------------------ | --------------------- |
| /api/**            | Web BFF               |
| /mobile/api/**     | Mobile BFF            |
| /media/api/**      | Media BFF (GraphQL)   |
| /auth/**           | Auth Service          |

### Konfiguration

- Routen werden deklarativ in YAML konfiguriert
- Tenant-Auflösung erfolgt weiterhin per Host-Header (ADR-0012)
- Gateway ist zustandslos und horizontal skalierbar

## Consequences

**Vorteile:**
- Single Point of Entry für alle Clients
- Querschnittsthemen zentral gelöst
- Spring-Ökosystem-Integration (gleiche Observability, Config, etc.)
- Vereinfachte Client-Konfiguration (nur eine URL)

**Nachteile:**
- Single Point of Failure (mitigiert durch Replikation + Health-Checks)
- Zusätzlicher Netzwerk-Hop für jeden Request
- Gateway muss performant genug sein (reaktiver Stack hilft)

## References

- ADR-0016: Backend for Frontend
- ADR-0012: Domain-basierte Tenant-Auswahl
- Spring Cloud Gateway Dokumentation
