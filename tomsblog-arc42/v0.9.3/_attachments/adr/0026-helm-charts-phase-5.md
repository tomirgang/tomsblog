# ADR-0026: Migration auf Helm Charts in Phase 5

## Status

Proposed

## Kontext

In den fruehen Phasen (Meilenstein 1-4) nutzen wir Kustomize fuer Kubernetes-Manifeste (siehe ADR-0024). Mit dem Hinzukommen von Operator-managed Services in Phase 5 (Kafka, RabbitMQ, MongoDB) steigt die Komplexitaet erheblich:

- Operator-Installationen (Strimzi fuer Kafka, RabbitMQ Cluster Operator, MongoDB Community Operator) erfordern umfangreiche CRDs und Konfiguration
- Helm Charts der Operatoren bieten getestete Default-Werte und Upgrade-Pfade
- Kustomize-Patches fuer Operator-CRDs sind fehleranfaellig und schlecht dokumentiert
- Versionsverwaltung von Operatoren ist mit Helm einfacher (Chart-Versionen vs. Raw-Manifeste)

## Entscheidung

Ab Phase 5 migrieren wir die Infrastruktur-Deployment-Strategie auf Helm Charts:

### Was migriert wird

- **Kafka** (Strimzi Operator): Helm Chart fuer Operator-Installation + CRDs fuer Topics/Cluster
- **RabbitMQ** (Cluster Operator): Helm Chart fuer Operator + RabbitmqCluster CRDs
- **MongoDB** (Community Operator): Helm Chart fuer Operator + MongoDBCommunity CRDs
- **Observability Stack**: Prometheus/Grafana/Loki/Tempo via kube-prometheus-stack Helm Chart

### Was bei Kustomize bleibt

- Applikations-Deployments (blog-content, auth-service, etc.) bleiben Kustomize-basiert
- Namespace-Definitionen und NetworkPolicies
- Einfache Ressourcen ohne Operator-Abhaengigkeit

### Migrations-Strategie

1. Neue Operator-Services werden direkt als Flux HelmRelease definiert
2. Bestehende Kustomize-Manifeste (CNPG, Reflector) werden nicht migriert (bereits stabil)
3. Hybrid-Ansatz: Flux verwaltet sowohl HelmReleases als auch Kustomizations

## Konsequenzen

### Positiv

- Bewaehlte Operator-Defaults ohne manuelle Konfiguration
- Einfache Upgrades via Chart-Version-Bumps
- Community-Support fuer Helm-basierte Operator-Installationen
- Flux unterstuetzt HelmRelease nativ (kein Tool-Wechsel)

### Negativ

- Zwei Deployment-Paradigmen im selben Cluster (Helm + Kustomize)
- Helm-Values muessen verstanden und gepflegt werden
- Chart-Breaking-Changes bei Major-Upgrades moeglich

### Neutral

- Kein Big-Bang-Migration: schrittweiser Uebergang bei neuen Services
- Reflector ist bereits als HelmRelease deployed (Praezedenzfall)
