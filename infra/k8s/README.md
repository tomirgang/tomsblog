# Kubernetes-Manifeste

Applikationsspezifische Kubernetes-Ressourcen für das Deployment auf dem Hetzner-Cluster.
Diese Manifeste werden zukünftig von Flux (GitOps) verwaltet.

## Voraussetzungen

Das Kubernetes-Cluster und die Basis-Operatoren (CloudNativePG, Hetzner CSI/CCM) werden
im separaten IaC-Repository (`kubernetes-playground/K8nCluster`) mit OpenTofu provisioniert.

## Verzeichnisstruktur

| Pfad | Beschreibung |
|------|-------------|
| `blog-content/` | Kustomize-Manifeste für den Blog Content Service (Deployment, Service, Secret) |
| `postgres/` | CloudNativePG Cluster-Definition und Namespace für die Blog-Datenbank |
| `pvc.yaml` | PersistentVolumeClaim für allgemeinen Blog-Storage (Hetzner Volumes) |

## Deployment

Flux synchronisiert dieses Verzeichnis automatisch (Kustomization `tomsblog-infra`, Intervall 10 Min).

Manuelle Anwendung:

```bash
kubectl config use-context tomsblog
kubectl apply -k .
```

## Abgrenzung zum IaC-Repository

| Verantwortung | Repository |
|---------------|-----------|
| Cluster-Provisionierung (Nodes, Netzwerk, CSI/CCM) | `kubernetes-playground/K8nCluster` |
| Operator-Installation (CloudNativePG, Strimzi, etc.) | `kubernetes-playground/K8nCluster` |
| Applikationsspezifische Ressourcen (DB-Instanzen, PVCs, Services) | `tomsblog/infra/k8s/` |
| Kustomize-Manifeste für Blog-Services | `tomsblog/infra/k8s/` |
