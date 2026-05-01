---
description: "Use when working with requirements, traceability, Doorstop documents, or linking requirements to code/tests. Covers ASPICE-oriented requirement tracing conventions."
applyTo: ["**/reqs/**", "**/.doorstop.yml", "**/src/main/**/*.java", "**/src/test/**/*.java"]
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

## Code Tracing – @req Annotations

Every Java class, interface, or record that implements or contributes to a requirement MUST have a `@req` tag in its Javadoc. This is mandatory, not optional.

### Rules

1. **Every public class/interface/record** in `src/main/java` MUST have a class-level Javadoc with at least one `@req SWR-xxx` tag
2. **Method-level `@req`** is required when a single method fulfills a different requirement than the class
3. **Multiple requirements** are expressed as separate `@req` lines
4. **Never create a class without determining which requirement it traces to**

### Requirement Mapping Reference

| Requirement | Scope |
|-------------|-------|
| SWR-001 | Post CRUD operations (controllers, use cases, services, commands, DTOs) |
| SWR-002 | Post publishing |
| SWR-003 | Tenant isolation (repositories, tenant-scoped queries) |
| SWR-004 | AI translation |
| SWR-005 | Manual translation |
| SWR-009 | Domain events (event classes, EventPublisher) |
| SWR-012 | Source management (Source, SourceController, commands) |
| SWR-015 | Input validation (GlobalExceptionHandler, validation annotations) |
| SWR-020 | Tag CRUD (TagController, TagUseCase, TagService, commands, DTOs) |
| SWR-021 | Translation CRUD (TranslationController, TranslationUseCase, commands, DTOs) |

### Examples

```java
/**
 * REST adapter for post management.
 *
 * @req SWR-001
 * @req SWR-002
 * @req SWR-015
 */
@RestController
public class PostController { ... }

/** @req SWR-001 */
public record CreatePostCommand(TenantId tenantId, ...) {}

/** @req SWR-009 */
public record PostCreatedEvent(...) implements DomainEvent {}
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
