# OWASP Security Review

| Field       | Value                                        |
| ----------- | -------------------------------------------- |
| Datum       | 2026-05-09                                   |
| Zeit        | 10:00 UTC                                    |
| Version     | 0.9.6-SNAPSHOT                               |
| Commit      | HEAD (siehe `git rev-parse HEAD`)            |
| Prüfer      | Security Review Agent                        |
| Vorgänger   | [2026-05-03_security-review.md](2026-05-03_security-review.md) |

## Umfang der Prüfung

Vollständige OWASP-Top-10-Prüfung aller Services, Bibliotheken und Infrastruktur des Toms-Blog-Monorepos:

- **blog-content** Service (REST-API, Thymeleaf-UI, Spring Security, JPA-Persistenz, gRPC-Client, Markdown-Rendering, Kafka-/RabbitMQ-Integration)
- **user-management** Service (REST-API, Auth-UI, gRPC-Server, OIDC-Integration, API-Key-Auth, Tenant-Settings, Benutzerregistrierung)
- **tenant-management** Service (gRPC-Server, Admin-UI, Tenant-CRUD, OIDC-Konfiguration)
- **feed** Service (Kafka-Consumer, JPA-Persistenz)
- **shared-kernel** Bibliothek (TenantId, AuthorId, AuditLogger, AggregateRoot)
- **shared-ui** Bibliothek (Thymeleaf-Layout, Fragments)
- **event-contracts** Bibliothek (Domain-Event-Definitionen)
- **grpc-contracts** Bibliothek (Protobuf-Definitionen)
- **Infrastruktur** (Dockerfiles, docker-compose.yml, Kubernetes-Manifeste, SOPS-Secrets, Network Policies, Linkerd AuthorizationPolicies, Ingress, CloudNativePG)
- **E2E-Tests** (Selenium-basierte Tests)

Besonderer Fokus: Multi-Tenant-Isolation, Authentifizierung/Autorisierung, Input-Validierung, sichere Konfiguration.

### Fortschritt seit letztem Audit (2026-05-03)

Folgende Findings aus dem vorherigen Audit wurden behoben:

| # (alt) | Titel                                              | Status   |
| ------- | -------------------------------------------------- | -------- |
| 1       | REST-API ohne Authentifizierung                    | BEHOBEN  |
| 2       | User-Management-Service ohne Authentifizierung      | BEHOBEN  |
| 3       | XSS durch unsanitisierten Markdown-Output           | BEHOBEN  |
| 4       | Default-Admin-Passwort "admin"                      | BEHOBEN  |
| 9       | K8s-Deployment ohne SecurityContext                  | BEHOBEN  |
| 12      | Kein Rate-Limiting für Login-Endpunkte               | BEHOBEN  |

## Prüfungen

### A01: Broken Access Control

- REST-API-Endpunkte: Prüfung aller CRUD-Operationen auf `X-Tenant-Id`-Header-Auswertung und Tenant-Isolation in Repository-Queries
- Blog-Content SecurityFilterChain: Rollenpezifische Zugriffskontrollen für API-Methoden (GET/POST/PUT/PATCH/DELETE), Admin-Bereiche, Thymeleaf-UI
- User-Management SecurityFilterChain: API-Chain (Order 1, API-Key), Admin-Chain (Order 2, form-based SUPERADMIN), Auth-UI-Chain (Order 3, OIDC+form), Default-Chain (Order 4)
- Tenant-Management SecurityFilterChain: Admin-Chain (Order 1, SUPERADMIN), Default-Chain (Order 2)
- Tag-Admin-Controller: Prüfung ob `/admin/tags/**` korrekt geschützt ist
- Open-Redirect: Prüfung der `switchTenant()`-Methoden auf Referer-Header-Nutzung
- gRPC-Endpunkte: Prüfung auf fehlende Authentifizierung
- IDOR: Prüfung auf fehlende Ownership-Checks bei Post-Bearbeitung

**Ergebnis:** REST-API ist jetzt rollenbasiert geschützt (GET erfordert Authentication, POST/PUT/PATCH erfordern AUTHOR/ADMIN/SUPERADMIN, DELETE erfordert ADMIN/SUPERADMIN). User-Management hat API-Key-Auth für Service-to-Service-Kommunikation. Tenant-Management hat SUPERADMIN-Auth. Allerdings bestehen mehrere neue Findings (Open Redirect, fehlende Ownership-Checks, Tag-Admin-Schutz, gRPC-Authentifizierung).

### A02: Cryptographic Failures

- BCrypt für Break-Glass-Admin und Benutzerregistrierung
- OIDC-Client-Secret-Verwaltung über Umgebungsvariablen
- TLS: Let's Encrypt via cert-manager, HTTPS-Redirect via Traefik-Middleware
- gRPC-Kommunikation: Plaintext-Konfiguration, Linkerd-mTLS-Kompensation
- API-Key-Vergleich: Timing-Sicherheit des `equals()`-Vergleichs geprüft
- Passwort-Hashing: PasswordEncoder-Implementierung geprüft
- K8s-Secrets: SOPS/age-Verschlüsselung geprüft
- Redis-Passwort-Konfiguration geprüft

**Ergebnis:** BCrypt wird korrekt für Passwörter verwendet. SOPS/age verschlüsselt K8s-Secrets. Der API-Key-Vergleich ist nicht timing-sicher. gRPC-Plaintext wird durch Linkerd-mTLS kompensiert (bewusste Designentscheidung gemäß ADR-0021).

### A03: Injection

