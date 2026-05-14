# ADR-0020: Flux für GitOps

## Status

Accepted

## Context

Für das automatisierte Deployment auf dem Hetzner-Kubernetes-Cluster wird ein GitOps-Tool benötigt, das Kubernetes-Manifeste aus einem Git-Repository mit dem Cluster-Zustand synchronisiert. Die zwei etablierten CNCF-Projekte sind ArgoCD und Flux.

Bewertungskriterien:
- **Architekturmodell**: ArgoCD bringt einen zentralen Application Controller mit Web-UI und API-Server mit. Flux arbeitet als Sammlung eigenständiger Controller (Source, Kustomize, Helm, Notification, Image Automation), die unabhängig deployed werden.
- **Ressourcenverbrauch**: ArgoCD benötigt deutlich mehr RAM und CPU durch Web-UI, API-Server, Redis-Cache und Dex-Server. Flux läuft mit minimalen Ressourcen, was für einen 3-Node-Cluster auf Hetzner relevant ist.
- **Kustomize-/Helm-Integration**: Flux unterstützt Kustomize und Helm nativ als First-Class-Konzepte über dedizierte Controller. ArgoCD unterstützt dies ebenfalls, aber über einen monolithischeren Ansatz.
- **Image Automation**: Flux bietet Image Automation Controller, der automatisch neue Container-Image-Tags erkennt und Git-Commits mit aktualisierten Tags erzeugt. ArgoCD benötigt dafür Argocd Image Updater als separates Projekt.
- **Multi-Tenancy**: Flux ermöglicht feingranulare Multi-Tenancy über Namespaces mit eigenen Source- und Kustomization-Ressourcen. ArgoCD löst dies über AppProjects.
- **Komplexität**: Für ein Lernprojekt mit einem kleinen Cluster ist die schlanke, deklarative Natur von Flux besser geeignet als die umfangreichere ArgoCD-Installation.

## Decision

Wir verwenden **Flux** als GitOps-Tool für das Kubernetes-Deployment.

### Begründung

- **Leichtgewichtig**: Flux benötigt weniger Ressourcen als ArgoCD, was für den 3-Node-Hetzner-Cluster optimal ist.
- **Deklarativ und Git-nativ**: Alle Flux-Konfigurationen sind Kubernetes-Custom-Resources, die selbst in Git verwaltet werden (GitOps für GitOps).
- **Modulare Controller**: Nur die benötigten Controller werden installiert (Source, Kustomize, Helm). Weitere (Image Automation, Notification) können bei Bedarf ergänzt werden.
- **CNCF Graduated**: Flux ist ein graduiertes CNCF-Projekt mit breiter Community-Unterstützung.
- **Kustomize-First**: Der Kustomize-Controller vereinfacht das Overlay-Modell für unterschiedliche Umgebungen (Dev, Staging, Prod).

### Einsatzbereich

- Synchronisation von Helm Charts und Kustomize-Overlays aus dem Git-Repository
- Automatische Reconciliation bei Git-Commits
- Health-Checks und Notifications bei Sync-Fehlern
- Später: Image Automation für automatische Deployments nach Container-Image-Builds

## Consequences

**Vorteile:**
- Minimaler Ressourcenverbrauch auf dem Cluster
- Kein zusätzlicher UI-Server nötig (Cluster-Zustand über kubectl oder Weave GitOps Dashboard bei Bedarf)
- Alle Konfigurationen sind versioniert und auditierbar in Git
- Einfache Erweiterbarkeit durch zusätzliche Flux-Controller

**Nachteile:**
- Kein eingebautes Web-UI (kann durch Weave GitOps Dashboard ergänzt werden)
- Debugging erfordert kubectl und Flux CLI statt einer grafischen Oberfläche
- Lernkurve für Custom Resources (GitRepository, Kustomization, HelmRelease)
