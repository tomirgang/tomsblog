---
description: "Use when creating or editing software detail design documentation. Enforces Mermaid diagrams, requirement references with context, and AsciiDoc + Antora conventions."
applyTo: "docs/design/**"
---
# Software Detail Design Documentation

## Location and Structure

- Design docs live in `docs/design/` as a separate Antora component (`tomsblog-design`)
- Each service gets its own subdirectory under `modules/ROOT/pages/<service-name>/`
- Navigation is maintained in `modules/ROOT/nav.adoc`

## Diagrams

- Use **Mermaid** diagrams inline in AsciiDoc via `[mermaid]` blocks (NOT PlantUML)
- Every diagram MUST be preceded by explaining text that describes what the diagram shows

```asciidoc
// GOOD: Diagram with context
Das folgende Klassendiagramm zeigt die Struktur des Domain Layers:

[mermaid]
----
classDiagram
    class Post { ... }
----

// BAD: Bare diagram without context
[mermaid]
----
classDiagram
    class Post { ... }
----
```

## Requirement References

- Reference Doorstop requirement IDs (SWR-xxx, SWA-xxx) inline where relevant
- Every requirement reference MUST be accompanied by explaining text
- Never place a bare requirement ID without context

```asciidoc
// GOOD: Requirement with context
Die Tenant-Isolation (SWR-003) wird durch TenantId-Filter in allen Repository-Abfragen sichergestellt.

// BAD: Bare reference
SWR-003
```

- Include a traceability table at the end of each service design document

## Language and Encoding

- Documentation language: **German** (with proper umlauts: ä, ö, ü, ß)
- Code language: English (class names, method names)
- Use proper German umlauts in all AsciiDoc content