- SQL-Injection: Alle Spring Data JPA Queries, Native Fulltext-Query mit `plainto_tsquery` und `@Param`-Bindings
- XSS: Markdown-Rendering mit OWASP HTML Sanitizer (`HtmlPolicyBuilder`) geprüft
- XSS: Legal-Pages (Impressum, Datenschutz) verwenden `th:utext` mit Admin-kontrolliertem HTML-Inhalt
- XSS: Thymeleaf-Templates systematisch auf `th:utext` vs. `th:text` geprüft
- gRPC: Protobuf-basierte Serialisierung (keine String-Interpolation)

**Ergebnis:** SQL-Injection wird durch parametrisierte Queries und `plainto_tsquery` verhindert. Markdown-XSS ist durch OWASP HTML Sanitizer behoben. Allerdings verwenden die Legal-Pages `th:utext` für Admin-konfigurierbaren HTML-Inhalt ohne Sanitisierung (neues Finding).

### A04: Insecure Design

- Hexagonale Architektur: Domain-Schicht frei von Framework-Abhängigkeiten
- Multi-Tenant-Design: Tenant-Isolation via `X-Tenant-Id`-Header, Repository-Queries filtern nach `tenantId`
- Autorisierungsmodell: Rollenbasierte Zugriffskontrolle (READER, AUTHOR, ADMIN, SUPERADMIN)
- Benutzer-Genehmigungsworkflow: PENDING -> APPROVED/REJECTED
- Domain-Validierung: Input-Validierung in Domain-Objekten und via Bean Validation
- Rate Limiting: `LoginRateLimitFilter` mit IP-basierter Beschränkung

**Ergebnis:** Die hexagonale Architektur und das Multi-Tenant-Design sind sauber implementiert. Das Rate-Limiting für Login-Versuche ist vorhanden, nutzt aber `X-Forwarded-For` ohne Validierung.

### A05: Security Misconfiguration

- Spring Security: CSRF-Konfiguration (deaktiviert für `/api/**` in blog-content), HTTP Basic (aktiviert in blog-content)
- Actuator: Nur `/actuator/health` und Sub-Paths öffentlich
- Swagger/OpenAPI: In k8s-Profil deaktiviert, in Dev/Local aktiv
- Dockerfiles: Non-root-User, readOnlyRootFilesystem, Capabilities gedroppt
- K8s-Deployments: SecurityContext vollständig (runAsNonRoot, runAsUser 1000, readOnlyRootFilesystem, allowPrivilegeEscalation false, drop ALL)
- K8s-Network-Policies: Default-Deny-Ingress, explizite Allow-Rules
- K8s-Linkerd AuthorizationPolicies: MeshTLS-Authentifizierung für Traefik und Inter-Service-Kommunikation
- AdminProperties: Startup-Fehler bei fehlendem Passwort

**Ergebnis:** Gute Sicherheitskonfiguration. HTTP Basic ist in blog-content ohne Grund aktiviert (Finding). CSRF-Deaktivierung für API ist akzeptabel, da API-Endpunkte session-basierte Auth nutzen. K8s-Sicherheit ist vorbildlich.

### A06: Vulnerable and Outdated Components

- Spring Boot 3.5.14 (aktuell)
- Java 25 (aktuell)
- gRPC 1.72.0, Testcontainers 1.21.4, Flexmark 0.64.8, springdoc 2.8.8
- Docker-Base-Images: eclipse-temurin:25-jre-alpine, PostgreSQL 17, Redis 7-alpine, Kafka 3.9.0
- OWASP Dependency Check Plugin: Version 12.1.1 konfiguriert
- OWASP Java HTML Sanitizer: 20240325.1

**Ergebnis:** Alle Hauptabhängigkeiten sind aktuell. OWASP Dependency Check ist als Plugin konfiguriert.

### A07: Identification and Authentication Failures

- OIDC-Integration: Tenant-spezifische Client-Registration, SyncingOidcUserService
- Break-Glass-Admin: BCrypt-geschützt, Pflicht-Passwort via Umgebungsvariable
- API-Key-Authentifizierung: Service-to-Service-Kommunikation
- Passwort-Policy: Mindestens 12 Zeichen (kein Komplexitätserfordernis)
- Rate-Limiting: LoginRateLimitFilter (10 Versuche/5 Minuten pro IP)
- Benutzer-Genehmigungsworkflow: Auto-Approval für OIDC und E-Mail-Domains konfigurierbar
- Session-Management: Redis-basierter Session-Store mit geteilten Sessions

**Ergebnis:** Authentifizierung ist solide implementiert. Passwort-Komplexitätsanforderungen fehlen. LoginRateLimitFilter hat eine potenzielle Memory-Leak-Schwachstelle.

### A08: Software and Data Integrity Failures

- Flyway-Migrationen: Checksummen-geschützt, `ddl-auto: validate` in allen Services
- Docker-Images: Vertrauenswürdiges Base-Image (eclipse-temurin), Flux-Image-Automation
- Kafka-Events: JSON-Serialisierung mit `spring.json.trusted.packages` Einschränkung
- gRPC: Protobuf-basierte Serialisierung (typsicher)
- RabbitMQ: Jackson2JsonMessageConverter mit Dead-Letter-Queues

**Ergebnis:** Flyway-Migrationen sind sauber. Docker-Images nutzen vertrauenswürdige Basis. Kafka trusted-packages Einschränkung vorhanden.

### A09: Security Logging and Monitoring Failures

- Audit-Trail: `AuditLogger`-Implementierung in allen Services (JpaAuditLogger)
- JPA-Auditing: `SecurityAuditorAware` befüllt `createdBy`/`updatedBy` aus SecurityContext
- gRPC-Exception-Interceptor: Loggt unerwartete Fehler, gibt keine Details an Clients weiter
- GlobalExceptionHandler: Loggt unerwartete Fehler, gibt generische Fehlermeldungen zurück
- Sicherheitsrelevante Aktionen: USER_SYNCED_OIDC, USER_REGISTERED, USER_APPROVED, USER_REJECTED, GLOBAL_ROLE_ASSIGNED werden auditiert
- K8s: Liveness-/Readiness-Probes konfiguriert

