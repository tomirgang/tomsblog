# Kubernetes-Manifeste

Applikationsspezifische Kubernetes-Ressourcen für das Deployment auf dem Hetzner-Cluster.
Diese Manifeste werden zukünftig von Flux (GitOps) verwaltet.

## Voraussetzungen

Das Kubernetes-Cluster und die Basis-Operatoren (CloudNativePG, Hetzner CSI/CCM) werden
im separaten Repository (`kubernetes-playground/k3s/`) mit hetzner-k3s provisioniert.

## Verzeichnisstruktur

| Pfad             | Beschreibung                                                                 |
| ---------------- | ---------------------------------------------------------------------------- |
| `blog-content/`  | Kustomize-Manifeste für den Blog Content Service (Deployment, Service, Ingress, Secret) |
| `cert-manager/`  | ClusterIssuer-Definitionen für Let's Encrypt (Staging + Production)           |
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
| Cluster-Provisionierung (Nodes, Netzwerk, CSI/CCM)       | `kubernetes-playground/k3s/`     |
| Operator-Installation (CloudNativePG, Reflector, cert-manager) | `kubernetes-playground/k3s/` + Flux |
| Applikationsspezifische Ressourcen (DB-Instanzen, PVCs)  | `tomsblog/infra/k8s/`            |
| Kustomize-Manifeste für Blog-Services                    | `tomsblog/infra/k8s/`            |
| cert-manager ClusterIssuer + Ingress-Ressourcen          | `tomsblog/infra/k8s/`            |

## TLS und Ingress

Der Blog ist unter `https://blog.tomirgang.de` erreichbar. TLS-Zertifikate werden
automatisch über cert-manager und Let's Encrypt (ACME HTTP-01) bereitgestellt.

### Voraussetzungen (IaC-Repository)

- cert-manager Operator installiert (CRDs + Controller)
- DNS A-Record: `blog.tomirgang.de` → Hetzner LB IP
- Port 80 am Load Balancer offen (für ACME Challenge-Validierung)

### Ressourcen in diesem Repo

| Datei | Zweck |
| ----- | ----- |
| `cert-manager/clusterissuer-letsencrypt-prod.yaml` | Let's Encrypt Production Issuer |
| `cert-manager/clusterissuer-letsencrypt-staging.yaml` | Let's Encrypt Staging Issuer (zum Testen) |
| `blog-content/ingress.yaml` | Ingress-Regel für blog.tomirgang.de mit TLS |
| `blog-content/middleware-redirect-https.yaml` | HTTP→HTTPS Redirect (Traefik Middleware) |

### Erstmaliges Testen mit Staging

Vor der Nutzung des Production-Issuers empfiehlt sich ein Test mit Staging
(höhere Rate Limits, kein vertrauenswürdiges Zertifikat):

```bash
# In ingress.yaml die Annotation ändern:
# cert-manager.io/cluster-issuer: letsencrypt-staging
kubectl apply -k .
kubectl get certificate -n tomsblog
kubectl describe certificate blog-content-tls -n tomsblog
```

Nach erfolgreichem Test die Annotation auf `letsencrypt-prod` zurücksetzen.
