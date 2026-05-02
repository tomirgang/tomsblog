# ADR-0025: Secrets Management Strategie

## Status

Accepted

## Kontext

Microservices im Kubernetes-Cluster benoetigen Zugriff auf Datenbank-Credentials und andere Secrets, die von Operatoren (z.B. CloudNativePG) in separaten Namespaces verwaltet werden. Es wird eine Loesung benoetigt, die:

- Cross-Namespace Secret-Synchronisation ermoeglicht
- Automatische Updates bei Credential-Rotation unterstuetzt
- Im spaeten Projektverlauf externe Secret-Stores (z.B. HashiCorp Vault) anbinden kann
- Einfach zu betreiben ist fuer die aktuelle Projektphase (Meilenstein 1)

## Entscheidung

Wir verfolgen eine zweistufige Secrets-Management-Strategie:

### Phase 1 (Meilenstein 1): Reflector

- **Reflector** (emberstack/kubernetes-reflector) fuer Cross-Namespace Secret-Synchronisation
- Secrets, die von Operatoren erstellt werden (z.B. CNPG `postgres-cluster-app`), werden per Annotation automatisch in Ziel-Namespaces gespiegelt
- Kein externer Secret-Store erforderlich
- Installation via Flux HelmRelease im IaC-Repository

### Phase 2 (Meilenstein 2): External Secrets Operator (ESO)

- **External Secrets Operator** fuer die Anbindung externer Secret-Stores
- Unterstuetzte Backends: HashiCorp Vault, AWS Secrets Manager, Azure Key Vault
- Ermoeglicht zentrale Secret-Verwaltung ausserhalb des Clusters
- Reflector bleibt fuer Operator-generierte Secrets (CNPG) weiterhin im Einsatz
- ESO wird ergaenzend fuer applikationsspezifische Secrets genutzt (API-Keys, OAuth-Secrets)

## Konsequenzen

### Positiv

- Meilenstein 1 hat minimale Infrastruktur-Komplexitaet
- Klarer Migrationspfad zu externem Secret-Management
- Reflector ist leichtgewichtig (~64MB RAM) und erprobt
- Automatische Synchronisation bei Secret-Rotation durch CNPG

### Negativ

- Reflector bietet keine Verschluesselung at-rest oder Audit-Logs
- Manuelle Annotation am Quell-Secret erforderlich (einmalig)
- Zwei Tools in Phase 2 (Reflector + ESO) erhoehen die Betriebskomplexitaet leicht

### Neutral

- Secrets werden als Kubernetes Secrets gespeichert (Standard-Verhalten)
- etcd-Encryption-at-rest wird separat ueber Cluster-Konfiguration sichergestellt

## Betroffene Requirements

- SWA-017: Kubernetes Cluster Provisionierung
- SWA-018: PostgreSQL mit CloudNativePG
