# Kubernetes-Manifeste

Applikationsspezifische Kubernetes-Ressourcen für das Deployment auf dem Hetzner-Cluster.
Diese Manifeste werden zukünftig von Flux (GitOps) verwaltet.

## Voraussetzungen

Das Kubernetes-Cluster und die Basis-Operatoren (CloudNativePG, Hetzner CSI/CCM) werden
im separaten IaC-Repository (`kubernetes-playground/K8nCluster`) mit OpenTofu provisioniert.

## Verzeichnisstruktur

| Pfad | Beschreibung |
|------|-------------|
| `postgres/` | CloudNativePG Cluster-Definition und Namespace für die Blog-Datenbank |
| `pvc.yaml` | PersistentVolumeClaim für allgemeinen Blog-Storage (Hetzner Volumes) |

## Deployment (manuell, bis Flux eingerichtet)

```bash
kubectl config use-context tomsblog

# PostgreSQL-Namespace und Cluster
kubectl apply -f postgres/namespace.yaml
kubectl apply -f postgres/cluster.yaml

# Allgemeiner Storage
kubectl apply -f pvc.yaml
```

## Abgrenzung zum IaC-Repository

| Verantwortung | Repository |
|---------------|-----------|
| Cluster-Provisionierung (Nodes, Netzwerk, CSI/CCM) | `kubernetes-playground/K8nCluster` |
| Operator-Installation (CloudNativePG, Strimzi, etc.) | `kubernetes-playground/K8nCluster` |
| Applikationsspezifische Ressourcen (DB-Instanzen, PVCs, Services) | `tomsblog/infra/k8s/` |
| Helm Charts / Kustomize für Blog-Services | `tomsblog/infra/k8s/` (geplant) |
