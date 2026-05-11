# Infrastruktur

Infrastructure-as-Code für die Toms-Blog-Plattform.

## Verzeichnisse

| Verzeichnis | Beschreibung |
|-------------|-------------|
| `k8s/` | Applikationsspezifische Kubernetes-Manifeste (DB-Instanzen, PVCs, Services) |
| `docker/` | Docker Compose für lokale Entwicklungsumgebung |

## Cluster-Provisionierung

Das Kubernetes-Cluster (Hetzner Cloud, 3 Nodes, k3s) wird im separaten Repository
provisioniert: `kubernetes-playground/k3s/` (hetzner-k3s CLI).

Dort werden verwaltet:
- Cluster-Nodes (Controller + Worker)
- Netzwerk (privates Hetzner-Netzwerk)
- Hetzner CSI/CCM (automatisch durch hetzner-k3s)

Siehe ADR-0034 und ADR-0023 für die Architekturentscheidungen.
