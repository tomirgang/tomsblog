---
description: "Use when working with requirements, traceability, Doorstop documents, or linking requirements to code/tests. Covers ASPICE-oriented requirement tracing conventions."
applyTo: ["**/reqs/**", "**/.doorstop.yml"]
---
# Requirement Tracing Conventions (Doorstop)

## Document Hierarchy (ASPICE-oriented)

| Prefix | Level | ASPICE | Description |
|--------|-------|--------|-------------|
| STK | 1 | SWE.1 | Stakeholder Requirements |
| SWR | 2 | SWE.2 | Software Requirements |
| SWA | 3 | SWE.3 | Architecture & Design |
| TST | 4 | SWE.5/6 | Test Specifications |

## Traceability Rules

- Every `SWR` MUST link to at least one `STK` (parent)
- Every `SWA` MUST link to at least one `SWR` (parent)
- Every `TST` MUST link to the `SWR` it verifies
- No orphaned requirements – run `doorstop` to validate

## Code Tracing

Reference requirement IDs in code to enable implementation tracing:

```java
/**
 * Creates a new blog post for the given tenant.
 *
 * @req SWR-042
 */
public Post createPost(CreatePostCommand command) { ... }
```

## Test Tracing

Use requirement IDs in test display names:

```java
@Test
@DisplayName("SWR-042: Create post assigns tenant ID and generates slug")
void createPostAssignsTenantAndSlug() { ... }
```

## Doorstop Commands

```bash
doorstop create STK reqs/stakeholder    # Initialize a new document
doorstop add STK                        # Add a requirement item
doorstop link SWR042 STK015             # Link child to parent
doorstop                                # Validate all
doorstop publish all docs/requirements/ # Export report
```
