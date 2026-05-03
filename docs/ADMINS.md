# Administrationshandbuch

Dieses Dokument beschreibt die Konfiguration und das Deployment der Toms Blog Plattform
auf einem generischen Kubernetes Cluster.

## Voraussetzungen

| Komponente         | Mindestversion | Zweck                              |
| ------------------ | -------------- | ---------------------------------- |
| Kubernetes         | 1.28+          | Container-Orchestrierung           |
| kubectl            | 1.28+          | Cluster-Verwaltung                 |
| Helm               | 3.x            | Optional, für Operator-Installtion |
| PostgreSQL         | 16+            | Datenbank (pro Service)            |
| Ingress-Controller | Traefik / Nginx| HTTP(S)-Routing                    |
| cert-manager       | 1.x            | TLS-Zertifikate (Let's Encrypt)    |
| OIDC Provider      | OIDC 1.0       | Authentifizierung (z.B. Authentik) |
| S3-Storage (Garage) | 1.x           | Backup-Ziel (externe Netcup VM)    |

## Architekturübersicht

Die Plattform besteht aus zwei Services:

| Service            | Port | Datenbank                | Beschreibung                          |
| ------------------ | ---- | ------------------------ | ------------------------------------- |
| blog-content       | 8080 | PostgreSQL (eigene DB)   | Blog-Inhalte, Thymeleaf-UI, SSR      |
| user-management    | 8081 | PostgreSQL (eigene DB)   | Benutzerverwaltung, Rollen, Tenants   |

Beide Services folgen der hexagonalen Architektur und kommunizieren über gRPC (synchron)
sowie Kafka/RabbitMQ (asynchron). REST-APIs dienen ausschließlich der Client-Kommunikation.

## Deployment

### Namespaces

```bash
kubectl create namespace tomsblog
```

### PostgreSQL

Jeder Service benötigt eine eigene PostgreSQL-Datenbank (Database-per-Service, ADR-0019).
Die Datenbank kann über den CloudNativePG Operator, einen managed Service oder eine
eigenständige PostgreSQL-Installation bereitgestellt werden.

**Beispiel mit CloudNativePG:**

```yaml
apiVersion: postgresql.cnpg.io/v1
kind: Cluster
metadata:
  name: postgres-cluster
  namespace: postgres
spec:
  instances: 2
  bootstrap:
    initdb:
      database: blog_content
      owner: blog_content
      postInitApplicationSQL:
        - CREATE DATABASE user_management OWNER app;
  storage:
    size: 10Gi
```

Beim Bootstrap werden automatisch alle Service-Datenbanken angelegt (`blog_content` via initdb, `user_management` via postInitApplicationSQL).

Die Datenbankverbindung wird über Umgebungsvariablen konfiguriert (siehe unten).

### Container-Images

Die Images werden über die GitHub Container Registry bereitgestellt:

```
ghcr.io/tomirgang/tomsblog/blog-content:<tag>
ghcr.io/tomirgang/tomsblog/user-management:<tag>
```

Als Tag wird Semantic Versioning im Format `<major>.<minor>.<patch>` verwendet (z.B. `0.8.4`).
Flux Image Automation aktualisiert das Deployment-Manifest automatisch bei neuen Releases.

### Secrets

Secrets für die Applikation müssen im Namespace `tomsblog` bereitgestellt werden:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: blog-content-secrets
  namespace: tomsblog
type: Opaque
stringData:
  admin-password: "<SuperAdmin-Passwort>"
  oidc-client-id: "<OIDC Client ID>"
  oidc-client-secret: "<OIDC Client Secret>"
  service-api-key: "<Gemeinsamer API-Key für Service-Kommunikation>"
```

Der `service-api-key` muss identisch in beiden Services konfiguriert sein
(blog-content als Client, user-management als Server).

Für die Verschlüsselung der Secrets im Git-Repository wird SOPS empfohlen (ADR-0025).

## Konfiguration

### blog-content Service

Alle Konfigurationsparameter werden über Umgebungsvariablen gesetzt.
Die Anwendung nutzt Spring Boot, daher können alle `application.yml`-Werte
über Umgebungsvariablen überschrieben werden
(z.B. `spring.datasource.url` wird zu `SPRING_DATASOURCE_URL`).

#### Datenbank

| Variable                    | Beschreibung                        | Beispiel                                                       |
| --------------------------- | ----------------------------------- | -------------------------------------------------------------- |
| `SPRING_DATASOURCE_URL`    | JDBC-URL zur PostgreSQL-Datenbank   | `jdbc:postgresql://postgres:5432/blogcontent`                  |
| `SPRING_DATASOURCE_USERNAME` | Datenbankbenutzer                 | `app`                                                          |
| `SPRING_DATASOURCE_PASSWORD` | Datenbankpasswort                 | (aus Secret)                                                   |

#### SuperAdmin (Break-Glass Login)

Der SuperAdmin-Account wird beim Start automatisch als In-Memory-Benutzer angelegt.
Der Login ist ausschließlich über `/admin/login` (formbasiert) möglich.

| Variable              | Beschreibung              | Default      |
| --------------------- | ------------------------- | ------------ |
| `BLOG_ADMIN_PASSWORD` | Passwort des SuperAdmin   | (erforderlich) |

Der Benutzername ist fest auf `admin` konfiguriert.
`BLOG_ADMIN_PASSWORD` ist eine Pflichtangabe ohne Default-Wert. Der Service startet nicht,
wenn die Variable fehlt oder leer ist. In Kubernetes wird der Wert über ein Secret gesetzt.

#### OIDC Konfiguration

Die OIDC-Anbindung an einen externen Identity Provider (z.B. Authentik, Keycloak, Entra ID)
wird vollständig über Umgebungsvariablen oder Kubernetes Secrets konfiguriert.
Eine Änderung der OIDC-Konfiguration erfordert kein Neubauen des Container-Images.

| Variable                  | Beschreibung                              | Default                                                          |
| ------------------------- | ----------------------------------------- | ---------------------------------------------------------------- |
| `OIDC_CLIENT_ID`          | Client-ID der OIDC-Registrierung          | `blog-content`                                                   |
| `OIDC_CLIENT_SECRET`      | Client-Secret der OIDC-Registrierung      | (leer)                                                           |
| `OIDC_AUTHORIZATION_URI`  | Authorization-Endpunkt des OIDC-Providers | `https://auth.do9ita.de/application/o/authorize/`                |
| `OIDC_TOKEN_URI`          | Token-Endpunkt des OIDC-Providers         | `https://auth.do9ita.de/application/o/tomsblog/token/`           |
| `OIDC_USERINFO_URI`       | UserInfo-Endpunkt des OIDC-Providers      | `https://auth.do9ita.de/application/o/tomsblog/userinfo/`        |
| `OIDC_JWKSET_URI`         | JWK-Set-Endpunkt des OIDC-Providers       | `https://auth.do9ita.de/application/o/tomsblog/jwks/`            |

**Beispiel: Authentik**

```yaml
env:
  - name: OIDC_CLIENT_ID
    valueFrom:
      secretKeyRef:
        name: blog-content-secrets
        key: oidc-client-id
  - name: OIDC_CLIENT_SECRET
    valueFrom:
      secretKeyRef:
        name: blog-content-secrets
        key: oidc-client-secret
  - name: OIDC_AUTHORIZATION_URI
    value: "https://auth.example.com/application/o/authorize/"
  - name: OIDC_TOKEN_URI
    value: "https://auth.example.com/application/o/myapp/token/"
  - name: OIDC_USERINFO_URI
    value: "https://auth.example.com/application/o/myapp/userinfo/"
  - name: OIDC_JWKSET_URI
    value: "https://auth.example.com/application/o/myapp/jwks/"
```

**Beispiel: Keycloak**

```yaml
env:
  - name: OIDC_AUTHORIZATION_URI
    value: "https://keycloak.example.com/realms/myrealm/protocol/openid-connect/auth"
  - name: OIDC_TOKEN_URI
    value: "https://keycloak.example.com/realms/myrealm/protocol/openid-connect/token"
  - name: OIDC_USERINFO_URI
    value: "https://keycloak.example.com/realms/myrealm/protocol/openid-connect/userinfo"
  - name: OIDC_JWKSET_URI
    value: "https://keycloak.example.com/realms/myrealm/protocol/openid-connect/certs"
```

#### User Management Service Verbindung

| Variable                      | Beschreibung                           | Default                    |
| ----------------------------- | -------------------------------------- | -------------------------- |
| `USER_MANAGEMENT_GRPC_HOST`   | Hostname des User Management Service   | `localhost`                |
| `USER_MANAGEMENT_GRPC_PORT`   | gRPC-Port des User Management Service  | `9090`                     |

Im Kubernetes-Cluster typischerweise:

```yaml
- name: USER_MANAGEMENT_GRPC_HOST
  value: "user-management.tomsblog.svc.cluster.local"
- name: USER_MANAGEMENT_GRPC_PORT
  value: "9090"
```

#### Sonstige

| Variable                   | Beschreibung                     | Default                                      |
| -------------------------- | -------------------------------- | -------------------------------------------- |
| `BLOG_DEFAULT_TENANT_ID`   | Standard-Tenant-ID               | `00000000-0000-0000-0000-000000000001`       |
| `BLOG_DEFAULT_AUTHOR_ID`   | Standard-Autor-ID                | `00000000-0000-0000-0000-000000000001`       |
| `SPRING_PROFILES_ACTIVE`   | Aktive Spring-Profile            | (keines)                                     |

### user-management Service

#### Datenbank

Der User Management Service benötigt eine eigene PostgreSQL-Datenbank.

| Variable                    | Beschreibung                        | Beispiel                                                       |
| --------------------------- | ----------------------------------- | -------------------------------------------------------------- |
| `SPRING_DATASOURCE_URL`    | JDBC-URL zur PostgreSQL-Datenbank   | `jdbc:postgresql://postgres:5433/usermanagement`               |
| `SPRING_DATASOURCE_USERNAME` | Datenbankbenutzer                 | `app`                                                          |
| `SPRING_DATASOURCE_PASSWORD` | Datenbankpasswort                 | (aus Secret)                                                   |

#### Service-Authentifizierung

Der User Management Service ist durch API-Key-Authentifizierung geschützt.
Alle Anfragen (außer Health-Checks) müssen den Header `X-API-Key` enthalten.

| Variable          | Beschreibung                             | Default        |
| ----------------- | ---------------------------------------- | -------------- |
| `SERVICE_API_KEY` | API-Key für Service-zu-Service-Zugriff   | (erforderlich) |

Der blog-content Service muss denselben API-Key als Umgebungsvariable erhalten,
um den User Management Service aufrufen zu können.

## Deployment-Manifest (Beispiel)

Minimales Deployment-Manifest für den blog-content Service:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: blog-content
  namespace: tomsblog
spec:
  replicas: 1
  selector:
    matchLabels:
      app: blog-content
  template:
    metadata:
      labels:
        app: blog-content
    spec:
      securityContext:
        runAsNonRoot: true
        fsGroup: 1000
      containers:
        - name: blog-content
          image: ghcr.io/tomirgang/tomsblog/blog-content:0.8.4
          ports:
            - containerPort: 8080
          securityContext:
            runAsUser: 1000
            readOnlyRootFilesystem: true
            allowPrivilegeEscalation: false
            capabilities:
              drop:
                - ALL
          env:
            - name: SPRING_DATASOURCE_URL
              value: "jdbc:postgresql://postgres:5432/blogcontent"
            - name: SPRING_DATASOURCE_USERNAME
              valueFrom:
                secretKeyRef:
                  name: db-credentials
                  key: username
            - name: SPRING_DATASOURCE_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: db-credentials
                  key: password
            - name: BLOG_ADMIN_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: blog-content-secrets
                  key: admin-password
            - name: OIDC_CLIENT_ID
              valueFrom:
                secretKeyRef:
                  name: blog-content-secrets
                  key: oidc-client-id
            - name: OIDC_CLIENT_SECRET
              valueFrom:
                secretKeyRef:
                  name: blog-content-secrets
                  key: oidc-client-secret
            - name: OIDC_AUTHORIZATION_URI
              value: "https://auth.example.com/authorize"
            - name: OIDC_TOKEN_URI
              value: "https://auth.example.com/token"
            - name: OIDC_USERINFO_URI
              value: "https://auth.example.com/userinfo"
            - name: OIDC_JWKSET_URI
              value: "https://auth.example.com/jwks"
            - name: USER_MANAGEMENT_URL
              value: "http://user-management.tomsblog.svc.cluster.local:8081"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 60
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 15
---
apiVersion: v1
kind: Service
metadata:
  name: blog-content
  namespace: tomsblog
spec:
  selector:
    app: blog-content
  ports:
    - port: 8080
      targetPort: 8080
```

## Ingress

Beispiel mit Traefik IngressRoute:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: blog-content
  namespace: tomsblog
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
spec:
  tls:
    - hosts:
        - blog.example.com
      secretName: blog-tls
  rules:
    - host: blog.example.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: blog-content
                port:
                  number: 8080
```

## Health Checks

Beide Services stellen Spring Boot Actuator Endpunkte bereit:

| Endpunkt                      | Zweck            | Beschreibung                              |
| ----------------------------- | ---------------- | ----------------------------------------- |
| `/actuator/health/liveness`   | Liveness-Probe   | Prüft nur App-Lifecycle (keine ext. Deps) |
| `/actuator/health/readiness`  | Readiness-Probe  | Prüft App-Lifecycle und Datenbankzugang   |
| `/actuator/health`            | Gesamtstatus     | Aggregiert alle Health-Indikatoren        |

IMPORTANT: Die Liveness-Probe muss `/actuator/health/liveness` verwenden (nicht `/actuator/health`), da der aggregierte Endpunkt externe Dependencies einschließt (z.B. gRPC zu user-management) und bei deren Ausfall den Pod unnötig neu startet.

## Backup-Storage (Garage / S3)

Für Backups steht ein S3-kompatibler Objektspeicher (Garage) auf einer separaten
Netcup VM bereit. Die Garage-Instanz ist unabhängig vom Kubernetes-Cluster und
dient als externes Backup-Ziel.

### Verbindungsdaten

| Parameter        | Wert                                        |
| ---------------- | ------------------------------------------- |
| Endpoint         | `https://garage.<BASE_DOMAIN>`              |
| Region           | `garage`                                    |
| Bucket           | `k8s-backup`                                |
| Force Path Style | `true`                                      |
| Access Key       | (aus Garage Key-Management)                 |
| Secret Key       | (aus Garage Key-Management)                 |

### Kubernetes Secret für Backup-Credentials

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: backup-s3-credentials
  namespace: tomsblog
type: Opaque
stringData:
  access-key: "<Access Key aus Garage>"
  secret-key: "<Secret Key aus Garage>"
  endpoint: "https://garage.<BASE_DOMAIN>"
  bucket: "k8s-backup"
```

### Verwendung

Die S3-Credentials können von Backup-Lösungen (z.B. CloudNativePG Barman,
Velero, K8up) verwendet werden, um PostgreSQL-Backups und
Cluster-Snapshots extern zu sichern.

Detaillierte Einrichtungsschritte für die Garage-Instanz selbst sind in der
separaten Infrastruktur-Dokumentation beschrieben.

## PostgreSQL Backup-Strategie

PostgreSQL-Backups werden durch CloudNativePG Barman Object Store automatisch verwaltet:

- **Tägliche Base Backups** um 02:00 UTC via `ScheduledBackup`
- **Kontinuierliches WAL-Streaming** (komprimiert mit gzip) für Point-in-Time Recovery
- **Retention:** 30 Tage
- **Ziel:** RPO < 1 Stunde, RTO < 15 Minuten

Die Konfiguration befindet sich in `infra/k8s/postgres/`:
- `cluster.yaml` (Backup-Sektion)
- `backup-s3-credentials.yaml` (S3-Zugangsdaten)
- `scheduled-backup.yaml` (Zeitplan)

### Voraussetzung: Garage-Bucket

Auf der Netcup VM muss ein dedizierter Bucket existieren:

```bash
docker compose exec garage /garage bucket create postgres-backup
docker compose exec garage /garage key create postgres-backup-key
docker compose exec garage /garage bucket allow --read --write --owner postgres-backup --key postgres-backup-key
```

Die Credentials aus `garage key info postgres-backup-key` in das Secret
`backup-s3-credentials` im Namespace `postgres` eintragen.

## Disaster Recovery

### Restore aus dem letzten Backup

Bei einem vollständigen Datenverlust kann ein neuer Cluster aus dem S3-Backup
wiederhergestellt werden:

```yaml
apiVersion: postgresql.cnpg.io/v1
kind: Cluster
metadata:
  name: postgres-cluster-restored
  namespace: postgres
spec:
  instances: 2
  bootstrap:
    recovery:
      source: postgres-cluster-backup
  externalClusters:
    - name: postgres-cluster-backup
      barmanObjectStore:
        destinationPath: s3://postgres-backup/
        endpointURL: https://garage.do9ita.de
        s3Credentials:
          accessKeyId:
            name: backup-s3-credentials
            key: ACCESS_KEY_ID
          secretAccessKey:
            name: backup-s3-credentials
            key: SECRET_ACCESS_KEY
  storage:
    size: 10Gi
    storageClass: hcloud-volumes
```

### Point-in-Time Recovery (PITR)

Für eine Wiederherstellung auf einen bestimmten Zeitpunkt:

```yaml
  bootstrap:
    recovery:
      source: postgres-cluster-backup
      recoveryTarget:
        targetTime: "2026-05-03T12:00:00Z"
```

### Ablauf

1. Ausfall feststellen und Ursache analysieren
2. Entscheiden: Restore letztes Backup oder PITR auf bestimmten Zeitpunkt
3. Recovery-Cluster-Manifest erstellen (siehe oben)
4. `kubectl apply -f postgres-cluster-restored.yaml`
5. Warten bis der Cluster `Ready` ist: `kubectl -n postgres get cluster`
6. Services auf den neuen Cluster umkonfigurieren (JDBC-URL anpassen)
7. Datenintegrität prüfen
8. Alten Cluster entfernen

## Datenbank-Migrationen

Beide Services verwenden Flyway für automatische Datenbank-Migrationen.
Die Migrationen werden beim Start des Services automatisch ausgeführt.
Ein manueller Eingriff ist nicht erforderlich.

## OIDC Provider einrichten

### Allgemeine Schritte

1. Im OIDC Provider eine neue OAuth2/OIDC Application anlegen
2. Redirect URI konfigurieren: `https://<blog-domain>/login/oauth2/code/authentik`
3. Scopes aktivieren: `openid`, `profile`, `email`
4. Client-ID und Client-Secret notieren
5. Die Endpunkt-URLs des Providers ermitteln (Authorization, Token, UserInfo, JWKS)
6. Werte als Kubernetes Secrets und Umgebungsvariablen konfigurieren (siehe oben)

### Authentik

1. Unter *Applications* eine neue *OAuth2/OpenID Provider* erstellen
2. Redirect URI: `https://<blog-domain>/login/oauth2/code/authentik`
3. Signing Key auswählen
4. Die Application mit dem Provider verknüpfen
5. Client-ID und Client-Secret aus der Provider-Konfiguration übernehmen

### Gruppen-Mapping

OIDC-Gruppen aus dem `groups`-Claim werden automatisch an den User Management Service
weitergeleitet. Die Zuordnung zu plattforminternen Rollen kann dort konfiguriert werden.

## Sicherheitshinweise

### Container-Hardening

Alle Deployments müssen mit restriktivem Security Context betrieben werden:

- `runAsNonRoot: true` (Pod und Container)
- `readOnlyRootFilesystem: true`
- `allowPrivilegeEscalation: false`
- `capabilities: drop: [ALL]`

Das Beispiel-Deployment-Manifest oben zeigt die empfohlene Konfiguration.

### Rate Limiting

Der blog-content Service enthält einen integrierten Login-Rate-Limiter:
maximal 10 Anmeldeversuche pro IP-Adresse innerhalb von 5 Minuten.
Bei Überschreitung wird HTTP 429 zurückgegeben.

Für produktive Umgebungen wird zusätzlich ein Ingress-Level Rate Limiting empfohlen
(z.B. via Traefik Middleware oder nginx `limit_req_zone`).

### Service-zu-Service-Kommunikation

Die Kommunikation zwischen blog-content und user-management erfolgt über gRPC.
Die Verbindung ist im Cluster als Plaintext konfiguriert, da Linkerd (Service Mesh)
automatisch mTLS zwischen den Pods bereitstellt (ADR-0021).

### Swagger/OpenAPI

Die API-Dokumentation (Swagger UI und OpenAPI-Spec) ist im Profil `k8s` deaktiviert.
Für lokale Entwicklung bleibt sie unter `/swagger-ui.html` zugänglich.

### Secrets-Rotation

Folgende Secrets sollten regelmäßig rotiert werden:

| Secret                | Betroffene Services          | Hinweis                                    |
| --------------------- | ---------------------------- | ------------------------------------------ |
| `BLOG_ADMIN_PASSWORD` | blog-content                 | Restart erforderlich                       |
| `SERVICE_API_KEY`     | blog-content, user-management| Beide Services gleichzeitig aktualisieren  |
| `OIDC_CLIENT_SECRET`  | blog-content                 | Im OIDC Provider gleichzeitig ändern       |
| DB-Passwörter         | Alle Services                | Restart erforderlich                       |
| S3 Access/Secret Key  | Backup-Jobs                  | In Backup-Secret und Garage aktualisieren  |
