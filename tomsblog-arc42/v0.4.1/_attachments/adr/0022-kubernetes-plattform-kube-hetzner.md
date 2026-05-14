# ADR-0022: Kubernetes-Plattform mit kube-hetzner und OpenTofu

## Status

Accepted

## Context

Für den Betrieb der Microservices wird ein Kubernetes-Cluster benötigt (STK-007). Die Plattform muss budgetgerecht, wartbar und reproduzierbar provisioniert werden.

Bewertungskriterien:
- **Kosten**: Budget-limitiertes Projekt auf Hetzner Cloud
- **Reproduzierbarkeit**: Infrastructure as Code für konsistente Cluster-Erstellung
- **Wartbarkeit**: Kontrollierte Updates ohne Datenverlust
- **Trennung**: Plattform-Provisionierung vs. Applikations-Deployment

### Evaluierte Alternativen

**Managed Kubernetes (Hetzner Cloud):**
- Hetzner bietet kein managed K8s-Produkt an

**kubeadm manuell:**
- Volle Kontrolle, aber hoher manueller Aufwand
- Keine deklarative Reproduzierbarkeit

**kube-hetzner (Terraform-Modul):**
- k3s-basiertes Cluster auf Hetzner Cloud VMs
- Terraform/OpenTofu als IaC-Tool
- Integriert Hetzner CSI, CCM, WireGuard-Verschlüsselung
- Packer für MicroOS-Snapshots
- Community-maintained, aktiv entwickelt

**Talos Linux:**
- Minimal-OS speziell für Kubernetes
- Weniger Community-Support auf Hetzner

## Decision

Wir verwenden **kube-hetzner** (Terraform-Modul) mit **OpenTofu** zur Provisionierung eines k3s-Clusters auf Hetzner Cloud.

### Cluster-Konfiguration

| Komponente | Konfiguration |
|-----------|---------------|
| Control Plane | 1x CX23, Nürnberg (nbg1) |
| Worker Nodes | 2x CX23, Nürnberg (nbg1) |
| Load Balancer | LB11, Nürnberg |
| CNI-Verschlüsselung | WireGuard |
| Storage | Hetzner CSI (hcloud-volumes) |
| Automatische Upgrades | Deaktiviert (manuelle Kontrolle) |
| Basis-Domain | cluster.tomirgang.de |
| Kosten | ca. 21,55 EUR/Monat |

### Repository-Trennung

| Verantwortung | Repository |
|---------------|-----------|
| Cluster-Provisionierung (Nodes, Netzwerk, Operatoren) | `kubernetes-playground/K8nCluster` |
| Applikationsspezifische Ressourcen (DB-Instanzen, Services) | `tomsblog/infra/k8s/` |

### Begründung

- **Budget-optimal**: ca. 21,55 EUR/Monat für ein vollständiges 3-Node-Cluster
- **Reproduzierbar**: OpenTofu State ermöglicht konsistente Infrastruktur
- **WireGuard**: Netzwerkverschlüsselung zwischen Nodes als Ergänzung zu Linkerd mTLS (ADR-0021)
- **Hetzner-nativ**: CSI und CCM direkt integriert, keine manuellen Volume-/LB-Konfigurationen
- **Saubere Trennung**: Plattform-Lifecycle (selten, IaC) getrennt von App-Lifecycle (häufig, GitOps/Flux)
- **OpenTofu statt Terraform**: Open-Source-Fork, vermeidet HashiCorp BSL-Lizenzprobleme

## Consequences

**Vorteile:**
- Cluster ist vollständig aus Code reproduzierbar
- Kontrollierte Updates über dedizierte Skripte (cluster_update.sh)
- Kosten-Modi (normal/reduziert/pausiert) für Entwicklungsphasen
- WireGuard-Verschlüsselung ohne zusätzliche Konfiguration

**Nachteile:**
- Kein HA (Single Control Plane): kurze Downtime bei Updates unvermeidbar
- k3s statt vollwertigem K8s: einige Randfälle bei Helm Charts möglich
- Abhängigkeit vom kube-hetzner Community-Modul
- Terraform-State muss separat gesichert werden

## References

- STK-007 (Cloud-Native Deployment)
- ADR-0020 (Flux für GitOps)
- ADR-0021 (Zero Trust mit Linkerd)
