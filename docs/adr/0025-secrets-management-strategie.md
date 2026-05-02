# ADR-0025: Secrets Management Strategie

## Status

Accepted

## Kontext

Microservices im Kubernetes-Cluster benötigen Zugriff auf Datenbank-Credentials und andere Secrets, die von Operatoren (z.B. CloudNativePG) in separaten Namespaces verwaltet werden. Gleichzeitig müssen applikationsspezifische Secrets (Admin-Passwörter, API-Keys) sicher im Git-Repository gespeichert werden können, ohne sie im Klartext zu committen. Es wird eine Lösung benötigt, die:

- Cross-Namespace Secret-Synchronisation ermöglicht
- Automatische Updates bei Credential-Rotation unterstützt
- Secrets verschlüsselt im Git-Repository speichern kann (GitOps-kompatibel)
- Im späten Projektverlauf externe Secret-Stores (z.B. HashiCorp Vault) anbinden kann
- Einfach zu betreiben ist für die aktuelle Projektphase (Meilenstein 1)

## Entscheidung

Wir verfolgen eine dreistufige Secrets-Management-Strategie:

### Secrets at Rest im Git-Repository: SOPS + age

- **SOPS** (Mozilla) verschlüsselt Secret-Manifeste im Git-Repository
- **age** als Verschlüsselungsbackend (einfacher als PGP, kein Key-Server nötig)
- Nur `data`/`stringData`-Felder werden verschlüsselt, Metadaten bleiben lesbar
- `.sops.yaml` definiert Creation Rules (Pfad-Pattern → age Public Key)
- Flux kustomize-controller entschlüsselt automatisch beim Apply (Decryption Provider)
- age Private Key liegt als `sops-age` Secret im `flux-system` Namespace

### Cross-Namespace Synchronisation: Reflector (Meilenstein 1)

- **Reflector** (emberstack/kubernetes-reflector) für Cross-Namespace Secret-Synchronisation
- Secrets, die von Operatoren erstellt werden (z.B. CNPG `postgres-cluster-app`), werden per Annotation automatisch in Ziel-Namespaces gespiegelt
- Kein externer Secret-Store erforderlich
- Installation via Flux HelmRelease im IaC-Repository

### Externe Secret-Stores: External Secrets Operator (Meilenstein 2)

- **External Secrets Operator** für die Anbindung externer Secret-Stores
- Unterstützte Backends: HashiCorp Vault, AWS Secrets Manager, Azure Key Vault
- Ermöglicht zentrale Secret-Verwaltung außerhalb des Clusters
- Reflector bleibt für Operator-generierte Secrets (CNPG) weiterhin im Einsatz
- ESO wird ergänzend für applikationsspezifische Secrets genutzt (API-Keys, OAuth-Secrets)

## Konsequenzen

### Positiv

- Secrets sind sicher im öffentlichen Git-Repository speicherbar
- Vollständig GitOps-konform: verschlüsselte Secrets werden wie normaler Code commited
- Meilenstein 1 hat minimale Infrastruktur-Komplexität
- Klarer Migrationspfad zu externem Secret-Management
- Reflector ist leichtgewichtig (~64MB RAM) und erprobt
- Automatische Synchronisation bei Secret-Rotation durch CNPG
- age ist einfacher als PGP (ein Schlüsselpaar, kein Web of Trust)

### Negativ

- age Private Key muss sicher außerhalb des Repos aufbewahrt werden (Passwort-Manager)
- Bei Cluster-Neuaufbau muss `sops-age` Secret manuell erstellt werden (Bootstrap-Problem)
- Reflector bietet keine Verschlüsselung at-rest oder Audit-Logs
- Manuelle Annotation am Quell-Secret erforderlich (einmalig)
- Zwei Tools in Meilenstein 2 (Reflector + ESO) erhöhen die Betriebskomplexität leicht

### Neutral

- Secrets werden als Kubernetes Secrets gespeichert (Standard-Verhalten)
- etcd-Encryption-at-rest wird separat über Cluster-Konfiguration sichergestellt
- SOPS unterstützt Key-Rotation (neuer age Key, alte Secrets re-encrypen)

## Betroffene Requirements

- SWA-019: Cross-Namespace Secret-Synchronisation mit Reflector
- SWA-020: SOPS+age Verschlüsselung für Secrets im Git-Repository
- STK-016: Sichere Verwaltung von Secrets im Cluster
