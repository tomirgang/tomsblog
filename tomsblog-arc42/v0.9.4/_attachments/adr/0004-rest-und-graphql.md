# ADR-0004: REST für Text-Blog, GraphQL für Podcast/Video

## Status

Accepted

## Context

Die Plattform hat unterschiedliche UI-Clients mit unterschiedlichen Anforderungen:
- **Text-Blog**: Thymeleaf (SSR) und später Angular mit klassischen CRUD-Operationen, seitenbasiert
- **Podcast/Video**: React mit verschachtelten Datenstrukturen (Episode, Segments, Timestamps), flexible Queries

Wir brauchen eine API-Strategie, die zu den jeweiligen Clients und Datenmustern passt.

## Decision

| Service-Gruppe         | API-Stil                     | Client             |
| ---------------------- | ---------------------------- | ------------------ |
| Text-Blog, Auth, Feeds | REST (Spring MVC)            | Thymeleaf, Angular |
| Podcast, Video         | GraphQL (Spring for GraphQL) | React              |

**Begründung:**
- REST ist einfacher für CRUD-lastige APIs und besser für SSR (Thymeleaf)
- GraphQL vermeidet Over-/Under-Fetching bei verschachtelten Media-Daten
- Trennung ermöglicht Lernen beider Technologien im selben Projekt

## Consequences

**Positiv:**
- Jeder API-Stil dort wo er am besten passt
- Thymeleaf funktioniert natürlich mit REST (Controller → Template)
- React + Apollo Client ist ein bewährtes Muster für GraphQL
- Lernerfahrung für beide API-Paradigmen

**Negativ:**
- Zwei unterschiedliche API-Stile im selben Projekt (höhere Komplexität)
- Tooling-Split: OpenAPI für REST, GraphQL Schema für Podcast/Video
- Team muss beide Paradigmen kennen
