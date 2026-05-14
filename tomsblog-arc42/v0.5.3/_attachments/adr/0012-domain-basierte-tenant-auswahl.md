# ADR-0012: Domain-basierte Tenant-Auswahl

## Status

Accepted

## Context

Multi-Tenancy erfordert eine Strategie zur Tenant-Identifikation bei eingehenden Requests. Übliche Ansätze:

1. **Subdomain**: `tenant1.example.com` (häufigster Ansatz)
2. **Custom Domain**: Jeder Tenant nutzt seine eigene Domain
3. **URL-Pfad**: `/tenant1/posts/...` (unüblich, SEO-Nachteile)
4. **Header/Token**: Tenant wird im Auth-Token mitgeliefert

Für eine Blog-Plattform ist die Domain das natürliche Identifikationsmerkmal: Jeder Blog hat seine eigene Web-Adresse. Sowohl eigene Domains als auch Subdomains der Plattform sollen unterstützt werden.

## Decision

Die **Tenant-Auswahl erfolgt über die Domain** (Host-Header) des eingehenden Requests.

**Design:**
- Ein Tenant kann mehrere Domains besitzen (z.B. `blog.example.com` + `toms-blog.platform.io`)
- Subdomains der Plattform werden ebenfalls unterstützt (z.B. `tenant1.platform.io`)
- Ein Domain-zu-Tenant-Mapping wird in einer zentralen Konfiguration gepflegt
- Die Auflösung erfolgt am Ingress-Level (oder in einem vorgelagerten Filter)
- Die aufgelöste TenantId wird als `X-Tenant-Id` Header an Backend-Services propagiert
- Unbekannte Domains erhalten einen 404-Response

**Datenmodell:**

| Feld          | Beschreibung                     |
| ------------- | -------------------------------- |
| domain        | FQDN (eindeutig)                |
| tenant_id     | Zugeordneter Tenant              |
| is_primary    | Primärdomain für Canonical-URLs  |
| verified      | DNS-Verifikation abgeschlossen   |

## Consequences

**Positiv:**
- Natürliches Modell für Blog-Plattformen (jeder Blog hat eigene URL)
- Unterstützt sowohl Custom Domains als auch Plattform-Subdomains
- Saubere Trennung: Backend-Services arbeiten nur mit TenantId
- SEO-freundlich (eigene Domains statt Pfad-basierte Trennung)
- TLS-Zertifikate pro Domain via Let's Encrypt (Wildcard oder per-Domain)

**Negativ:**
- DNS-Konfiguration durch Tenant nötig (Custom Domains)
- TLS-Zertifikat-Management wird komplexer (viele Domains)
- Domain-Verification-Prozess muss implementiert werden
- Ingress-Konfiguration muss dynamisch sein (neue Domains zur Laufzeit)

## References

- SWR-017: Domain-basierte Tenant-Auswahl
- SWR-003: Tenant-Isolation auf Datenebene
- SWA-011: Domain-basierte Tenant-Resolution am Ingress
