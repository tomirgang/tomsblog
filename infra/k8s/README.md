# Kubernetes-Manifeste

Applikationsspezifische Kubernetes-Ressourcen für das Deployment auf dem Hetzner-Cluster.
Diese Manifeste werden zukünftig von Flux (GitOps) verwaltet.

## Voraussetzungen

Das Kubernetes-Cluster und die Basis-Operatoren (CloudNativePG, Hetzner CSI/CCM) werden
im separaten IaC-Repository (`kubernetes-playground/K8nCluster`) mit OpenTofu provisioniert.

## Verzeichnisstruktur

| Pfad             | Beschreibung                                                                 |
| ---------------- | ---------------------------------------------------------------------------- |
| `blog-content/`  | Kustomize-Manifeste für den Blog Content Service (Deployment, Service, Secret) |
| `postgres/`      | CloudNativePG Cluster-Definition und Namespace für die Blog-Datenbank        |
| `pvc.yaml`       | PersistentVolumeClaim für allgemeinen Blog-Storage (Hetzner Volumes)          |

## Secrets Management

Secrets werden mit **SOPS + age** verschlüsselt im Git-Repository gespeichert.
Flux entschlüsselt sie automatisch beim Deployment (Decryption Provider: `sops`).

### Architektur

```
┌─────────────────────────┐     ┌──────────────────────────┐
│  Developer (lokal)      │     │  Flux (Cluster)          │
│                         │     │                          │
│  sops --encrypt         │────>│  kustomize-controller    │
│  (age Public Key)       │     │  entschlüsselt mit       │
│                         │     │  sops-age Secret         │
└─────────────────────────┘     └──────────────────────────┘
```

### Konfiguration

| Datei           | Zweck                                                         |
| --------------- | ------------------------------------------------------------- |
| `.sops.yaml`    | SOPS Creation Rules (welche Dateien wie verschlüsselt werden) |
| `*/secret.yaml` | Verschlüsselte Secret-Manifeste (nur `stringData`/`data`)     |

### Verwendung

**Secret bearbeiten (entschlüsselte Ansicht im Editor):**

```bash
sops infra/k8s/blog-content/secret.yaml
```

**Neues Secret erstellen und verschlüsseln:**

```bash
# Klartext-Secret erstellen, dann verschlüsseln:
sops --encrypt --in-place infra/k8s/neuer-service/secret.yaml
```

**Secret entschlüsseln (nur zur Ansicht, nicht committen!):**

```bash
sops --decrypt infra/k8s/blog-content/secret.yaml
```

### Schlüsselverwaltung

- **age Public Key** (in `.sops.yaml`): Darf öffentlich im Repo liegen, wird nur zum Verschlüsseln genutzt
- **age Private Key** (`~/.config/sops/age/keys.txt`): Muss sicher aufbewahrt werden (Passwort-Manager). Wird lokal zum Bearbeiten und im Cluster zum Entschlüsseln benötigt
- **Cluster-Secret** (`sops-age` in `flux-system`): Enthält den Private Key für Flux. Muss bei Cluster-Neuaufbau manuell erstellt werden:

```bash
kubectl create secret generic sops-age \
  --namespace=flux-system \
  --from-file=age.agekey=$HOME/.config/sops/age/keys.txt
```

### Secret-Typen im Projekt

| Secret                   | Namespace  | Herkunft                       | Verschlüsselung |
| ------------------------ | ---------- | ------------------------------ | --------------- |
| `blog-content-secrets`   | tomsblog   | SOPS-verschlüsselt im Repo    | age + SOPS      |
| `postgres-cluster-app`   | tomsblog   | Reflector (gespiegelt von CNPG) | Operator-managed |

### Wichtige Hinweise

- Niemals entschlüsselte Secrets committen
- Bei Verlust des age Private Keys: Neuen Key generieren, alle Secrets neu verschlüsseln
- Der age Private Key ist das einzige "Bootstrap-Secret" bei Cluster-Neuaufbau

## Deployment

Flux synchronisiert dieses Verzeichnis automatisch (Kustomization `tomsblog-infra`, Intervall 10 Min).

Manuelle Anwendung:

```bash
kubectl config use-context tomsblog
kubectl apply -k .
```

## Abgrenzung zum IaC-Repository

| Verantwortung                                            | Repository                       |
| -------------------------------------------------------- | -------------------------------- |
| Cluster-Provisionierung (Nodes, Netzwerk, CSI/CCM)       | `kubernetes-playground/K8nCluster` |
| Operator-Installation (CloudNativePG, Reflector, etc.)   | `kubernetes-playground/flux/`    |
| Applikationsspezifische Ressourcen (DB-Instanzen, PVCs)  | `tomsblog/infra/k8s/`            |
| Kustomize-Manifeste für Blog-Services                    | `tomsblog/infra/k8s/`            |
