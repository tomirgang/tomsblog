# ADR-0024: Kustomize für Kubernetes-Manifeste

## Status

Accepted

## Context

Für das Deployment der Services auf Kubernetes wird ein Werkzeug benötigt, das Kubernetes-Manifeste verwaltet, parametrisiert und über Umgebungen hinweg anpassbar macht. Die zwei etablierten Ansätze sind Helm (Chart-basiertes Templating) und Kustomize (Overlay-basiertes Patching).

### Bewertungskriterien

- **Komplexität**: Aufwand für Erstellung und Wartung
- **Transparenz**: Lesbarkeit der resultierenden Manifeste bei Code-Reviews
- **Integration mit Flux**: Kompatibilität mit dem bestehenden GitOps-Setup (ADR-0020)
- **Wiederverwendbarkeit**: Skalierung auf mehrere Services
- **Tooling**: Abhängigkeiten und Lernkurve

### Evaluierte Alternativen

**Kustomize:**
- In kubectl integriert (`kubectl apply -k`)
- Reine YAML-Dateien mit Overlay-Patches für Umgebungsunterschiede
- Kein Templating, keine Variablen-Substitution
- Flux Kustomize-Controller bereits aktiv
- Transparentes Diff bei Code-Reviews

**Helm:**
- Go-Template-basiertes Rendering mit Values-Dateien
- Eingebautes Versioning, Packaging und Rollback
- Breites Ökosystem an Community-Charts
- Helm-Controller in Flux verfügbar
- Höhere Abstraktion, gerenderte Templates schwerer zu reviewen
- Hooks für Pre-/Post-Install-Jobs (z.B. Migrations)

## Decision

Wir verwenden **Kustomize** für die Verwaltung der Kubernetes-Manifeste.

### Begründung

- **Bereits aktiv**: Der Flux Kustomize-Controller reconciliert bereits `infra/k8s/` erfolgreich.
- **Einfachheit**: Ein einzelner Service (blog-content) rechtfertigt keinen Helm-Chart-Overhead.
- **Transparenz**: YAML bleibt direkt lesbar und reviewbar, kein Template-Rendering nötig.
- **Kein Vendor-Lock**: Kustomize ist in kubectl eingebaut, keine zusätzliche Dependency.
- **Overlay-Modell**: Umgebungsspezifische Anpassungen (Dev/Staging/Prod) über Overlays ergänzbar.
- **Konsistenz**: Passt zur existierenden Struktur (`infra/k8s/kustomization.yaml`).

### Wann Helm ergänzt werden kann

- Wenn Community-Charts als Dependency genutzt werden sollen (z.B. Redis, Kafka)
- Wenn ein Service so komplex wird, dass Templating-Logik unvermeidbar ist
- Wenn Versioning und Packaging über eine Chart-Registry benötigt wird

## Consequences

**Vorteile:**
- Minimale Tooling-Abhängigkeit
- Code-Reviews zeigen exakt die Manifeste, die deployed werden
- Kein separater Build-/Render-Schritt nötig
- Overlays ermöglichen saubere Umgebungstrennung ohne Duplikation

**Nachteile:**
- Keine Template-Logik (Conditionals, Loops) verfügbar
- Wiederverwendung über Services erfordert gemeinsame Base-Manifeste
- Kein eingebauter Rollback-Mechanismus (nur über Git-Revert)
- Komplexe Parametrisierung (viele Varianten) kann zu vielen Patch-Dateien führen

## References

- ADR-0020 (Flux für GitOps)
- ADR-0022 (Kubernetes-Plattform)
