# Infrastruktur

Infrastructure-as-Code für die Toms-Blog-Plattform.

## Verzeichnisse

| Verzeichnis | Beschreibung |
|-------------|-------------|
| `k8s/` | Applikationsspezifische Kubernetes-Manifeste (DB-Instanzen, PVCs, Services) |
| `docker/` | Docker Compose für lokale Entwicklungsumgebung |

## Cluster-Provisionierung

Das Kubernetes-Cluster (Hetzner Cloud, 3 Nodes, k3s) wird im separaten IaC-Repository
provisioniert: `kubernetes-playground/K8nCluster` (OpenTofu + kube-hetzner).

Dort werden verwaltet:
- Cluster-Nodes (Control Plane + Worker)
- Netzwerk und Load Balancer
- Hetzner CSI/CCM
- Basis-Operatoren (CloudNativePG, künftig Strimzi etc.)

Siehe ADR-0022 und ADR-0023 für die Architekturentscheidungen.
