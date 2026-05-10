---
description: "Use when creating or modifying Kubernetes manifests, Flux Kustomizations, Helm releases, or adding new infrastructure components under infra/k8s/."
applyTo: "infra/k8s/**"
---

# Kubernetes & Flux Conventions

## CRD Ordering Rule

**Never place Custom Resource instances in the same Flux Kustomization as the Operator that installs their CRDs.**

Flux validates all resources via dry-run before applying. If a Custom Resource (e.g., `Kafka`, `KafkaTopic`, `ServiceMonitor`, `MongoDBCommunity`, `RabbitmqCluster`) is in the same Kustomization as the HelmRelease that installs its CRD, the dry-run fails with `no matches for kind`.

### Three-Layer Structure

| Layer | Path | Content | Flux Kustomization |
| --- | --- | --- | --- |
| Infrastructure + Apps | `./infra/k8s` | Namespaces, Operator HelmReleases, App Deployments, Observability HelmReleases | `tomsblog` |
| CRD Instances | `./infra/k8s/clusters` | Kafka/MongoDB/RabbitMQ clusters, KafkaTopics, ServiceMonitors | `tomsblog-clusters` (dependsOn: tomsblog) |

### File Placement Rules

- **Operator HelmRelease + HelmRepository + Namespace** → `infra/k8s/operators/<name>/`
- **CR instances** (clusters, topics, queues) → `infra/k8s/clusters/<name>/`
- **ServiceMonitors** → `infra/k8s/clusters/monitoring/` (requires kube-prometheus-stack CRDs)
- **App Deployments** (Deployment, Service, Ingress, NetworkPolicy, Secret, AuthzPolicy) → `infra/k8s/<service-name>/`

### Adding a New Operator-Managed Component

1. Create `infra/k8s/operators/<name>/` with: `namespace.yaml`, `helm-repository.yaml`, `helm-release-operator.yaml`
2. Add the namespace + HelmRepo + HelmRelease to `infra/k8s/operators/kustomization.yaml`
3. Create `infra/k8s/clusters/<name>/` with the CR instances (cluster.yaml, network-policy.yaml, etc.)
4. Add the CRs to `infra/k8s/clusters/kustomization.yaml`
5. Validate with `kubectl kustomize infra/k8s/` and `kubectl kustomize infra/k8s/clusters/`

### Adding a ServiceMonitor for a Service

1. Create `infra/k8s/clusters/monitoring/<service>-servicemonitor.yaml`
2. Add it to `infra/k8s/clusters/kustomization.yaml`
3. Do NOT place it in `infra/k8s/<service>/kustomization.yaml`

## New Service Deployment Checklist

When adding a new microservice to Kubernetes:

1. Create `infra/k8s/<service>/` with: `serviceaccount.yaml`, `deployment.yaml`, `service.yaml`, `ingress.yaml`, `secret.yaml`, `server.yaml` (Linkerd), network policies, authorization policies
2. Add to `infra/k8s/kustomization.yaml`
3. Add environment variables for inter-service communication (gRPC host/port) to deployment.yaml
4. ServiceMonitor goes to `infra/k8s/clusters/monitoring/`, NOT in the service directory
5. Add `ImagePolicy` + `ImageRepository` to `infra/k8s/flux-system/`
6. Add healthCheck entry to `infra/k8s/flux-system/kustomization-app.yaml` if needed
7. Update `infra/k8s/flux-system/kustomization.yaml` with the new image policy file

## Validation

Always validate kustomize builds after changes:

```bash
kubectl kustomize infra/k8s/           # Main: must not contain any CRD instances
kubectl kustomize infra/k8s/operators/  # Operators: only Namespace, HelmRepository, HelmRelease
kubectl kustomize infra/k8s/clusters/   # Clusters: CRD instances only
```
