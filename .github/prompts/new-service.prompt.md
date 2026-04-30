---
description: "Scaffold a new microservice with hexagonal architecture (Ports & Adapters), Spring Boot, Maven"
agent: "agent"
argument-hint: "Service name, e.g. 'comment-service'"
---
# New Service Scaffold

Create a new Spring Boot microservice with hexagonal architecture for the Toms Blog platform.

## Input

The service name is: {{input}}

## Requirements

1. Create a Maven module at `services/{{input}}/`
2. Use a `pom.xml` that inherits from the root parent POM
3. Apply the hexagonal package structure:

```
services/{{input}}/
├── pom.xml
└── src/
    ├── main/java/de/tomsblog/{{input_snake}}/
    │   ├── domain/           # Entities, Value Objects, Domain Events
    │   ├── application/      # Use Cases, Port interfaces (inbound + outbound)
    │   └── adapter/
    │       ├── inbound/
    │       │   └── rest/     # REST Controllers (Spring MVC)
    │       └── outbound/
    │           └── persistence/  # JPA/JDBC implementations
    └── test/java/de/tomsblog/{{input_snake}}/
        ├── domain/
        ├── application/
        └── adapter/
```

4. Include basic dependencies: Spring Boot Web, Spring Data JPA, PostgreSQL driver, Testcontainers
5. Create a placeholder `Application.java` with `@SpringBootApplication`
6. Create a basic `application.yml` with PostgreSQL config (use environment variables)
7. Add the module to the root `pom.xml` `<modules>` section
8. Follow English for code, German for documentation files

## Conventions

- Multi-tenant: include `tenantId` in domain entities
- Audit: include `createdAt`, `updatedAt`, `createdBy` fields
- Events: define domain events as Java records in `domain/event/`
- Tests: create one example unit test and one integration test with Testcontainers
