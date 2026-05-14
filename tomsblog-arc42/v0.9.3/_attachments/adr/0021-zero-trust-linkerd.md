# ADR-0021: Zero Trust mit Linkerd Service Mesh

## Status

Accepted

## Context

Die Microservice-Architektur erfordert sichere Kommunikation zwischen den Services. Im Kubernetes-Cluster sind Pod-zu-Pod-Verbindungen standardmäßig unverschlüsselt und nicht authentifiziert. Jeder Pod im Cluster kann jeden anderen Pod erreichen (flaches Netzwerk). Das widerspricht dem Zero-Trust-Prinzip: "never trust, always verify".

Bewertungskriterien:
- **Automatisches mTLS**: Verschlüsselung und gegenseitige Authentifizierung aller Service-zu-Service-Verbindungen ohne Code-Änderungen.
- **Service-Identität**: Kryptografische Zuordnung von Identitäten zu Services basierend auf Kubernetes ServiceAccounts.
- **Autorisierungspolicies**: Feingranulare Kontrolle, welcher Service mit welchem kommunizieren darf (Default-Deny).
- **Ressourcenverbrauch**: Relevant für den 3-Node-Hetzner-Cluster mit begrenzten Ressourcen.
- **Operationale Komplexität**: Lernkurve und Wartungsaufwand.
- **User-Kontext-Propagierung**: KI-Features und schreibende Operationen dürfen nur im Kontext eines autorisierten Benutzers ausgeführt werden.

### Evaluierte Alternativen

**Linkerd:**
- Leichtgewichtiger Rust-basierter Sidecar-Proxy (linkerd2-proxy)
- Ca. 50 MB RAM pro Proxy, minimaler Latenz-Overhead (<1 ms p99)
- Automatisches mTLS ohne Konfiguration nach Mesh-Injection
- AuthorizationPolicy auf Basis von ServiceAccount-Identitäten
- CNCF Graduated Projekt

**Istio:**
- Envoy-basierter Sidecar-Proxy
- Ca. 100+ MB RAM pro Proxy, höherer Ressourcenverbrauch durch istiod Control Plane
- Umfangreichere Features (Traffic Management, ext. AuthZ, Wasm-Plugins)
- Höhere Komplexität und Lernkurve

**Cilium Service Mesh:**
- eBPF-basiert, kein Sidecar nötig
- Niedrigster Overhead, aber weniger ausgereift bei L7-Policies
- Abhängig davon, ob Cilium als CNI im Cluster läuft

**Application-Level TLS (Spring Boot + cert-manager):**
- Maximale Kontrolle, aber hoher Implementierungsaufwand pro Service
- Manuelle Policy-Definition und Certificate-Rotation
- Kein automatisches Mesh-Verhalten

## Decision

Wir verwenden **Linkerd** als Service Mesh für Zero Trust im Kubernetes-Cluster.

### Begründung

- **Leichtgewichtig**: Minimaler Ressourcenverbrauch passt zum 3-Node-Cluster auf Hetzner.
- **Automatisches mTLS**: Nach Mesh-Injection sind alle Verbindungen verschlüsselt und authentifiziert ohne Code-Änderungen.
- **Default-Deny-Policies**: AuthorizationPolicy erlaubt nur explizit konfigurierte Service-zu-Service-Kommunikation.
- **Einfache Installation**: Helm-basiert, passt in den Flux-GitOps-Workflow (ADR-0020).
- **gRPC-Support**: Nativer HTTP/2-Support für die geplante gRPC-Kommunikation zwischen Services (ADR-0010).
- **Geringer Latenz-Overhead**: Rust-basierter Proxy mit <1 ms p99 zusätzlicher Latenz.
- **CNCF Graduated**: Breite Community, langfristige Unterstützung.

### Zero-Trust-Modell (zwei Ebenen)

**Ebene 1: Service-Identität (Netzwerkebene)**
- Linkerd vergibt jedem Pod eine kryptografische Identität basierend auf dem Kubernetes ServiceAccount.
- AuthorizationPolicy definiert erlaubte Kommunikationspfade (z.B. nur blog-content darf ai-service aufrufen).
- Default-Deny: Ohne explizite Policy ist keine Kommunikation möglich.

**Ebene 2: User-Kontext-Propagierung (Anwendungsebene)**
- Das API-Gateway validiert JWT-Token und extrahiert User-Claims.
- Aufrufende Services propagieren User-Kontext als Header (z.B. X-User-Id, X-User-Roles).
- Aufgerufene Services prüfen: Caller-Identität OK (Linkerd) UND User-Claims mit passender Berechtigung vorhanden.
- KI-Features erfordern die Rolle AUTHOR oder ADMIN im propagierten User-Kontext.

### Einsatzbereich

- Alle Service-zu-Service-Verbindungen im Cluster (REST, gRPC)
- Ingress-Traffic über den Gateway (terminiert TLS am Ingress, Linkerd sichert intern)
- AuthorizationPolicies pro Service (z.B. AI-Service nur von blog-content erreichbar)
- Certificate-Rotation automatisch durch Linkerd Identity Controller

## Consequences

**Vorteile:**
- Vollständige Verschlüsselung aller internen Verbindungen ohne Anwendungscode-Änderungen
- Kryptografisch verifizierte Service-Identitäten
- Feingranulare Zugriffskontrolle auf Netzwerkebene
- Automatische Certificate-Rotation (kein manuelles Key-Management)
- Observability-Metriken (Golden Signals) als Nebeneffekt des Proxys
- Vorbereitung für Phase 9 (Observability): Linkerd liefert Latenz-, Traffic- und Error-Metriken

**Nachteile:**
- Zusätzlicher Ressourcenverbrauch durch Sidecar-Proxies (~50 MB RAM pro Pod)
- Initiale Lernkurve für Linkerd-Konzepte (Server, ServerAuthorization, AuthorizationPolicy)
- Debugging erfordert Verständnis der Proxy-Logs und Linkerd CLI (linkerd viz)
- Certificate Trust Anchor muss bei Installation korrekt konfiguriert und rotiert werden