**Ergebnis:** Audit-Logging ist implementiert und befüllt. Sicherheitsrelevante Aktionen werden nachvollziehbar protokolliert. Fehlgeschlagene Login-Versuche werden nicht explizit auditiert.

### A10: Server-Side Request Forgery (SSRF)

- Source-URLs: Werden nur gespeichert und als Links angezeigt, nicht serverseitig aufgelöst
- OIDC-Provider-URIs: `TenantAwareClientRegistrationRepository` baut URLs aus Tenant-Settings, die in der DB gespeichert sind
- gRPC-Client-Adressen: Statisch konfiguriert via Umgebungsvariablen

**Ergebnis:** Kein direktes SSRF-Risiko bei Source-URLs. Allerdings können OIDC-Issuer-URLs durch Tenant-Admins gesetzt werden, was zu SSRF führen kann, wenn der Server intern erreichbare Endpunkte kontaktiert (neues Finding).

## Findings

| #  | Severity      | OWASP | Titel                                                             | Betroffene Datei(en)                                                |
| -- | ------------- | ----- | ----------------------------------------------------------------- | ------------------------------------------------------------------- |
| 1  | High          | A01   | Open Redirect via Referer-Header in switchTenant()                | AuthAdminController.java, TenantAdminController.java                |
| 2  | Medium        | A01   | gRPC-Endpunkte ohne Anwendungsebene-Authentifizierung             | UserManagementGrpcService.java, TenantManagementGrpcService.java    |
| 3  | Medium        | A01   | Tag-Admin-Controller fehlt explizite Rollenbeschränkung           | SecurityConfiguration.java (blog-content), TagAdminController.java  |
| 4  | Medium        | A01   | Fehlende Ownership-Prüfung bei Post-Bearbeitung                  | PostController.java, BlogViewController.java                       |
| 5  | Medium        | A02   | API-Key-Vergleich nicht timing-sicher                             | ApiKeyAuthenticationFilter.java                                     |
| 6  | Medium        | A03   | XSS-Risiko in Legal-Pages durch unsanitisierten Admin-HTML-Inhalt | impressum.html, privacy.html, LegalController.java                 |
| 7  | Low           | A05   | HTTP Basic ohne Notwendigkeit aktiviert in blog-content           | SecurityConfiguration.java (blog-content)                           |
| 8  | Low           | A05   | CSRF-Schutz für API-Endpunkte deaktiviert (bei Session-Auth)     | SecurityConfiguration.java (blog-content)                           |
| 9  | Low           | A07   | Keine Passwort-Komplexitätsanforderungen                         | AuthRegistrationController.java, RegisterUserRequest.java           |
| 10 | Low           | A07   | LoginRateLimitFilter: Memory-Leak durch fehlende Bereinigung     | LoginRateLimitFilter.java                                          |
| 11 | Low           | A10   | SSRF-Potenzial durch Tenant-konfigurierbare OIDC-Issuer-URLs     | TenantAwareClientRegistrationRepository.java                       |
| 12 | Informational | A02   | gRPC-Plaintext-Kommunikation (kompensiert durch Linkerd mTLS)    | application.yml (blog-content)                                     |
| 13 | Informational | A05   | Hardcoded Credentials in docker-compose.yml (nur Entwicklung)    | docker-compose.yml                                                 |
| 14 | Informational | A02   | JPA show-sql in local-Profil aktiviert                           | application-local.yml                                              |
| 15 | Informational | A09   | Fehlgeschlagene Login-Versuche werden nicht auditiert            | LoginRateLimitFilter.java, SecurityConfiguration.java               |

---

### Finding 1: Open Redirect via Referer-Header in switchTenant()

