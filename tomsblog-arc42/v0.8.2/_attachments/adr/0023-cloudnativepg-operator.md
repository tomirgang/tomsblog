# ADR-0023: CloudNativePG für PostgreSQL auf Kubernetes

## Status

Accepted

## Context

Die Blog-Plattform benötigt PostgreSQL als primäre Datenbank (SWA-015: Database per Service). Auf Kubernetes gibt es verschiedene Ansätze für den PostgreSQL-Betrieb.

Bewertungskriterien:
- **Kubernetes-native Verwaltung**: Deklarative Cluster-Definition als Custom Resource
- **Betriebssicherheit**: Automatisches Failover, Backup, WAL-Archivierung
- **Ressourcenverbrauch**: Relevant für den 3-Node-Cluster
- **Community und Reife**: Langfristige Unterstützung

### Evaluierte Alternativen

**CloudNativePG:**
- CNCF Sandbox-Projekt, von EDB (EnterpriseDB) entwickelt
- Operator verwaltet PostgreSQL-Cluster als Kubernetes Custom Resource
- Automatisches Failover, Backup zu S3, Point-in-Time Recovery
- Leichtgewichtig: kein zusätzlicher Proxy (wie PgBouncer) nötig für einfache Setups
- Native Hetzner-Volume-Integration über StorageClass

**Zalando Postgres Operator:**
- Bewährt, aber komplex (Patroni-basiert)
- Höherer Ressourcenverbrauch durch Sidecar-Container
- Weniger aktive Entwicklung in letzter Zeit

**CrunchyData PGO:**
- Enterprise-orientiert, umfangreiche Features
- Höhere Komplexität für ein Lern-/Entwicklungsprojekt
- Doppelte Lizenzierung (Open Source + Enterprise)

**Manuelles StatefulSet:**
- Volle Kontrolle, aber kein automatisches Failover
- Backup und Recovery manuell zu implementieren
- Hoher Wartungsaufwand

## Decision

Wir verwenden den **CloudNativePG Operator** für den PostgreSQL-Betrieb auf Kubernetes.

### Konfiguration (Phase 3 / MVP)

| Parameter | Wert |
|----------|------|
| Instanzen | 1 (Single Instance, kein HA) |
| Storage | 10 Gi (Hetzner CSI, hcloud-volumes) |
| shared_buffers | 256 MB |
| effective_cache_size | 512 MB |
| CPU Request/Limit | 250m / 1000m |
| Memory Request/Limit | 512 Mi / 1 Gi |
| Namespace | postgres |

### Verantwortlichkeiten

| Aufgabe | Wo |
|---------|-----|
| Operator-Installation (CRDs, Controller) | IaC-Repository (Plattform-Voraussetzung) |
| Cluster-Definition (Instanz, Sizing, Storage) | tomsblog/infra/k8s/postgres/ |
| Backup-Konfiguration (S3, WAL) | tomsblog/infra/k8s/postgres/ (später) |

### Begründung

- **Kubernetes-nativ**: PostgreSQL-Cluster als deklarative YAML-Ressource passt zum GitOps-Ansatz (ADR-0020)
- **Leichtgewichtig**: Minimaler Overhead auf dem 3-Node-Cluster
- **CNCF-Projekt**: Aktive Community, langfristige Perspektive
- **Einfaches Scaling**: Von Single Instance auf HA (2+ Replicas) ohne Architekturwechsel
- **Hetzner-CSI-Integration**: Storage über hcloud-volumes StorageClass

## Consequences

**Vorteile:**
- Deklarative Datenbank-Verwaltung im Git (Flux-kompatibel)
- Automatisches Secret-Management (Zugangsdaten als Kubernetes Secrets)
- Einfacher Pfad zu HA (Replica-Count erhöhen)
- Integrierte Backup-Möglichkeit zu S3 (für spätere Backup-Strategie)

**Nachteile:**
- Abhängigkeit vom CloudNativePG Operator (muss auf Cluster installiert sein)
- Single Instance in Phase 3: kein automatisches Failover
- Operator-Updates müssen separat koordiniert werden
- Hetzner Volumes sind AZ-gebunden (kein Cross-AZ-Failover)

## References

- SWA-015 (Database per Service)
- STK-007 (Cloud-Native Deployment)
- ADR-0022 (Kubernetes-Plattform mit kube-hetzner)
