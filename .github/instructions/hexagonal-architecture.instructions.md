---
description: "Use when writing Java code in domain or application layers. Enforces hexagonal architecture boundaries: domain must not import framework or adapter code."
applyTo: "**/src/main/**/domain/**"
---
# Hexagonal Architecture – Domain Layer

The domain layer is the innermost ring. It MUST NOT depend on:
- Spring Framework (`org.springframework.*`)
- Persistence frameworks (`jakarta.persistence.*`, `org.hibernate.*`)
- Web/HTTP (`jakarta.servlet.*`, `org.springframework.web.*`)
- Kafka/messaging (`org.apache.kafka.*`, `org.springframework.kafka.*`)
- Any adapter code from this or other services

## Allowed in domain/

- Pure domain entities, value objects, aggregates
- Domain events (plain POJOs / records)
- Domain services (business logic only)
- Port interfaces (inbound + outbound) defined here or in `application/`

## Patterns

```java
// GOOD: Port interface in domain/application
public interface PostRepository {
    Optional<Post> findById(PostId id);
    Post save(Post post);
}

// BAD: Framework annotation in domain
@Entity  // ← NEVER in domain layer
public class Post { ... }
```

Domain classes use constructor injection via port interfaces – never concrete adapter implementations.
