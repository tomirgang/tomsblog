# ADR-0009: Domain-Modell für Blog Content Service

## Status

Accepted

## Context

Der Blog Content Service benötigt ein vollständiges Domain-Modell, das folgende Konzepte abbildet:

- **Post**: Kern-Aggregate für Blog-Beiträge
- **Author**: Wer hat den Post verfasst
- **Tenant**: Multi-Mandanten-Isolation
- **Tag**: Kategorisierung/Verschlagwortung
- **Attachment**: Dateianhänge (Bilder, PDFs)
- **Translation**: Mehrsprachige Versionen eines Posts
- **Source**: Quellenangaben/Referenzen

Die Herausforderung besteht darin, diese Konzepte DDD-konform zu modellieren (Aggregates, Entities, Value Objects) und gleichzeitig die Anforderungen an Mehrsprachigkeit (SWR-004, SWR-005), Quellenverwaltung (SWR-012) und Multi-Tenancy (SWR-003) zu erfüllen.

## Decision

### Post (Aggregate Root)

Post bleibt das zentrale Aggregate. Es wird erweitert um:

- `AuthorId` als Referenz auf den Autor (nicht embedded, um Konsistenz zu wahren)
- `Set<TagId>` statt `Set<Tag>` — lose Kopplung, Tags haben eigenen Lifecycle
- `List<Source>` als Value Objects direkt im Post
- `List<Attachment>` als Value Objects direkt im Post

### Author (nur AuthorId-Referenz)

Author wird **nicht** als eigene Entity im blog-content Bounded Context modelliert. Der Post trägt lediglich eine `AuthorId`-Referenz. Begründung:

- Keine Datenduplikation zwischen Content-Service und Auth-Service
- Author-Stammdaten (displayName, email, bio) werden vom Auth-Service (Phase 3) verwaltet
- Der blog-content Service löst AuthorId bei Bedarf über einen Read-Adapter auf
- `AuthorId` liegt im shared-kernel und ist service-übergreifend nutzbar

### Tag (Entity mit Identity)

Tag wird von einem einfachen Value Object (nur `name`) zu einer vollwertigen Entity mit eigener ID aufgewertet:

- `TagId`, `TenantId`, `name`, `slug`
- Ermöglicht Tag-Management-API (Umbenennen, Zusammenführen)
- Slug für URL-freundliche Tag-basierte Feeds (SWR-008)
- Tenant-Scoped: Tags sind pro Mandant isoliert

### Translation (eigenes Aggregate)

Übersetzungen werden als separates Aggregate modelliert (nicht als Child im Post):

- Eigener Lifecycle: DRAFT → REVIEW_PENDING → APPROVED/REJECTED
- AI-generierte Übersetzungen starten in REVIEW_PENDING (Autor muss prüfen)
- Manuelle Übersetzungen starten in DRAFT
- Referenziert Post via `PostId`
- Eigene Domain Events: `TranslationCreatedEvent`, `TranslationApprovedEvent`

Begründung: Der KI-Übersetzungs-Workflow (SWR-004) erfordert einen eigenen Freigabe-Prozess. Dies wäre als Child-Entity im Post-Aggregate zu komplex und würde die Transaktionsgrenze unnötig aufblähen.

### Tenant

Tenant wird **nicht** als eigene Entity im blog-content modelliert. Es bleibt bei `TenantId` als Referenz. Begründung: Tenant-Verwaltung (Name, Konfiguration, Abrechnung) gehört in einen separaten Service (Phase 3). Der blog-content Service nutzt `TenantId` nur zur Datenfilterung.

### Source (Value Object)

Quellen werden als Value Object `Source(url, title)` im Post-Aggregate gehalten. Sie haben keinen eigenen Lifecycle und keine eigene Identity.

### Attachment (Value Object)

Attachments werden als Value Object mit einer generierten `AttachmentId` modelliert. Die ID ermöglicht gezieltes Entfernen. Der `storageKey` referenziert den Speicherort im S3-Backend (Phase 7).

Ein `show`-Flag steuert die Darstellung im Blog-Rendering:
- `show=true`: Attachment wird in der Blog-Ansicht angezeigt (z.B. als Galerie-Element oder Download-Link)
- `show=false`: Attachment ist für eingebettete Inhalte gedacht (z.B. Inline-Bilder im Content)

## Consequences

**Positiv:**

- Klare Aggregate-Grenzen: Post, Tag, Translation sind unabhängig persistierbar
- Translation als eigenes Aggregate ermöglicht den KI-Workflow ohne Post zu belasten
- TagId-Referenzen im Post ermöglichen Tag-Rename ohne Post-Update
- Source und Attachment als VOs im Post halten die Konsistenz einfach
- Alle Entities tragen TenantId für Multi-Tenant-Isolation
- Keine Datenduplikation durch reine AuthorId-Referenz
- show-Flag ermöglicht flexible Attachment-Darstellung ohne zusätzliche Entitäten

**Negativ:**

- Eventual Consistency zwischen Post und Translation (separate Aggregates)
- Tag-Löschung erfordert Cleanup der TagId-Referenzen in Posts
- Autor-Anzeigename erfordert Lookup beim Auth-Service (Latenz, Ausfallrisiko)
- Mehr Mapping-Aufwand im Persistence-Adapter (Source-Embeddable, Attachment-Embeddable)

## References

- SWR-001: Post CRUD API
- SWR-003: Tenant-Isolation
- SWR-004: KI-Übersetzung
- SWR-005: Manuelle Übersetzung
- SWR-007: Rollenbasierte Zugriffskontrolle
- SWR-012: Quellenverwaltung
- ADR-0001: Hexagonale Architektur
- ADR-0008: Shared Kernel Framework-frei
