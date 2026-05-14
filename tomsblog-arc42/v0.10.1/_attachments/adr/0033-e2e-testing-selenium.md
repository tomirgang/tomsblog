# ADR-0033: E2E-Testing-Strategie mit Selenium WebDriver

## Status

Proposed

## Context

Die Plattform verfügt über 19 Thymeleaf-Templates verteilt auf drei Services (Blog Content, User Management, Tenant Management) mit 16 UI-Use-Cases. Aktuell existieren ausschließlich Unit-Tests (MockMvc) und Integrationstests (Testcontainers) für die Web-Adapter-Schicht. Echte Browser-basierte End-to-End-Tests, die das Zusammenspiel von UI, Backend und Datenbank aus Benutzersicht verifizieren, fehlen vollständig (STK-052).

### Probleme ohne E2E-Tests

1. **Kein Rendering-Nachweis**: MockMvc prüft HTTP-Responses, aber nicht ob Templates korrekt rendern (Thymeleaf-Fehler, fehlende Fragmente, CSS-Probleme)
2. **Keine JavaScript-Verifikation**: Mermaid-Diagramme, Syntax-Highlighting und interaktive Elemente werden nicht getestet
3. **Keine Cross-Service-Verifikation**: Login-Flow (User Management) → Post-Erstellung (Blog Content) → Tenant-Settings (Tenant Management) wird nie als Ganzes getestet
4. **Kein Regressionsschutz**: UI-Änderungen in der Shared UI Library (libs/shared-ui) können unbemerkt alle Services beeinflussen

### Evaluierte Alternativen

**Playwright (Node.js-basiert):**

- Vorteil: Modernes API, Auto-Wait, Multi-Browser
- Nachteil: Node.js-Toolchain parallel zu Maven, kein nativer JUnit-5-Support, separate Dependency-Verwaltung

**Cypress (JavaScript-basiert):**

- Vorteil: Interaktives Test-UI, Time-Travel Debugging
- Nachteil: Eigene Runtime, kein Java-Ökosystem, nur Chromium-Support (Firefox experimentell)

**Selenium WebDriver (Java-basiert):**

- Vorteil: Gleiche Sprache wie Produktionscode, JUnit 5 nativ, Maven-Integration, breiter Browser-Support, Testcontainers-Selenium-Module verfügbar
- Nachteil: Älteres API, explizite Waits erforderlich, verbose Page Objects

**HtmlUnit (headless, kein echter Browser):**

- Vorteil: Schnell, kein Browser-Prozess
- Nachteil: Kein echtes Rendering, kein JavaScript-Support, kein visueller Nachweis

## Decision

**Selenium WebDriver** wird als E2E-Test-Framework eingesetzt. Die Tests werden in einem **separaten Maven-Modul `e2e-tests/`** auf Root-Ebene organisiert.

### Architektur-Entscheidungen

1. **Separates Maven-Modul**: Das Modul `e2e-tests/` ist kein Service, sondern ein reines Testprojekt. Es hat Testabhängigkeiten auf die drei Services, startet diese als Spring-Boot-Anwendungen mit Testcontainers und testet gegen echte Browser-Instanzen.

2. **Testcontainers-Selenium**: Browser-Container (Chrome) werden per Testcontainers gestartet. Damit entfällt die Notwendigkeit, lokal einen Browser oder ChromeDriver zu installieren.

3. **Page Object Model (POM)**: Jede UI-Seite wird als Page Object abstrahiert. Test-Logik und Seitenstruktur sind getrennt. Änderungen an Templates erfordern nur Anpassungen im zugehörigen Page Object.

4. **JUnit 5 Extension für Screenshots**: Bei Test-Fehlschlägen wird automatisch ein Screenshot erstellt und im `target/screenshots/`-Verzeichnis abgelegt.

5. **Eigenes Maven-Profil**: E2E-Tests laufen ausschließlich im Profil `e2e` (`mvn verify -Pe2e`), damit der reguläre Build-Zyklus nicht verlangsamt wird.

6. **Lokale Ausführung**: Entwickler können E2E-Tests lokal mit `./mvnw verify -Pe2e` ausführen. Testcontainers starten PostgreSQL und Chrome automatisch.

7. **Release-Pflicht**: Das Release-Script (`scripts/release.sh`) führt die E2E-Tests als Pflichtschritt vor dem Release aus.

8. **CI/CD-Integration**: Ein separater GitHub Actions Job führt die E2E-Tests nach dem regulären Build aus. Screenshots und HTML-Reports werden als Artefakte archiviert.

### Testabdeckung

| Bereich | Testklasse | UI-Use-Cases |
|---------|-----------|-------------|
| Blog public | BlogPublicViewE2ETest | Landing Page, Post-Liste, Suche, Detailansicht, Legal |
| Blog author | BlogAuthorWorkflowE2ETest | Post erstellen, Post bearbeiten |
| Auth | AuthenticationE2ETest | Login, Registrierung, Break-Glass Login |
| Admin | AdminWorkflowE2ETest | Benutzerverwaltung, Tenant-Settings |
| SuperAdmin | SuperAdminE2ETest | SuperAdmin-Login, Tenant-CRUD, Global Settings |

## Consequences

### Vorteile

- Vollständiger Nachweis, dass alle UI-Use-Cases aus Benutzersicht funktionieren
- Regressionsschutz bei Änderungen an Templates, CSS oder JavaScript
- Gleiche Sprache und Build-Toolchain wie der Produktionscode
- Automatische Browser-Verwaltung durch Testcontainers (kein lokales Setup nötig)
- Screenshots bei Fehlschlag für schnelle Fehleranalyse
- Lokale Ausführbarkeit und Release-Integration gewährleisten Qualitätssicherung

### Nachteile

- Längere Testlaufzeit (Browser-Start, Seitennavigation, Waits)
- Zusätzliches Maven-Modul erhöht die Build-Komplexität
- Selenium-Tests können flaky sein (Timing-Probleme, Browser-Updates)
- Testcontainers-Selenium benötigt Docker lokal und in CI

## References

- STK-052: E2E-Selenium-Tests für alle relevanten UI-Use-Cases
- ADR-0005: UI-Strategie (Thymeleaf -> Angular + React)
- ADR-0032: Eigenständige UI für Authentifizierung und Benutzerverwaltung
- [Selenium WebDriver Documentation](https://www.selenium.dev/documentation/webdriver/)
- [Testcontainers Selenium Module](https://java.testcontainers.org/modules/webdriver_containers/)
