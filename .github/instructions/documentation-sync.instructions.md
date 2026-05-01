---
description: "Use when implementing new features, creating adapters, or changing domain logic. Enforces design-first workflow: documentation before code, user approval before implementation."
applyTo: ["**/src/main/**", "**/src/test/**", "**/reqs/**", "docs/**"]
---
# Design-First Implementation Workflow

## Mandatory Sequence

Every feature implementation MUST follow this strict sequence:

### Phase 1: Design (before any code)

1. **Requirements** (Doorstop)
   - New APIs or user-facing functionality → add `SWR` entry in `reqs/software/`
   - New architectural patterns or infrastructure → add `SWA` entry in `reqs/architecture/`
   - Link `SWR` → `STK`, `SWA` → `SWR`

2. **Architecture Decision Records**
   - Significant technology choices (new frameworks, libraries, patterns) → create ADR in `docs/adr/`
   - ADR format: Status, Context, Decision, Consequences, References

3. **Arc42 Architecture** (docs/arc42/)
   - New services, adapters, or ports → update `docs/arc42/modules/ROOT/pages/05_building_blocks/index.adoc`
   - New domain events or runtime flows → update `docs/arc42/modules/ROOT/pages/06_runtime/`

4. **Software Detail Design** (docs/design/)
   - New Use Cases, Services, or Adapters → update the service's detail design doc in `docs/design/modules/ROOT/pages/<service>/index.adoc`
   - Include Mermaid diagrams for new class structures or flows
   - Update the Anforderungsabdeckung (traceability) table

### Phase 2: User Approval Gate

**STOP and ask the user for feedback on the design.**

Present a summary of:
- New/changed requirements (SWR, SWA)
- ADRs created
- Architecture changes
- Detail design decisions

Do NOT proceed to code implementation until the user explicitly approves.

### Phase 3: Implementation (only after approval)

1. Implement domain model, application services, and adapters
2. Write tests with requirement tracing (`@DisplayName("SWR-xxx: ...")`)
3. Add `@req SWR-xxx` annotations to service classes and use case interfaces
4. Create `IMP` entries in `reqs/implementation/` with `references:` pointing to source files
5. Verify build passes (`./mvnw verify`)

## Code-Level Tracing

- Service classes and Use Case interfaces → add `@req SWR-xxx` in Javadoc
- Test methods → include requirement ID in `@DisplayName("SWR-xxx: ...")`

## Completion Checklist

Before considering a feature implementation complete, verify:

- [ ] Requirements (SWR/SWA) created in Phase 1
- [ ] ADR created if a new technology or pattern was introduced
- [ ] Arc42 building blocks updated
- [ ] Detail design doc updated with diagrams and traceability table
- [ ] User approved the design before implementation started
- [ ] Requirement IDs referenced in code (`@req`, `@DisplayName`)
- [ ] IMP entries created with `references:` to new files
- [ ] Build passes (`./mvnw verify`)
