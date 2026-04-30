# Requirements (Doorstop)

ASPICE-orientiertes Requirement Tracing mit Doorstop.

## Dokumenthierarchie

```
STK (Stakeholder Requirements)
 └── SWR (Software Requirements)
      └── SWA (Architecture/Design)
           └── TST (Test Specifications)
```

## Verzeichnisse

| Verzeichnis     | Prefix | Beschreibung                                |
| --------------- | ------ | ------------------------------------------- |
| `stakeholder/`  | STK    | Stakeholder-Anforderungen                   |
| `software/`     | SWR    | Software-Anforderungen (abgeleitet aus STK) |
| `architecture/` | SWA    | Architektur-/Design-Entscheidungen          |
| `tests/`        | TST    | Testspezifikationen (verifizieren SWR)      |

## Befehle

```bash
doorstop                                   # Validierung
doorstop publish all ../docs/requirements/ # HTML-Export
```
