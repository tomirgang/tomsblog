# ADR-0029: Deklarative Datenbankverwaltung mit CloudNativePG Database CRD

## Status

Accepted

## Context

Die Blog-Plattform nutzt das Database-per-Service-Pattern (ADR-0019). Aktuell werden die Datenbanken asymmetrisch angelegt:

- `blog_content` über die `initdb`-Konfiguration der Cluster-Ressource
- `user_management` über `postInitApplicationSQL` (einmalig beim Bootstrap)

Diese Lösung hat mehrere Schwächen:

1. **Asymmetrie**: Die primäre Datenbank hat eine Sonderrolle gegenüber weiteren Datenbanken
2. **Keine Reconciliation**: `postInitApplicationSQL` läuft nur einmalig beim Bootstrap. Drift wird nicht erkannt oder korrigiert
3. **Fehler unsichtbar**: Probleme beim Anlegen zusätzlicher Datenbanken erscheinen nur in Pod-Logs statt in einer dedizierten Kubernetes-Ressource
4. **Kein Lifecycle-Management**: Es gibt keine deklarative Möglichkeit, Datenbanken zu entfernen oder deren Konfiguration zu ändern
5. **Owner-Inkonsistenz**: `initdb` nutzt `owner: blog_content`, `postInitApplicationSQL` nutzt `OWNER app` (der CNPG-Default-User), was zu Verwirrung führt

Seit CloudNativePG v1.25 existiert eine dedizierte `Database`-CRD, die den Lebenszyklus von PostgreSQL-Datenbanken als eigenständige Kubernetes-Ressourcen verwaltet.

### Evaluierte Alternativen

**Aktuelle Lösung (initdb + postInitApplicationSQL):**
- Einfach, aber asymmetrisch
- Kein Drift-Detection oder Status-Reporting
- Bootstrap-Only: nachträgliche Änderungen nicht möglich

**Database CRD (ab CNPG ≥ 1.25):**
- Jede Datenbank ist ein eigenes Kubernetes-Objekt
- Reconciliation durch den Operator
- Status und Fehler im `.status`-Feld sichtbar
- `databaseReclaimPolicy` steuert Löschverhalten (retain/delete)
- GitOps-fähig: jedes Team/Service pflegt sein eigenes Manifest
- Erweiterbar um Extensions und Schemas

**Manuelle SQL-Skripte:**
- Volle Kontrolle, aber kein Kubernetes-nativer Workflow
- Kein Reconciliation, kein Status-Reporting
- Nicht GitOps-kompatibel

## Decision

Wir migrieren auf die **CloudNativePG Database CRD** für die deklarative Verwaltung aller Service-Datenbanken. Jede Datenbank wird als eigenständige `Database`-Ressource definiert.

### Voraussetzungen

- CloudNativePG Operator ≥ 1.25 (Upgrade des Operators im IaC-Repository)

### Ziel-Konfiguration

```yaml
apiVersion: postgresql.cnpg.io/v1
kind: Cluster
metadata:
  name: postgres-cluster
  namespace: postgres
spec:
  instances: 2
  bootstrap:
    initdb:
      database: app
      owner: app
---
apiVersion: postgresql.cnpg.io/v1
kind: Database
metadata:
  name: blog-content-db
  namespace: postgres
spec:
  name: blog_content
  owner: app
  cluster:
    name: postgres-cluster
  databaseReclaimPolicy: retain
---
apiVersion: postgresql.cnpg.io/v1
kind: Database
metadata:
  name: user-management-db
  namespace: postgres
spec:
  name: user_management
  owner: app
  cluster:
    name: postgres-cluster
  databaseReclaimPolicy: retain
```

### Rollen-Konzept

Vorerst wird die vom Operator verwaltete `app`-Rolle als Owner für alle Datenbanken verwendet. Eine Trennung in dedizierte Rollen pro Service (`blog_content`, `user_management`) wird evaluiert, sobald die geplante `DatabaseRole`-CRD (erwartet in CNPG 1.30) stabil verfügbar ist.

### Migrationsplan

1. CloudNativePG Operator auf ≥ 1.25 upgraden (IaC-Repository)
2. `Database`-Ressourcen für bestehende Datenbanken erstellen (adopt existing)
3. `initdb.postInitApplicationSQL` aus der Cluster-Definition entfernen
4. `initdb.database` auf neutralen Platzhalter `app` umstellen
5. Validierung: Datenbanken und Berechtigungen unverändert

## Consequences

**Vorteile:**
- Symmetrische, deklarative Verwaltung aller Datenbanken
- GitOps-fähig: jede Datenbank als eigenständiges Manifest in `infra/k8s/postgres/`
- Operator überwacht Zustand und reconciliert bei Drift
- Fehler im `.status`-Feld der Ressource sichtbar (`kubectl get database`)
- `databaseReclaimPolicy: retain` schützt vor versehentlichem Datenverlust
- Konsistente Owner-Rolle für alle Datenbanken
- Einfaches Hinzufügen weiterer Service-Datenbanken (z. B. comment-service, feed-service)

**Nachteile:**
- Erfordert CNPG-Operator-Upgrade auf ≥ 1.25
- Migration bestehender Datenbanken muss sorgfältig getestet werden (adopt)
- Dedizierte Rollen pro Service erst mit zukünftiger `DatabaseRole`-CRD möglich

## References

- ADR-0019 (Database per Service)
- ADR-0023 (CloudNativePG Operator)
- INF-005 (PostgreSQL HA via CloudNativePG)
- SWA-018 (PostgreSQL-Betrieb mit CloudNativePG Operator)
- INF-013 (Deklarative Datenbankverwaltung mit Database CRD)
- SWA-030 (Deklarative Datenbank-Ressourcen pro Service)
