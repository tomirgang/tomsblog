# ADR-0013: LLM-basierte Kommentar-Moderation

## Status

Accepted

## Context

Das Kommentar-Modul (Meilenstein 2) benötigt eine Moderationsstrategie. Manuelle Moderation skaliert nicht und belastet Autoren. Regelbasierte Filter (Wortlisten, Regex) sind leicht zu umgehen.

LLM-basierte Moderation kann Kontext verstehen: Beleidigungen, Spam, Off-Topic-Inhalte und subtile Verstöße erkennen, die regelbasierte Systeme übersehen.

## Decision

Neue Kommentare werden **automatisiert durch ein LLM über das OpenRouter-Backend überprüft**.

**Ablauf:**
1. Benutzer erstellt Kommentar
2. Kommentar wird sofort gespeichert (Status: `PENDING`)
3. Asynchron wird der Kommentar an das LLM zur Bewertung übergeben
4. LLM klassifiziert: `APPROVED`, `REJECTED` oder `REVIEW_PENDING`
5. Bei `APPROVED` wird der Kommentar sofort sichtbar
6. Bei `REJECTED` wird der Kommentar ausgeblendet (Autor/Admin kann manuell freigeben)
7. Bei `REVIEW_PENDING` wartet der Kommentar auf manuelle Prüfung

**Graceful Degradation:**
- Bei OpenRouter-Ausfall wird der Kommentar als `REVIEW_PENDING` markiert
- Autoren/Admins werden benachrichtigt, dass manuelle Moderation nötig ist
- Kein Kommentar geht verloren, kein Kommentar wird ungeprüft veröffentlicht

**Architektur:**
- Moderation als Outbound-Port im Kommentar-Service (hexagonal, austauschbar)
- LLM-Adapter implementiert den Port via OpenRouter
- Fallback-Adapter mit regelbasierter Moderation als Alternative
- Event-getriggert: `CommentCreatedEvent` löst Moderation aus

## Consequences

**Positiv:**
- Skaliert ohne manuellen Aufwand
- Erkennt kontextuelle Verstöße (nicht nur Keyword-basiert)
- Wiederverwendung des vorhandenen OpenRouter-Backends
- Austauschbarer Adapter (andere LLMs oder regelbasiert als Fallback)
- Kein Kommentar wird ungeprüft öffentlich sichtbar

**Negativ:**
- Kosten pro Kommentar (LLM-API-Aufruf)
- Latenz zwischen Absenden und Sichtbarkeit (async Moderation)
- False Positives möglich (legitime Kommentare werden zurückgehalten)
- Abhängigkeit von externem Service (Graceful Degradation nötig)

## References

- SWR-018: Automatisierte Kommentar-Moderation via LLM
- SWA-012: LLM-basierte Kommentar-Moderation als Adapter
- SWA-006: AI Service als separater Adapter
- SWR-014: Graceful Degradation bei KI-Ausfall
