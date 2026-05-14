# ADR-0034: Cluster-Provisionierung mit hetzner-k3s

## Status

Accepted

Supersedes [ADR-0022](0022-kubernetes-plattform-kube-hetzner.md)

## Context

Das Kubernetes-Cluster wurde bisher mit dem Terraform-Modul kube-hetzner und OpenTofu provisioniert (ADR-0022). Dieses Setup erfordert mehrere Werkzeuge (OpenTofu, Packer für MicroOS-Snapshots, Terraform-State-Management) und bringt eine hohe Komplexität für ein Lernprojekt mit sich.

Das CLI-Tool [hetzner-k3s](https://github.com/vitobotta/hetzner-k3s) bietet eine deutlich einfachere Alternative: Eine einzige YAML-Datei und ein CLI-Befehl reichen aus, um ein vollständiges k3s-Cluster auf Hetzner Cloud zu erstellen.

### Evaluierte Alternativen

**kube-hetzner (bisherig, ADR-0022):**
- OpenTofu + Terraform-Modul + Packer
- Hohe Komplexität (State-Management, MicroOS-Snapshots)
- WireGuard-Verschlüsselung integriert
- Community-maintained, aber komplexe Fehlersuche

**hetzner-k3s (Vito Botta):**
- Einzelnes CLI-Tool + YAML-Konfiguration
- Kein Terraform, kein Packer, kein Ansible
- Idempotente create/upgrade/delete Befehle
- Hetzner CSI und CCM automatisch integriert
- Aktiv gepflegt, einfache Bedienung

**Managed Kubernetes:**
- Hetzner bietet weiterhin kein managed K8s-Produkt an

## Decision

Wir wechseln die Cluster-Provisionierung von **kube-hetzner/OpenTofu** auf **hetzner-k3s** (CLI-Tool von Vito Botta).

### Cluster-Konfiguration

| Komponente          | Konfiguration                                  |
| ------------------- | ---------------------------------------------- |
| Alle Nodes          | 3x CPX22 (2 vCPU, 4 GB), Nürnberg (nbg1)      |
| k3s-Version         | v1.33.1+k3s1                                   |
| CNI                 | Flannel (k3s-Standard, ohne Verschlüsselung)  |
| Storage             | Hetzner CSI (hcloud-volumes)                   |
| Privates Netzwerk   | 10.0.0.0/16                                    |
| Ingress             | Traefik (k3s-Standard)                         |
| K8s API LB          | Deaktiviert (direkter Zugang via Controller IP)|
| Basis-Domain        | cluster.tomirgang.de                           |
| Kosten              | ca. 26 EUR/Monat (3x CPX22 + IPs)              |

### Repository-Trennung

| Verantwortung                                          | Repository                    |
| ------------------------------------------------------ | ----------------------------- |
| Cluster-Provisionierung (Nodes, Netzwerk, k3s)         | `kubernetes-playground/k3s/`  |
| Applikationsspezifische Ressourcen (DB, Services, etc.) | `tomsblog/infra/k8s/`         |

### Begründung

- **Einfacher**: Ein CLI-Tool + eine YAML-Datei statt OpenTofu + Packer + State-Management
- **Budget-optimal**: ca. 26 EUR/Monat, etwas günstiger als bisher
- **Idempotent**: `hetzner-k3s create` kann beliebig oft ausgeführt werden (Skalierung, Updates)
- **Hetzner-nativ**: CSI und CCM werden automatisch installiert
- **Saubere Trennung**: Plattform-Lifecycle (selten, CLI) getrennt von App-Lifecycle (häufig, GitOps/Flux)
- **Einheitliches Node-Sizing**: Alle Nodes identisch (CPX22), vereinfacht Verwaltung und Austausch

## Consequences

**Vorteile:**
- Drastisch reduzierte Komplexität bei der Cluster-Verwaltung
- Kein Terraform-State mehr zu sichern
- Schnelle Cluster-Erstellung (2-3 Minuten)
- Einfache Upgrades via `hetzner-k3s upgrade`
- Einfache Skalierung via Änderung der `instance_count` in YAML

**Nachteile:**
- Kein HA (Single Controller): kurze Downtime bei Updates unvermeidbar
- WireGuard Node-zu-Node-Verschlüsselung entfällt (Flannel ohne Encryption)
  - Pod-zu-Pod-Kommunikation bleibt durch Linkerd mTLS (ADR-0021) geschützt
  - Node-zu-Node-Traffic innerhalb des privaten Hetzner-Netzwerks ist nicht verschlüsselt, aber isoliert
- Abhängigkeit von einem Single-Maintainer-Projekt (Vito Botta)
- Kein automatischer Load Balancer für die Kubernetes API (direkter Zugriff via Controller-IP)

### Änderungen gegenüber ADR-0022

| Aspekt               | Vorher (kube-hetzner)                 | Nachher (hetzner-k3s)                  |
| -------------------- | ------------------------------------- | -------------------------------------- |
| Provisioning-Tool    | OpenTofu + kube-hetzner Modul         | hetzner-k3s CLI                        |
| Nodes                | 1x CX33 + 2x CX23 (heterogen)        | 3x CPX22 (2 vCPU, 4 GB, einheitlich) |
| CNI-Verschlüsselung  | WireGuard                             | Keine (privates Netzwerk + Linkerd)    |
| Basis-OS             | MicroOS (Packer Snapshots)            | Tool-verwaltet                         |
| K8s API Load Balancer | LB11                                 | Deaktiviert                            |
| Upgrades             | system-upgrade-controller             | hetzner-k3s upgrade                    |
| State-Management     | OpenTofu State                        | Keines (deklarative YAML)              |
| IaC-Repo-Pfad        | kubernetes-playground/K8nCluster      | kubernetes-playground/k3s/             |

## References

- STK-007 (Cloud-Native Deployment)
- ADR-0020 (Flux für GitOps)
- ADR-0021 (Zero Trust mit Linkerd)
- ADR-0022 (Vorgänger, superseded)
- https://github.com/vitobotta/hetzner-k3s
