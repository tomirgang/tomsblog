# ADR-0005: UI-Strategie (Thymeleaf → Angular + React)

## Status

Accepted

## Context

Die Plattform braucht eine Benutzeroberfläche. Wir wollen inkrementell vorgehen: zuerst etwas Funktionierendes, dann modernisieren.

**Alternativen für Phase 1:**
- **SPA sofort (Angular/React)**: modernes UX, aber höherer initialer Aufwand
- **Thymeleaf (SSR)**: schnell funktionsfähig, Spring-integriert, SEO-freundlich

**Zielzustand:**
- Angular + Nx + Native Federation für den Text-Blog (REST)
- React + Module Federation für Podcast/Video (GraphQL)

## Decision

**Phase 1:** Thymeleaf als SSR-UI für den vollständigen Text-Blog.

**Später:**
- Angular-UI (Nx + Native Federation) als SPA für den Text-Blog
- React-UI (Module Federation) für Podcast und Video
- Thymeleaf bleibt als Fallback/SEO-Variante

Die REST-API wird von Anfang an so designed, dass sie sowohl von Thymeleaf-Controllern als auch von SPAs konsumiert werden kann.

## Consequences

**Positiv:**
- Schneller erster lauffähiger Blog (kein Frontend-Build-Tooling nötig)
- SSR = SEO-freundlich out-of-the-box
- API-First-Design erzwingt saubere Trennung
- Micro-Frontend-Architektur als Lernziel (Native Federation + Module Federation)

**Negativ:**
- Thymeleaf-UI wird teilweise obsolet nach Angular-Migration
- Doppelter UI-Code während der Übergangsphase
- Native Federation und Module Federation sind unterschiedliche Toolchains