**Severity:** High
**OWASP Category:** A01 - Broken Access Control
**Betroffene Dateien:** [AuthAdminController.java](../../services/user-management/src/main/java/de/tomsblog/usermanagement/adapter/inbound/web/AuthAdminController.java#L153-L157), [TenantAdminController.java](../../services/tenant-management/src/main/java/de/tomsblog/tenantmanagement/adapter/inbound/web/TenantAdminController.java#L121-L125)

**Beschreibung:**
Beide `switchTenant()`-Methoden verwenden den `Referer`-HTTP-Header direkt als Redirect-Ziel ohne jegliche Validierung. Ein Angreifer kann den Referer-Header manipulieren, um Benutzer nach einem Tenant-Wechsel auf eine externe, bösartige Seite umzuleiten. Dies ermöglicht Phishing-Angriffe, bei denen ein Opfer auf eine gefälschte Login-Seite umgeleitet wird.

**Beweis:**
```java
// AuthAdminController.java
@PostMapping("/switch-tenant")
public String switchTenant(@RequestParam UUID tenantId, HttpSession session, HttpServletRequest request) {
    session.setAttribute("activeTenantId", tenantId.toString());
    String referer = request.getHeader("Referer");
    return "redirect:" + (referer != null ? referer : "/");
}
```

```java
// TenantAdminController.java
@PostMapping("/switch-tenant")
public String switchTenant(@RequestParam UUID tenantId, HttpSession session, HttpServletRequest request) {
    session.setAttribute("activeTenantId", tenantId.toString());
    String referer = request.getHeader("Referer");
    return "redirect:" + (referer != null ? referer : "/tenant/admin/tenants");
}
```

**Empfohlener Fix:**
Validieren, dass der Referer-Header ein relativer Pfad oder eine URL auf derselben Domain ist:
```java
@PostMapping("/switch-tenant")
public String switchTenant(@RequestParam UUID tenantId, HttpSession session, HttpServletRequest request) {
    session.setAttribute("activeTenantId", tenantId.toString());
    String referer = request.getHeader("Referer");
    if (referer != null && isSafeRedirect(referer, request)) {
        return "redirect:" + referer;
    }
    return "redirect:/";
}

private boolean isSafeRedirect(String url, HttpServletRequest request) {
    try {
        var uri = java.net.URI.create(url);
        // Nur relative Pfade oder gleicher Host erlauben
        return !uri.isAbsolute() || request.getServerName().equals(uri.getHost());
    } catch (Exception e) {
        return false;
    }
}
```

---

### Finding 2: gRPC-Endpunkte ohne Anwendungsebene-Authentifizierung

**Severity:** Medium
**OWASP Category:** A01 - Broken Access Control
**Betroffene Dateien:** [UserManagementGrpcService.java](../../services/user-management/src/main/java/de/tomsblog/usermanagement/adapter/inbound/grpc/UserManagementGrpcService.java), [TenantManagementGrpcService.java](../../services/tenant-management/src/main/java/de/tomsblog/tenantmanagement/adapter/inbound/grpc/TenantManagementGrpcService.java), [TenantSettingsGrpcService.java](../../services/user-management/src/main/java/de/tomsblog/usermanagement/adapter/inbound/grpc/TenantSettingsGrpcService.java)

**Beschreibung:**
Die gRPC-Server-Endpunkte in user-management und tenant-management haben keine Anwendungsebene-Authentifizierung. Sie verlassen sich ausschließlich auf Kubernetes-NetworkPolicies und Linkerd-mTLS-AuthorizationPolicies. Obwohl dies durch die Zero-Trust-Architektur (ADR-0021) kompensiert wird, bietet Defense-in-Depth eine zusätzliche Sicherheitsebene für den Fall, dass Netzwerk-Layer-Schutzmaßnahmen umgangen werden (z.B. durch Pod-Kompromittierung innerhalb des gleichen Namespace).

**Beweis:**
```java
// UserManagementGrpcService.java - Kein Authentifizierungs-Interceptor
@GrpcService
public class UserManagementGrpcService extends UserManagementServiceGrpc.UserManagementServiceImplBase {
    // Alle Methoden sind ohne Auth erreichbar
}
```

```yaml
# network-policy-intra.yaml - Erlaubt Intra-Namespace-Kommunikation
ingress:
  - from:
      - podSelector: {}  # Alle Pods im gleichen Namespace
```

**Empfohlener Fix:**
Einen gRPC-Server-Interceptor hinzufügen, der ein Service-Token oder Metadata-basierte Authentifizierung prüft:
```java
@GrpcGlobalServerInterceptor
public class GrpcAuthInterceptor implements ServerInterceptor {
    private static final Metadata.Key<String> API_KEY =
        Metadata.Key.of("x-api-key", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        String key = headers.get(API_KEY);
        if (!expectedApiKey.equals(key)) {
            call.close(Status.UNAUTHENTICATED, new Metadata());
            return new ServerCall.Listener<>() {};
        }
        return next.startCall(call, headers);
    }
}
```

---

### Finding 3: Tag-Admin-Controller fehlt explizite Rollenbeschränkung

**Severity:** Medium
**OWASP Category:** A01 - Broken Access Control
**Betroffene Dateien:** [SecurityConfiguration.java](../../services/blog-content/src/main/java/de/tomsblog/blogcontent/adapter/inbound/web/SecurityConfiguration.java#L55-L60), [TagAdminController.java](../../services/blog-content/src/main/java/de/tomsblog/blogcontent/adapter/inbound/web/TagAdminController.java)

**Beschreibung:**
Der `TagAdminController` ist unter `/admin/tags` gemountet. In der `SecurityConfiguration` gibt es eine explizite Regel für `/admin/audit-logs` (ADMIN/SUPERADMIN), aber keine für `/admin/tags/**`. Die Pfade `/admin/tags` und `/admin/tags/**` fallen unter die Default-Regel `.anyRequest().authenticated()`, was bedeutet, dass jeder authentifizierte Benutzer (einschließlich READER) Tags erstellen, umbenennen und löschen kann.

**Beweis:**
```java
// SecurityConfiguration.java - blog-content
.requestMatchers("/admin/audit-logs")
.hasAnyRole("ADMIN", "SUPERADMIN")
// /admin/tags fehlt hier!
.requestMatchers("/posts/new", "/posts/*/edit", "/posts/*/preview")
.authenticated()
// ...
.anyRequest()
.authenticated() // READER kann /admin/tags erreichen
```

**Empfohlener Fix:**
Explizite Rollenbeschränkung für `/admin/tags/**` hinzufügen:
```java
.requestMatchers("/admin/audit-logs")
.hasAnyRole("ADMIN", "SUPERADMIN")
.requestMatchers("/admin/tags", "/admin/tags/**")
.hasAnyRole("ADMIN", "SUPERADMIN")
```

---

### Finding 4: Fehlende Ownership-Prüfung bei Post-Bearbeitung

**Severity:** Medium
**OWASP Category:** A01 - Broken Access Control
**Betroffene Dateien:** [PostController.java](../../services/blog-content/src/main/java/de/tomsblog/blogcontent/adapter/inbound/rest/PostController.java), [BlogViewController.java](../../services/blog-content/src/main/java/de/tomsblog/blogcontent/adapter/inbound/web/BlogViewController.java)

**Beschreibung:**
Jeder Benutzer mit der Rolle AUTHOR kann alle Posts desselben Tenants bearbeiten, veröffentlichen oder löschen (über die API benötigt er ADMIN für DELETE). Es gibt keine Prüfung, ob der aktuelle Benutzer der Autor des Posts ist. Im Web-Controller (`BlogViewController`) sind Edit- und Preview-Endpunkte nur `authenticated()` geschützt, sodass auch READER die Edit-Seite aufrufen können (die Form-Submission wird durch die API-Rollenbeschränkung verhindert, aber das ist nicht konsistent).

**Beweis:**
```java
// PostController.java - Kein Ownership-Check
@PutMapping("/{id}")
public ResponseEntity<PostResponse> updatePost(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @PathVariable UUID id,
        @Valid @RequestBody UpdatePostRequest request) {
    // Jeder AUTHOR kann jeden Post bearbeiten
    UpdatePostCommand command = new UpdatePostCommand(PostId.of(id), TenantId.of(tenantId), ...);
    Post post = postUseCase.updatePost(command);
    return ResponseEntity.ok(PostResponse.from(post));
}
```

**Empfohlener Fix:**
Ownership-Prüfung im `PostService` hinzufügen:
```java
public Post updatePost(UpdatePostCommand command) {
    Post post = repository.findById(command.postId(), command.tenantId())
        .orElseThrow(() -> new PostNotFoundException(command.postId()));
    // Nur Autor oder Admin darf bearbeiten
    if (!post.getAuthorId().equals(currentUser.getAuthorId())
        && !currentUser.hasRole(Role.ADMIN)) {
        throw new AccessDeniedException("Not the post author");
    }
    // ...
}
```

---

### Finding 5: API-Key-Vergleich nicht timing-sicher

**Severity:** Medium
**OWASP Category:** A02 - Cryptographic Failures
**Betroffene Dateien:** [ApiKeyAuthenticationFilter.java](../../services/user-management/src/main/java/de/tomsblog/usermanagement/adapter/inbound/rest/ApiKeyAuthenticationFilter.java#L40)

**Beschreibung:**
Der API-Key-Vergleich in `ApiKeyAuthenticationFilter` verwendet `String.equals()`, was anfällig für Timing-Angriffe ist. Ein Angreifer kann durch Messung der Antwortzeiten Byte für Byte den korrekten API-Key ermitteln.

**Beweis:**
```java
// ApiKeyAuthenticationFilter.java
if (expectedApiKey != null && expectedApiKey.equals(providedKey)) {
    var auth = new ApiKeyAuthentication(expectedApiKey);
    SecurityContextHolder.getContext().setAuthentication(auth);
}
```

**Empfohlener Fix:**
`MessageDigest.isEqual()` für einen konstanten Zeitvergleich verwenden:
```java
import java.security.MessageDigest;

if (expectedApiKey != null && providedKey != null
        && MessageDigest.isEqual(
            expectedApiKey.getBytes(java.nio.charset.StandardCharsets.UTF_8),
            providedKey.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
    var auth = new ApiKeyAuthentication(expectedApiKey);
    SecurityContextHolder.getContext().setAuthentication(auth);
}
```

---

### Finding 6: XSS-Risiko in Legal-Pages durch unsanitisierten Admin-HTML-Inhalt

**Severity:** Medium
**OWASP Category:** A03 - Injection
**Betroffene Dateien:** [impressum.html](../../services/blog-content/src/main/resources/templates/legal/impressum.html#L12), [privacy.html](../../services/blog-content/src/main/resources/templates/legal/privacy.html#L12), [LegalController.java](../../services/blog-content/src/main/java/de/tomsblog/blogcontent/adapter/inbound/web/LegalController.java)

**Beschreibung:**
Die Impressum- und Datenschutz-Seiten verwenden `th:utext` zur Ausgabe von Admin-konfigurierbarem HTML-Inhalt (`customContent`). Dieser Inhalt wird von Tenant-Admins über die Einstellungen-UI gesetzt und unsanitisiert in der Datenbank gespeichert. Obwohl nur Admins diesen Inhalt setzen können (Privilege-Required), besteht ein Stored-XSS-Risiko durch:
1. Kompromittierte Admin-Accounts
2. Social Engineering
3. Fehlende Sanitisierung bei der Eingabe

**Beweis:**
```html
<!-- impressum.html -->
<div th:if="${customContent}" th:utext="${customContent}">
    Tenant-spezifisches Impressum
</div>
```

```html
<!-- privacy.html -->
<div th:if="${customContent}" th:utext="${customContent}">
    Tenant-spezifische Datenschutzerklärung
</div>
```

**Empfohlener Fix:**
HTML-Sanitisierung beim Speichern der Legal-Inhalte anwenden oder vor dem Rendern sanitisieren:
```java
// LegalController.java
private static final PolicyFactory LEGAL_POLICY = new HtmlPolicyBuilder()
    .allowCommonBlockElements()
    .allowCommonInlineFormattingElements()
    .allowElements("a", "img", "table", "thead", "tbody", "tr", "th", "td")
    .allowAttributes("href").onElements("a")
    .allowAttributes("src", "alt").onElements("img")
    .allowUrlProtocols("http", "https", "mailto")
    .requireRelNofollowOnLinks()
    .toFactory();

// Im Controller vor model.addAttribute:
model.addAttribute("customContent", LEGAL_POLICY.sanitize(settings.impressumContent()));
```

---

### Finding 7: HTTP Basic ohne Notwendigkeit aktiviert in blog-content

**Severity:** Low
**OWASP Category:** A05 - Security Misconfiguration
**Betroffene Dateien:** [SecurityConfiguration.java](../../services/blog-content/src/main/java/de/tomsblog/blogcontent/adapter/inbound/web/SecurityConfiguration.java#L80)

**Beschreibung:**
In der blog-content SecurityConfiguration ist HTTP Basic Authentication mit `.httpBasic(basic -> {})` aktiviert. Der Service nutzt session-basierte Auth (Redis) und Redirect zum Auth-Login. HTTP Basic ist nicht notwendig und stellt ein Risiko dar, da Credentials in Base64 (nicht verschlüsselt) übertragen werden. Obwohl TLS vorhanden ist, bietet HTTP Basic keine Vorteile gegenüber der bestehenden session-basierten Auth.

**Beweis:**
```java
// SecurityConfiguration.java - blog-content
.httpBasic(basic -> {})  // Aktiviert HTTP Basic ohne Konfiguration
```

Die user-management und tenant-management Services deaktivieren HTTP Basic korrekt:
```java
.httpBasic(basic -> basic.disable())
```

**Empfohlener Fix:**
HTTP Basic deaktivieren:
```java
.httpBasic(basic -> basic.disable())
```

---

### Finding 8: CSRF-Schutz für API-Endpunkte deaktiviert (bei Session-Auth)

**Severity:** Low
**OWASP Category:** A05 - Security Misconfiguration
**Betroffene Dateien:** [SecurityConfiguration.java](../../services/blog-content/src/main/java/de/tomsblog/blogcontent/adapter/inbound/web/SecurityConfiguration.java#L81)

**Beschreibung:**
CSRF-Schutz ist für `/api/**`-Endpunkte in blog-content deaktiviert. Da die API session-basierte Authentifizierung verwendet (keine separaten API-Tokens), können Cross-Site-Request-Forgery-Angriffe auf die API durchgeführt werden. Ein Angreifer könnte eine bösartige Website erstellen, die im Namen eines eingeloggten Benutzers API-Requests absetzt (z.B. Post erstellen, Tags löschen).

Die user-management- und tenant-management-Services haben dieses Problem ebenfalls in ihren Default-Filter-Chains, aber deren API-Endpoints sind durch API-Key-Auth (stateless) geschützt.

**Beweis:**
```java
// SecurityConfiguration.java - blog-content
.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
```

**Empfohlener Fix:**
Entweder CSRF für die API aktivieren (mit `CookieCsrfTokenRepository`) oder die API auf token-basierte Authentifizierung umstellen:
```java
// Option A: CSRF mit Cookie-basiertem Token
.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
    .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
// Option B: API auf Bearer-Token-Auth umstellen (langfristig besser)
```

---

### Finding 9: Keine Passwort-Komplexitätsanforderungen

**Severity:** Low
**OWASP Category:** A07 - Identification and Authentication Failures
**Betroffene Dateien:** [AuthRegistrationController.java](../../services/user-management/src/main/java/de/tomsblog/usermanagement/adapter/inbound/web/AuthRegistrationController.java#L47), [RegisterUserRequest.java](../../services/user-management/src/main/java/de/tomsblog/usermanagement/adapter/inbound/rest/RegisterUserRequest.java)

**Beschreibung:**
Die Passwort-Validierung bei der Benutzerregistrierung prüft nur die Mindestlänge (12 Zeichen). Es gibt keine Anforderungen an Komplexität (Großbuchstaben, Kleinbuchstaben, Ziffern, Sonderzeichen). Zwar ist eine Mindestlänge von 12 Zeichen ein guter Anfang, aber ohne Komplexitätsanforderungen sind Passwörter wie "aaaaaaaaaaaa" oder "123456789012" möglich.

**Beweis:**
```java
// AuthRegistrationController.java
private static final int MIN_PASSWORD_LENGTH = 12;

if (password.length() < MIN_PASSWORD_LENGTH) {
    model.addAttribute("error", "Das Passwort muss mindestens " + MIN_PASSWORD_LENGTH + " Zeichen lang sein.");
    return "register";
}
// Keine Komplexitätsprüfung
```

```java
// RegisterUserRequest.java
@NotBlank @Size(min = 12) String password,
// Keine @Pattern-Annotation für Komplexität
```

**Empfohlener Fix:**
Passwort-Komplexitätsregeln hinzufügen (NIST SP 800-63B empfiehlt lange Passwörter, aber auch gegen bekannte kompromittierte Passwörter prüfen):
```java
private boolean isPasswordStrong(String password) {
    if (password.length() < MIN_PASSWORD_LENGTH) return false;
    boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
    boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
    boolean hasDigit = password.chars().anyMatch(Character::isDigit);
    return hasUpper && hasLower && hasDigit;
}
```

---

### Finding 10: LoginRateLimitFilter: Memory-Leak durch fehlende Bereinigung

**Severity:** Low
**OWASP Category:** A07 - Identification and Authentication Failures
**Betroffene Dateien:** [LoginRateLimitFilter.java](../../services/user-management/src/main/java/de/tomsblog/usermanagement/adapter/inbound/web/LoginRateLimitFilter.java)

**Beschreibung:**
Der `LoginRateLimitFilter` speichert Rate-Limit-Einträge pro IP-Adresse in einer `ConcurrentHashMap`. Abgelaufene Einträge werden nur ersetzt, wenn die gleiche IP erneut zugreift (`compute`-Callback prüft `isExpired()`). Bei einem DDoS-Angriff mit vielen verschiedenen IP-Adressen werden Einträge nie bereinigt, was zu einem schleichenden Memory-Leak führt.

Zusätzlich verwendet der Filter `X-Forwarded-For` ohne Validierung. Ein Angreifer könnte den Header fälschen, um das Rate-Limiting zu umgehen (in K8s wird der Header durch Traefik gesetzt und ist vertrauenswürdig, aber bei direktem Zugriff nicht).

**Beweis:**
```java
// LoginRateLimitFilter.java
private final ConcurrentHashMap<String, RateEntry> attempts = new ConcurrentHashMap<>();
// Keine periodische Bereinigung abgelaufener Einträge

static String getClientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
        return forwarded.split(",")[0].trim(); // Kein Spoofing-Schutz
    }
    return request.getRemoteAddr();
}
```

**Empfohlener Fix:**
Periodische Bereinigung mittels ScheduledExecutorService oder Caffeine-Cache mit TTL:
```java
private final Cache<String, RateEntry> attempts = Caffeine.newBuilder()
    .expireAfterWrite(Duration.ofSeconds(WINDOW_SECONDS))
    .maximumSize(100_000)
    .build();
```

---

### Finding 11: SSRF-Potenzial durch Tenant-konfigurierbare OIDC-Issuer-URLs

**Severity:** Low
**OWASP Category:** A10 - Server-Side Request Forgery
**Betroffene Dateien:** [TenantAwareClientRegistrationRepository.java](../../services/user-management/src/main/java/de/tomsblog/usermanagement/adapter/inbound/web/TenantAwareClientRegistrationRepository.java)

**Beschreibung:**
Die `TenantAwareClientRegistrationRepository` baut OIDC-Provider-URLs (Authorization-URI, Token-URI, UserInfo-URI, JWK-Set-URI) aus der `oidcIssuerUrl` zusammen, die von Tenant-Admins über die Einstellungs-UI konfiguriert werden kann. Ein bösartiger oder kompromittierter Tenant-Admin könnte die Issuer-URL auf einen internen Service setzen (z.B. `http://redis.redis.svc.cluster.local:6379/`), was zu SSRF führen kann, wenn der Spring OAuth2-Client versucht, Tokens von diesem Endpunkt abzurufen.

**Beweis:**
```java
// TenantAwareClientRegistrationRepository.java
private ClientRegistration buildRegistration(String registrationId, TenantSettings settings) {
    String issuerUrl = settings.getOidcIssuerUrl(); // Admin-kontrolliert, keine Validierung
    return ClientRegistration.withRegistrationId(registrationId)
        .tokenUri(issuerUrl + "token/")     // Server kontaktiert diese URL
        .userInfoUri(issuerUrl + "userinfo/") // Server kontaktiert diese URL
        .jwkSetUri(issuerUrl + "jwks/")       // Server kontaktiert diese URL
        .build();
}
```

**Empfohlener Fix:**
Validierung der OIDC-Issuer-URL gegen eine Allowlist oder mindestens gegen interne Netzwerke:
```java
private void validateIssuerUrl(String issuerUrl) {
    var uri = java.net.URI.create(issuerUrl);
    if (!"https".equals(uri.getScheme())) {
        throw new IllegalArgumentException("OIDC issuer URL must use HTTPS");
    }
    String host = uri.getHost();
    if (host.endsWith(".svc.cluster.local") || host.equals("localhost")
            || host.startsWith("10.") || host.startsWith("192.168.")) {
        throw new IllegalArgumentException("OIDC issuer URL must not point to internal services");
    }
}
```

---

### Finding 12: gRPC-Plaintext-Kommunikation (kompensiert durch Linkerd mTLS)

**Severity:** Informational
**OWASP Category:** A02 - Cryptographic Failures
**Betroffene Dateien:** [application.yml](../../services/blog-content/src/main/resources/application.yml#L19)

**Beschreibung:**
Die gRPC-Kommunikation zwischen blog-content und user-management verwendet `negotiation-type: plaintext`. Dies ist eine bewusste Designentscheidung (ADR-0021): Linkerd Service Mesh stellt mTLS zwischen allen Pods bereit. Der Kommentar im Code dokumentiert dies korrekt. Bei einem Ausfall oder einer Fehlkonfiguration von Linkerd wäre die Kommunikation jedoch unverschlüsselt.

**Beweis:**
```yaml
# application.yml - blog-content
grpc:
  client:
    user-management:
      address: static://${USER_MANAGEMENT_GRPC_HOST:localhost}:${USER_MANAGEMENT_GRPC_PORT:9090}
      negotiation-type: plaintext
      # gRPC: plaintext is acceptable because Linkerd service mesh provides mTLS (ADR-0021)
```

**Empfohlener Fix:**
Kein unmittelbarer Handlungsbedarf. Die Kompensation durch Linkerd mTLS ist dokumentiert und durch K8s-AuthorizationPolicies (MeshTLSAuthentication) durchgesetzt. Langfristig könnte native gRPC-TLS als zusätzliche Defense-in-Depth-Maßnahme erwogen werden.

---

### Finding 13: Hardcoded Credentials in docker-compose.yml (nur Entwicklung)

**Severity:** Informational
**OWASP Category:** A05 - Security Misconfiguration
**Betroffene Dateien:** [docker-compose.yml](../../infra/docker/docker-compose.yml)

**Beschreibung:**
Die docker-compose.yml enthält Default-Credentials für PostgreSQL und RabbitMQ (`tomsblog`/`tomsblog`, `postgres`/`postgres`). Da diese nur für die lokale Entwicklung gedacht ist und die Produktionsumgebung SOPS-verschlüsselte K8s-Secrets verwendet, ist das Risiko gering. Die Credentials sind über Umgebungsvariablen überschreibbar.

**Beweis:**
```yaml
# docker-compose.yml
postgres-blog-content:
  environment:
    POSTGRES_USER: ${BLOG_CONTENT_DB_USER:-tomsblog}
    POSTGRES_PASSWORD: ${BLOG_CONTENT_DB_PASSWORD:-tomsblog}
```

**Empfohlener Fix:**
Kein unmittelbarer Handlungsbedarf für die Entwicklungsumgebung. Optional: `.env.example` mit Platzhaltern und `.env` in `.gitignore`.

---

### Finding 14: JPA show-sql in local-Profil aktiviert

**Severity:** Informational
**OWASP Category:** A02 - Cryptographic Failures
**Betroffene Dateien:** [application-local.yml](../../services/blog-content/src/main/resources/application-local.yml#L9-L12)

**Beschreibung:**
Im local-Profil ist `spring.jpa.show-sql: true` mit `format_sql: true` aktiviert. SQL-Statements können sensible Daten (Passwort-Hashes, E-Mail-Adressen) in Log-Dateien ausgeben. Da dies nur im local-Profil aktiv ist (in k8s-Profil ist `show-sql: false`), ist das Risiko gering.

**Beweis:**
```yaml
# application-local.yml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        '[format_sql]': true
```

**Empfohlener Fix:**
Kein unmittelbarer Handlungsbedarf. Das k8s-Profil deaktiviert show-sql korrekt.

---

### Finding 15: Fehlgeschlagene Login-Versuche werden nicht auditiert

**Severity:** Informational
**OWASP Category:** A09 - Security Logging and Monitoring Failures
**Betroffene Dateien:** [LoginRateLimitFilter.java](../../services/user-management/src/main/java/de/tomsblog/usermanagement/adapter/inbound/web/LoginRateLimitFilter.java), SecurityConfiguration.java

**Beschreibung:**
Fehlgeschlagene Login-Versuche werden zwar durch den `LoginRateLimitFilter` gezählt, aber nicht als Audit-Events protokolliert. Für die Erkennung von Brute-Force-Angriffen und die Einhaltung von Compliance-Anforderungen sollten fehlgeschlagene Authentifizierungsversuche auditiert werden.

**Beweis:**
Es gibt keinen `AuthenticationFailureHandler` oder `ApplicationListener<AuthenticationFailureBadCredentialsEvent>`, der fehlgeschlagene Logins protokolliert.

**Empfohlener Fix:**
Einen `AuthenticationFailureHandler` oder Spring-Event-Listener hinzufügen:
```java
@Component
public class AuthFailureListener implements ApplicationListener<AuthenticationFailureBadCredentialsEvent> {
    private final AuditLogger auditLogger;

    @Override
    public void onApplicationEvent(AuthenticationFailureBadCredentialsEvent event) {
        String username = event.getAuthentication().getName();
        auditLogger.log(AuditLogEntry.create(null, username, "LOGIN_FAILED", "Auth", username));
    }
}
```

---

## Potentielle Improvements

1. **OWASP Dependency Check in CI**: Der OWASP Dependency Check Plugin (Version 12.1.1) ist im Parent-POM konfiguriert. Es sollte in der CI/CD-Pipeline regelmäßig ausgeführt werden, um bekannte CVEs in Abhängigkeiten zu erkennen.

2. **Content-Security-Policy in user-management und tenant-management**: Nur blog-content hat eine CSP-Header-Konfiguration. Die Auth-UI und Tenant-Admin-UI sollten ebenfalls CSP-Header setzen.

3. **Secure-/SameSite-Cookie-Attribute**: Die Session-Cookies sollten explizit mit `Secure`, `HttpOnly` und `SameSite=Lax` konfiguriert werden. Spring Boot setzt diese teilweise als Default, aber eine explizite Konfiguration wäre robuster.

4. **Image-Digest statt Tag in K8s-Deployments**: Die Deployments verwenden Image-Tags (`0.9.5`), die potenziell überschrieben werden können. Image-Digests (`@sha256:...`) bieten stärkeren Supply-Chain-Schutz. Flux-Image-Automation kompensiert dies teilweise.

5. **Pod-Security-Standards**: Die K8s-Namespace-Konfiguration sollte Pod-Security-Standards (`restricted` Level) über Labels durchsetzen: `pod-security.kubernetes.io/enforce: restricted`.

6. **Read-Only PostgreSQL-User**: Die CloudNativePG-Konfiguration verwendet einen `app`-User für alle Operationen. Ein separater read-only User für Abfragen würde die Angriffsfläche bei SQL-Injection reduzieren.

7. **Kafka-Authentifizierung**: Die Kafka-Konfiguration verwendet `PLAINTEXT`-Listener ohne SASL-Authentifizierung. In Produktion sollte SASL_SSL verwendet werden.

8. **RabbitMQ-Credentials in docker-compose**: Die lokalen RabbitMQ-Credentials sollten über `.env`-Dateien verwaltet werden.

9. **Regelmäßige Security-Audits**: Automatisierte Security-Reviews als Teil der CI/CD-Pipeline einrichten.

## Zusammenfassung

| Severity      | Anzahl |
| ------------- | ------ |
| Critical      | 0      |
| High          | 1      |
| Medium        | 5      |
| Low           | 4      |
| Informational | 4      |

**Gesamtbewertung:**

Die Sicherheitslage hat sich seit dem letzten Audit (2026-05-03) **deutlich verbessert**. Sechs der 16 vorherigen Findings wurden behoben, darunter die kritischsten (REST-API-Auth, User-Management-Auth, XSS-Sanitisierung, Default-Admin-Passwort, K8s-SecurityContext, Rate-Limiting).

Die verbleibenden Findings sind überwiegend mittlerer und niedriger Schwere. Das einzige High-Severity-Finding (Open Redirect) ist einfach zu beheben. Die Kubernetes-Infrastruktur ist vorbildlich gesichert mit Network Policies, Linkerd-mTLS, SOPS-Secrets und gehärteten Container-SecurityContexts.

**Prioritäten für die Behebung:**
1. **Sofort**: Finding 1 (Open Redirect) - einfacher Fix, hohes Risiko
2. **Kurzfristig**: Finding 5 (Timing-sicherer API-Key-Vergleich), Finding 3 (Tag-Admin-Rollenbeschränkung), Finding 6 (Legal-Pages Sanitisierung)
3. **Mittelfristig**: Finding 2 (gRPC-Auth), Finding 4 (Ownership-Checks), Finding 7 (HTTP Basic deaktivieren)
4. **Langfristig**: Finding 8 (CSRF), Finding 9 (Passwort-Komplexität), Finding 10 (Rate-Limit Memory-Leak), Finding 11 (SSRF-Validierung)